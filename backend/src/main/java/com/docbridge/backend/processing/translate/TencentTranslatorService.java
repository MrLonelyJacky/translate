package com.docbridge.backend.processing.translate;

import com.docbridge.backend.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class TencentTranslatorService implements TranslatorProviderClient {
    private static final String TENCENT_HOST = "tmt.tencentcloudapi.com";
    private static final String TENCENT_ENDPOINT = "https://tmt.tencentcloudapi.com";
    private static final String TENCENT_SERVICE = "tmt";
    private static final String TENCENT_VERSION = "2018-03-21";
    private static final String TENCENT_ACTION = "TextTranslate";
    private static final String CHUNK_SEPARATOR = "<<<DOCBRIDGE_SEG_7F3B1C>>>";

    private static final Logger log = LoggerFactory.getLogger(TencentTranslatorService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String tencentSecretId;
    private final String tencentSecretKey;
    private final String tencentRegion;
    private final long tencentProjectId;
    private final String sourceLang;
    private final String targetLang;
    private final int timeoutSeconds;
    private final int batchMaxLines;
    private final int batchMaxChars;

    public TencentTranslatorService(ObjectMapper objectMapper,
                                    @Value("${app.translate.tencent.secret-id:}") String tencentSecretId,
                                    @Value("${app.translate.tencent.secret-key:}") String tencentSecretKey,
                                    @Value("${app.translate.tencent.region:ap-beijing}") String tencentRegion,
                                    @Value("${app.translate.tencent.project-id:0}") long tencentProjectId,
                                    @Value("${app.translate.source-lang:en}") String sourceLang,
                                    @Value("${app.translate.target-lang:zh}") String targetLang,
                                    @Value("${app.translate.timeout-seconds:20}") int timeoutSeconds,
                                    @Value("${app.translate.batch-max-lines:15}") int batchMaxLines,
                                    @Value("${app.translate.batch-max-chars:1500}") int batchMaxChars) {
        this.objectMapper = objectMapper;
        this.tencentSecretId = tencentSecretId;
        this.tencentSecretKey = tencentSecretKey;
        this.tencentRegion = tencentRegion;
        this.tencentProjectId = tencentProjectId;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.timeoutSeconds = timeoutSeconds;
        this.batchMaxLines = Math.max(1, batchMaxLines);
        this.batchMaxChars = Math.max(100, batchMaxChars);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 1))).build();
    }

    @Override
    public String provider() {
        return "tencent";
    }

    @Override
    public List<String> translateBatch(List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        return translateBatchByTencent(segments);
    }

    private List<String> translateBatchByTencent(List<String> sources) {
        if (tencentSecretId == null || tencentSecretId.isBlank() || tencentSecretKey == null || tencentSecretKey.isBlank()) {
            throw new BusinessException("TRANSLATION_CONFIG_ERROR", "腾讯翻译未配置密钥，请设置 app.translate.tencent.secret-id/secret-key", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<List<String>> chunks = splitIntoChunks(sources);
        List<String> translated = new ArrayList<>(sources.size());
        for (List<String> chunk : chunks) {
            translated.addAll(translateChunk(chunk));
        }
        return translated;
    }

    private List<List<String>> splitIntoChunks(List<String> sources) {
        List<List<String>> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentChars = 0;

        for (String source : sources) {
            String normalized = source == null ? "" : source;
            int nextChars = normalized.length();

            boolean lineLimitReached = current.size() >= batchMaxLines;
            boolean charLimitReached = !current.isEmpty() && (currentChars + nextChars > batchMaxChars);
            if (lineLimitReached || charLimitReached) {
                chunks.add(current);
                current = new ArrayList<>();
                currentChars = 0;
            }

            current.add(normalized);
            currentChars += nextChars;
        }

        if (!current.isEmpty()) {
            chunks.add(current);
        }
        return chunks;
    }

    private List<String> translateChunk(List<String> chunk) {
        if (chunk.size() == 1) {
            return List.of(translateByTencentApi(chunk.get(0)));
        }

        boolean hasSeparatorConflict = chunk.stream().anyMatch(text -> text.contains(CHUNK_SEPARATOR));
        if (hasSeparatorConflict) {
            log.warn("Chunk contains separator token, fallback to line-by-line translation for {} lines", chunk.size());
            return translateLineByLine(chunk);
        }

        String mergedSource = String.join(CHUNK_SEPARATOR, chunk);
        String mergedTranslated = translateByTencentApi(mergedSource);
        String[] parts = mergedTranslated.split(Pattern.quote(CHUNK_SEPARATOR), -1);
        if (parts.length != chunk.size()) {
            log.warn("Chunk split mismatch after translation. expected={}, actual={}, fallback line-by-line", chunk.size(), parts.length);
            return translateLineByLine(chunk);
        }
        return Arrays.asList(parts);
    }

    private List<String> translateLineByLine(List<String> chunk) {
        List<String> translated = new ArrayList<>(chunk.size());
        for (String source : chunk) {
            translated.add(translateByTencentApi(source));
        }
        return translated;
    }

    private String translateByTencentApi(String sourceText) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String date = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE);

            Map<String, Object> payload = new HashMap<>();
            payload.put("SourceText", sourceText);
            payload.put("Source", sourceLang);
            payload.put("Target", targetLang);
            payload.put("ProjectId", tencentProjectId);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String signedHeaders = "content-type;host;x-tc-action";
            String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                    + "host:" + TENCENT_HOST + "\n"
                    + "x-tc-action:" + TENCENT_ACTION.toLowerCase(Locale.ROOT) + "\n";
            String canonicalRequest = "POST\n/\n\n"
                    + canonicalHeaders + "\n"
                    + signedHeaders + "\n"
                    + sha256Hex(payloadJson);

            String credentialScope = date + "/" + TENCENT_SERVICE + "/tc3_request";
            String stringToSign = "TC3-HMAC-SHA256\n"
                    + timestamp + "\n"
                    + credentialScope + "\n"
                    + sha256Hex(canonicalRequest);

            byte[] secretDate = hmacSha256(("TC3" + tencentSecretKey).getBytes(StandardCharsets.UTF_8), date);
            byte[] secretService = hmacSha256(secretDate, TENCENT_SERVICE);
            byte[] secretSigning = hmacSha256(secretService, "tc3_request");
            String signature = toHex(hmacSha256(secretSigning, stringToSign));

            String authorization = "TC3-HMAC-SHA256 "
                    + "Credential=" + tencentSecretId + "/" + credentialScope + ", "
                    + "SignedHeaders=" + signedHeaders + ", "
                    + "Signature=" + signature;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TENCENT_ENDPOINT))
                    .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 1)))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-TC-Action", TENCENT_ACTION)
                    .header("X-TC-Version", TENCENT_VERSION)
                    .header("X-TC-Region", tencentRegion)
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String raw = response.body() == null ? "" : response.body();
                String preview = raw.length() > 200 ? raw.substring(0, 200) + "..." : raw;
                throw new IllegalStateException("unexpected status: " + response.statusCode() + ", body=" + preview);
            }

            Map<String, Object> root = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object responseObj = root.get("Response");
            if (!(responseObj instanceof Map<?, ?> responseMap)) {
                throw new IllegalStateException("tencent response missing Response field");
            }

            Object errorObj = responseMap.get("Error");
            if (errorObj instanceof Map<?, ?> errorMap) {
                Object code = errorMap.get("Code");
                Object message = errorMap.get("Message");
                throw new BusinessException("TRANSLATION_FAILED", "腾讯翻译失败: " + code + " - " + message, HttpStatus.BAD_GATEWAY);
            }

            Object targetText = responseMap.get("TargetText");
            if (targetText == null) {
                throw new IllegalStateException("tencent response missing TargetText");
            }
            return String.valueOf(targetText);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
