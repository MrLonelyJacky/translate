package com.docbridge.backend.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docbridge.backend.auth.dto.WechatLoginResponse;
import com.docbridge.backend.auth.dto.WechatQrUrlResponse;
import com.docbridge.backend.auth.entity.UserEntity;
import com.docbridge.backend.auth.mapper.UserMapper;
import com.docbridge.backend.common.exception.BusinessException;
import com.docbridge.backend.common.security.JwtTokenProvider;
import com.docbridge.backend.quota.entity.QuotaAccountEntity;
import com.docbridge.backend.quota.mapper.QuotaAccountMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final long STATE_EXPIRE_SECONDS = 300;

    private final JwtTokenProvider jwtTokenProvider;
    private final long expireSeconds;
    private final int ocrTotal;
    private final int translateTotal;
    private final UserMapper userMapper;
    private final QuotaAccountMapper quotaAccountMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final boolean wechatEnabled;
    private final String appId;
    private final String appSecret;
    private final String redirectUri;
    private final String frontendOrigin;
    private final Map<String, LocalDateTime> stateStore = new ConcurrentHashMap<>();

    public AuthService(JwtTokenProvider jwtTokenProvider,
                       @Value("${app.jwt.expire-seconds:7200}") long expireSeconds,
                       @Value("${app.quota.ocr-total:3}") int ocrTotal,
                       @Value("${app.quota.translate-total:3}") int translateTotal,
                       @Value("${app.wechat.enabled:false}") boolean wechatEnabled,
                       @Value("${app.wechat.app-id:}") String appId,
                       @Value("${app.wechat.app-secret:}") String appSecret,
                       @Value("${app.wechat.redirect-uri:}") String redirectUri,
                       @Value("${app.wechat.frontend-origin:http://localhost}") String frontendOrigin,
                       UserMapper userMapper,
                       QuotaAccountMapper quotaAccountMapper,
                       ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.expireSeconds = expireSeconds;
        this.ocrTotal = ocrTotal;
        this.translateTotal = translateTotal;
        this.wechatEnabled = wechatEnabled;
        this.appId = appId;
        this.appSecret = appSecret;
        this.redirectUri = redirectUri;
        this.frontendOrigin = frontendOrigin;
        this.userMapper = userMapper;
        this.quotaAccountMapper = quotaAccountMapper;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public WechatQrUrlResponse createQrLoginUrl() {
        if (!wechatEnabled) {
            throw new BusinessException("WECHAT_DISABLED", "微信扫码登录未启用，请配置 app.wechat 参数", HttpStatus.BAD_REQUEST);
        }
        assertWechatConfig();
        String state = UUID.randomUUID().toString().replace("-", "");
        stateStore.put(state, LocalDateTime.now().plusSeconds(STATE_EXPIRE_SECONDS));
        cleanupExpiredState();

        String encodedRedirectUri = UriUtils.encode(redirectUri, StandardCharsets.UTF_8);
        String authUrl = "https://open.weixin.qq.com/connect/qrconnect"
            + "?appid=" + appId
            + "&redirect_uri=" + encodedRedirectUri
            + "&response_type=code"
            + "&scope=snsapi_login"
            + "&state=" + state
            + "#wechat_redirect";
        return new WechatQrUrlResponse(authUrl, state, STATE_EXPIRE_SECONDS);
    }

    public WechatLoginResponse loginByCode(String code, String state) {
        if (!wechatEnabled) {
            return mockWechatLogin(code);
        }
        assertWechatConfig();
        validateState(state);

        Map<String, Object> tokenResult = fetchToken(code);
        String accessToken = valueAsString(tokenResult, "access_token");
        String openId = valueAsString(tokenResult, "openid");
        if (accessToken == null || openId == null) {
            throw new BusinessException("WECHAT_AUTH_FAILED", "微信授权失败，请重试", HttpStatus.BAD_GATEWAY);
        }

        Map<String, Object> userInfo = fetchUserInfo(accessToken, openId);
        String unionId = valueAsString(userInfo, "unionid");
        String normalizedUnionId = (unionId == null || unionId.isBlank()) ? "openid_" + openId : unionId;
        return loginOrCreateUser(normalizedUnionId, openId);
    }

    public String buildCallbackHtml(WechatLoginResponse loginResponse) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                "type", "DOCBRIDGE_WECHAT_LOGIN",
                "success", true,
                "data", loginResponse
            ));
        } catch (Exception ex) {
            throw new BusinessException("INTERNAL_ERROR", "登录结果序列化失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>登录中</title></head><body>"
            + "<script>"
            + "(function(){"
            + "const payload=" + payload + ";"
            + "if(window.opener){window.opener.postMessage(payload, '" + frontendOrigin + "');window.close();}"
            + "else{document.body.innerText='登录成功，请返回原页面';}"
            + "})();"
            + "</script>"
            + "</body></html>";
    }

    public WechatLoginResponse mockWechatLogin(String code) {
        return loginOrCreateUser("mock_unionid_" + code, "mock_openid_" + code);
    }

    private WechatLoginResponse loginOrCreateUser(String unionId, String openId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getWechatUnionid, unionId));

        if (user == null) {
            user = new UserEntity();
            user.setWechatUnionid(unionId);
            user.setWechatOpenid(openId);
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        initQuotaIfMissing(user.getId());

        String role = user.getRole();
        Long userId = user.getId();
        String token = jwtTokenProvider.createToken(String.valueOf(userId), role);

        return new WechatLoginResponse(
            token,
            expireSeconds,
            new WechatLoginResponse.UserProfile(userId, unionId, openId, role)
        );
    }

    private Map<String, Object> fetchToken(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token"
            + "?appid=" + appId
            + "&secret=" + appSecret
            + "&code=" + code
            + "&grant_type=authorization_code";
        return getWechatJson(url);
    }

    private Map<String, Object> fetchUserInfo(String accessToken, String openId) {
        String url = "https://api.weixin.qq.com/sns/userinfo"
            + "?access_token=" + accessToken
            + "&openid=" + openId
            + "&lang=zh_CN";
        return getWechatJson(url);
    }

    private Map<String, Object> getWechatJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            if (result.get("errcode") != null) {
                throw new BusinessException("WECHAT_API_ERROR", "微信接口调用失败: " + result.get("errmsg"), HttpStatus.BAD_GATEWAY);
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("WECHAT_API_ERROR", "微信接口调用失败", HttpStatus.BAD_GATEWAY);
        }
    }

    private void validateState(String state) {
        if (state == null || state.isBlank()) {
            throw new BusinessException("WECHAT_STATE_INVALID", "微信登录状态缺失，请重新扫码", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime expiredAt = stateStore.remove(state);
        if (expiredAt == null || expiredAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException("WECHAT_STATE_INVALID", "微信登录状态已过期，请重新扫码", HttpStatus.BAD_REQUEST);
        }
    }

    private void cleanupExpiredState() {
        LocalDateTime now = LocalDateTime.now();
        stateStore.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    private void assertWechatConfig() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException("WECHAT_CONFIG_MISSING", "微信登录配置缺失，请检查 app.wechat.*", HttpStatus.BAD_REQUEST);
        }
    }

    private String valueAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private void initQuotaIfMissing(Long userId) {
        if (quotaAccountMapper.selectById(userId) != null) {
            return;
        }
        QuotaAccountEntity quota = new QuotaAccountEntity();
        quota.setUserId(userId);
        quota.setOcrTotal(ocrTotal);
        quota.setOcrUsed(0);
        quota.setOcrRefunded(0);
        quota.setTranslateTotal(translateTotal);
        quota.setTranslateUsed(0);
        quota.setTranslateRefunded(0);
        quota.setUpdatedAt(LocalDateTime.now());
        quotaAccountMapper.insert(quota);
    }
}
