package com.docbridge.backend.processing.ocr;

import com.docbridge.backend.common.exception.BusinessException;
import com.docbridge.backend.processing.model.OcrLine;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BaiduOcrService implements OcrProviderClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String secretKey;
    private final String tokenUrl;
    private final String ocrUrl;

    private volatile String accessToken;
    private volatile Instant tokenExpireAt;

    public BaiduOcrService(ObjectMapper objectMapper,
                           @Value("${app.ocr.baidu.api-key:}") String apiKey,
                           @Value("${app.ocr.baidu.secret-key:}") String secretKey,
                           @Value("${app.ocr.baidu.token-url:https://aip.baidubce.com/oauth/2.0/token}") String tokenUrl,
                           @Value("${app.ocr.baidu.ocr-url:https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic}") String ocrUrl,
                           @Value("${app.ocr.timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.tokenUrl = tokenUrl;
        this.ocrUrl = ocrUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 1))).build();
    }

    @Override
    public String provider() {
        return "baidu";
    }

    @Override
    public List<OcrLine> extractLines(String sourceType, byte[] content) {
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new BusinessException("OCR_CONFIG_ERROR", "百度 OCR 未配置密钥，请设置 app.ocr.baidu.api-key/secret-key", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (content == null || content.length == 0) {
            return List.of();
        }
        try {
            String token = ensureAccessToken();
            String imageBase64 = Base64.getEncoder().encodeToString(content);
            String body = "image=" + URLEncoder.encode(imageBase64, StandardCharsets.UTF_8)
                + "&language_type=ENG"
                + "&detect_direction=false";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ocrUrl + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("OCR_FAILED", "百度 OCR 调用失败，HTTP " + response.statusCode(), HttpStatus.BAD_GATEWAY);
            }

            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object errorCode = result.get("error_code");
            if (errorCode != null) {
                Object errorMsg = result.get("error_msg");
                throw new BusinessException("OCR_FAILED", "百度 OCR 失败: " + errorCode + " - " + errorMsg, HttpStatus.BAD_GATEWAY);
            }

            Object wordsResultObj = result.get("words_result");
            if (!(wordsResultObj instanceof List<?> wordsResult)) {
                return List.of();
            }

            List<OcrLine> lines = new ArrayList<>();
            int order = 1;
            for (Object rowObj : wordsResult) {
                if (!(rowObj instanceof Map<?, ?> row)) {
                    continue;
                }
                Object words = row.get("words");
                String text = words == null ? "" : String.valueOf(words).trim();
                if (text.isBlank()) {
                    continue;
                }
                lines.add(new OcrLine(order++, text, 0, 0, 0, 0));
            }
            return lines;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("OCR_FAILED", "百度 OCR 调用异常", HttpStatus.BAD_GATEWAY);
        }
    }

    private String ensureAccessToken() throws Exception {
        Instant now = Instant.now();
        if (accessToken != null && tokenExpireAt != null && now.isBefore(tokenExpireAt.minusSeconds(60))) {
            return accessToken;
        }
        synchronized (this) {
            if (accessToken != null && tokenExpireAt != null && Instant.now().isBefore(tokenExpireAt.minusSeconds(60))) {
                return accessToken;
            }
            String body = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("OCR_CONFIG_ERROR", "百度 OCR 获取 access_token 失败，HTTP " + response.statusCode(), HttpStatus.BAD_GATEWAY);
            }
            Map<String, Object> tokenResp = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object token = tokenResp.get("access_token");
            if (token == null || String.valueOf(token).isBlank()) {
                throw new BusinessException("OCR_CONFIG_ERROR", "百度 OCR 获取 access_token 失败", HttpStatus.BAD_GATEWAY);
            }
            long expiresIn = 2592000L;
            Object expiresObj = tokenResp.get("expires_in");
            if (expiresObj instanceof Number number) {
                expiresIn = number.longValue();
            }
            this.accessToken = String.valueOf(token);
            this.tokenExpireAt = Instant.now().plusSeconds(Math.max(expiresIn, 120));
            return this.accessToken;
        }
    }
}

