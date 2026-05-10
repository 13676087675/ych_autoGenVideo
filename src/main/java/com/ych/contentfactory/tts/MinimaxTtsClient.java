package com.ych.contentfactory.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ych.contentfactory.config.PipelineConfig;
import com.ych.contentfactory.media.FfmpegUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MiniMax TTS（speech-2.8-hd），与 bajie-skills content-factory 脚本一致。
 */
public final class MinimaxTtsClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final URI API = URI.create("https://api.minimaxi.com/v1/t2a_v2");

    private final PipelineConfig cfg;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public MinimaxTtsClient(PipelineConfig cfg) {
        this.cfg = cfg;
    }

    /**
     * 按顺序生成 slide_01 ... 的 mp3，返回有序时长表（秒）。
     */
    public Map<String, Double> synthesizeOrdered(Map<String, String> slideKeyToText, Path audioDir) throws Exception {
        Files.createDirectories(audioDir);
        Map<String, Double> durations = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : slideKeyToText.entrySet()) {
            String key = e.getKey();
            String text = e.getValue();
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("旁白为空：" + key);
            }
            Path out = audioDir.resolve("slide_" + key + ".mp3");
            double sec = synthesizeOne(text, out);
            durations.put(key, sec);
            Thread.sleep(300);
        }
        return durations;
    }

    private double synthesizeOne(String text, Path outFile) throws Exception {
        String payload = MAPPER.writeValueAsString(Map.of(
                "model", "speech-2.8-hd",
                "text", text,
                "stream", false,
                "output_format", "url",
                "voice_setting", Map.of(
                        "voice_id", cfg.minimaxVoiceId,
                        "speed", cfg.minimaxSpeed,
                        "volume", 1.0,
                        "pitch", 0
                )
        ));

        HttpRequest req = HttpRequest.newBuilder(API)
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + cfg.minimaxApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("MiniMax TTS HTTP " + resp.statusCode() + "：" + resp.body());
        }

        JsonNode root = MAPPER.readTree(resp.body());
        JsonNode base = root.path("base_resp");
        if (base.path("status_code").asInt(-1) != 0) {
            throw new IllegalStateException("MiniMax 错误：" + base.path("status_msg").asText());
        }
        String url = root.path("data").path("audio").asText("");
        if (url.isBlank()) {
            throw new IllegalStateException("MiniMax 响应缺少音频 URL：" + resp.body());
        }
        long ms = root.path("extra_info").path("audio_length").asLong(0);
        double sec = ms > 0 ? ms / 1000.0 : 0;

        download(url, outFile);
        if (Files.notExists(outFile) || Files.size(outFile) == 0) {
            throw new IllegalStateException("音频下载失败或文件为空：" + outFile);
        }
        if (sec <= 0) {
            sec = FfmpegUtil.audioDurationSeconds(outFile);
        }
        return sec;
    }

    private void download(String url, Path dest) throws Exception {
        HttpRequest get = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<byte[]> bin = http.send(get, HttpResponse.BodyHandlers.ofByteArray());
        if (bin.statusCode() < 200 || bin.statusCode() >= 300) {
            throw new IllegalStateException("下载音频失败 HTTP " + bin.statusCode());
        }
        Files.write(dest, bin.body());
    }
}
