package com.docbridge.backend.processing.translate;

import com.docbridge.backend.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BaiduTranslatorService implements TranslatorProviderClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String appId;
    private final String secretKey;
    private final String endpoint;
    private final String sourceLang;
    private final String targetLang;

    public BaiduTranslatorService(ObjectMapper objectMapper,
                                  @Value("${app.translate.baidu.app-id:}") String appId,
                                  @Value("${app.translate.baidu.secret-key:}") String secretKey,
                                  @Value("${app.translate.baidu.endpoint:https://fanyi-api.baidu.com/api/trans/vip/translate}") String endpoint,
                                  @Value("${app.translate.source-lang:en}") String sourceLang,
                                  @Value("${app.translate.target-lang:zh}") String targetLang,
                                  @Value("${app.translate.timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 1))).build();
    }

    @Override
    public String provider() {
        return "baidu";
    }

    @Override
    public List<String> translateBatch(List<String> segments) {
        if (appId == null || appId.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new BusinessException("TRANSLATION_CONFIG_ERROR", "百度翻译未配置密钥，请设置 app.translate.baidu.app-id/secret-key", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        List<String> translated = new ArrayList<>(segments.size());
        for (String text : segments) {
            translated.add(translateOne(text));
        }
        return translated;
    }

    private String translateOne(String source) {
        try {
            String salt = UUID.randomUUID().toString().replace("-", "");
            String sign = md5Hex(appId + source + salt + secretKey);
            String body = "q=" + URLEncoder.encode(source, StandardCharsets.UTF_8)
                + "&from=" + URLEncoder.encode(sourceLang, StandardCharsets.UTF_8)
                + "&to=" + URLEncoder.encode(targetLang, StandardCharsets.UTF_8)
                + "&appid=" + URLEncoder.encode(appId, StandardCharsets.UTF_8)
                + "&salt=" + URLEncoder.encode(salt, StandardCharsets.UTF_8)
                + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("TRANSLATION_FAILED", "百度翻译调用失败，HTTP " + response.statusCode(), HttpStatus.BAD_GATEWAY);
            }

            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object errorCode = result.get("error_code");
            if (errorCode != null) {
                Object errorMsg = result.get("error_msg");
                throw new BusinessException("TRANSLATION_FAILED", "百度翻译失败: " + errorCode + " - " + errorMsg, HttpStatus.BAD_GATEWAY);
            }

            Object transObj = result.get("trans_result");
            if (!(transObj instanceof List<?> list) || list.isEmpty()) {
                throw new BusinessException("TRANSLATION_FAILED", "百度翻译返回结果为空", HttpStatus.BAD_GATEWAY);
            }
            Object first = list.get(0);
            if (!(first instanceof Map<?, ?> row)) {
                throw new BusinessException("TRANSLATION_FAILED", "百度翻译响应格式异常", HttpStatus.BAD_GATEWAY);
            }
            Object dst = row.get("dst");
            if (dst == null) {
                throw new BusinessException("TRANSLATION_FAILED", "百度翻译返回缺少 dst", HttpStatus.BAD_GATEWAY);
            }
            return String.valueOf(dst);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("TRANSLATION_FAILED", "百度翻译调用异常", HttpStatus.BAD_GATEWAY);
        }
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

