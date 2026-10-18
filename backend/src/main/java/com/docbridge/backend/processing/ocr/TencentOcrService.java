package com.docbridge.backend.processing.ocr;

import com.docbridge.backend.common.exception.BusinessException;
import com.docbridge.backend.processing.model.OcrLine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TencentOcrService implements OcrProviderClient {
    private static final String OCR_HOST = "ocr.tencentcloudapi.com";
    private static final String OCR_ENDPOINT = "https://ocr.tencentcloudapi.com";
    private static final String OCR_SERVICE = "ocr";
    private static final String OCR_VERSION = "2018-11-19";
    private static final String OCR_ACTION = "GeneralBasicOCR";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String secretId;
    private final String secretKey;
    private final String region;
    private final int timeoutSeconds;

    public TencentOcrService(ObjectMapper objectMapper,
                             @Value("${app.ocr.tencent.secret-id:}") String secretId,
                             @Value("${app.ocr.tencent.secret-key:}") String secretKey,
                             @Value("${app.ocr.tencent.region:ap-beijing}") String region,
                             @Value("${app.ocr.timeout-seconds:20}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 1))).build();
    }

    @Override
    public String provider() {
        return "tencent";
    }

    @Override
    public List<OcrLine> extractLines(String sourceType, byte[] content) {
        if (secretId == null || secretId.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new BusinessException("OCR_CONFIG_ERROR", "腾讯 OCR 未配置密钥，请设置 app.ocr.tencent.secret-id/secret-key", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (content == null || content.length == 0) {
            return List.of();
        }

        try {
            long timestamp = Instant.now().getEpochSecond();
            String date = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_DATE);

            String imageBase64 = Base64.getEncoder().encodeToString(content);
            Map<String, Object> payload = new HashMap<>();
            payload.put("ImageBase64", imageBase64);
            payload.put("IsPdf", false);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String signedHeaders = "content-type;host;x-tc-action";
            String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                + "host:" + OCR_HOST + "\n"
                + "x-tc-action:" + OCR_ACTION.toLowerCase(Locale.ROOT) + "\n";
            String canonicalRequest = "POST\n/\n\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + sha256Hex(payloadJson);

            String credentialScope = date + "/" + OCR_SERVICE + "/tc3_request";
            String stringToSign = "TC3-HMAC-SHA256\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

            byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
            byte[] secretService = hmacSha256(secretDate, OCR_SERVICE);
            byte[] secretSigning = hmacSha256(secretService, "tc3_request");
            String signature = toHex(hmacSha256(secretSigning, stringToSign));

            String authorization = "TC3-HMAC-SHA256 "
                + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OCR_ENDPOINT))
                .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 1)))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-TC-Action", OCR_ACTION)
                .header("X-TC-Version", OCR_VERSION)
                .header("X-TC-Region", region)
                .header("X-TC-Timestamp", String.valueOf(timestamp))
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("OCR_FAILED", "腾讯 OCR 调用失败，HTTP " + response.statusCode(), HttpStatus.BAD_GATEWAY);
            }

            Map<String, Object> root = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object responseObj = root.get("Response");
            if (!(responseObj instanceof Map<?, ?> responseMap)) {
                throw new BusinessException("OCR_FAILED", "腾讯 OCR 响应格式异常", HttpStatus.BAD_GATEWAY);
            }

            Object errorObj = responseMap.get("Error");
            if (errorObj instanceof Map<?, ?> errorMap) {
                Object code = errorMap.get("Code");
                Object message = errorMap.get("Message");
                throw new BusinessException("OCR_FAILED", "腾讯 OCR 失败: " + code + " - " + message, HttpStatus.BAD_GATEWAY);
            }

            Object detectionsObj = responseMap.get("TextDetections");
            if (!(detectionsObj instanceof List<?> detections)) {
                return List.of();
            }

            List<OcrLine> lines = new ArrayList<>();
            int order = 1;
            for (Object item : detections) {
                if (!(item instanceof Map<?, ?> row)) {
                    continue;
                }
                Object detectedText = row.get("DetectedText");
                String text = detectedText == null ? "" : String.valueOf(detectedText).trim();
                if (text.isBlank()) {
                    continue;
                }
                int[] bbox = resolveBoundingBox(row.get("Polygon"));
                lines.add(new OcrLine(order++, text, bbox[0], bbox[1], bbox[2], bbox[3]));
            }
            return lines;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("OCR_FAILED", "腾讯 OCR 调用异常", HttpStatus.BAD_GATEWAY);
        }
    }

    private int[] resolveBoundingBox(Object polygonObj) {
        if (!(polygonObj instanceof List<?> points) || points.isEmpty()) {
            return new int[]{0, 0, 0, 0};
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Object pointObj : points) {
            if (!(pointObj instanceof Map<?, ?> point)) {
                continue;
            }
            int x = toInt(point.get("X"));
            int y = toInt(point.get("Y"));
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (minX == Integer.MAX_VALUE) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
