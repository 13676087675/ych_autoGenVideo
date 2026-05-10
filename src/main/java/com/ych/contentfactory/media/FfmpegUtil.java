package com.ych.contentfactory.media;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 调用本机 ffmpeg / ffprobe（需已安装并在 PATH 中）。
 */
public final class FfmpegUtil {

    private FfmpegUtil() {
    }

    public static void requireFfmpegOnPath() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException("未找到可用的 ffmpeg，请安装并加入 PATH（例如 macOS: brew install ffmpeg）。");
        }
        ProcessBuilder pp = new ProcessBuilder("ffprobe", "-version");
        pp.redirectErrorStream(true);
        Process p2 = pp.start();
        int c2 = p2.waitFor();
        if (c2 != 0) {
            throw new IllegalStateException("未找到可用的 ffprobe（通常随 ffmpeg 一起安装）。");
        }
    }

    public static double audioDurationSeconds(Path audio) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                audio.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException("ffprobe 失败: " + out);
        }
        return Double.parseDouble(out);
    }

    public static void mergeMp3s(List<Path> filesInOrder, Path outMp3) throws Exception {
        Files.createDirectories(outPathParent(outMp3));
        Path list = Files.createTempFile("ych-audio-concat-", ".txt");
        try {
            StringBuilder sb = new StringBuilder();
            for (Path f : filesInOrder) {
                sb.append("file ").append(escapeConcatPath(f.toAbsolutePath().toString())).append('\n');
            }
            Files.writeString(list, sb.toString(), StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-f", "concat", "-safe", "0",
                    "-i", list.toAbsolutePath().toString(),
                    "-c", "copy",
                    outMp3.toAbsolutePath().toString()
            );
            runOrThrow(pb, "合并音频");
        } finally {
            Files.deleteIfExists(list);
        }
    }

    /**
     * 按每页时长将静帧序列与整段配音合成视频（对齐 bajie video_synthesizer.py）。
     */
    public static void synthesizeVideoByDurations(
            Path framesDir,
            Path mergedAudio,
            Path outMp4,
            Map<String, Double> orderedSlideKeyToDurationSeconds,
            int fps
    ) throws Exception {
        Files.createDirectories(outPathParent(outMp4));
        Path concat = Files.createTempFile("ych-video-concat-", ".txt");
        Path tempVideo = Files.createTempFile("ych-video-only-", ".mp4");
        try {
            StringBuilder sb = new StringBuilder();
            List<String> keys = new ArrayList<>(orderedSlideKeyToDurationSeconds.keySet());
            for (String key : keys) {
                double dur = orderedSlideKeyToDurationSeconds.get(key);
                Path frame = framesDir.resolve(String.format(Locale.ROOT, "slide_%s.png", key));
                if (Files.notExists(frame)) {
                    throw new IllegalStateException("缺少帧文件：" + frame);
                }
                sb.append("file ").append(escapeConcatPath(frame.toAbsolutePath().toString())).append('\n');
                sb.append("duration ").append(dur).append('\n');
            }
            if (!keys.isEmpty()) {
                String lastKey = keys.get(keys.size() - 1);
                Path lastFrame = framesDir.resolve(String.format(Locale.ROOT, "slide_%s.png", lastKey));
                sb.append("file ").append(escapeConcatPath(lastFrame.toAbsolutePath().toString())).append('\n');
            }
            Files.writeString(concat, sb.toString(), StandardCharsets.UTF_8);

            ProcessBuilder v = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-f", "concat", "-safe", "0",
                    "-i", concat.toAbsolutePath().toString(),
                    "-vsync", "cfr",
                    "-vf", "fps=" + fps + ",scale=1920:1080:force_original_aspect_ratio=decrease,"
                            + "pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1",
                    "-c:v", "libx264", "-preset", "fast", "-crf", "23", "-pix_fmt", "yuv420p",
                    tempVideo.toAbsolutePath().toString()
            );
            runOrThrow(v, "静帧合成视频");

            ProcessBuilder mux = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", tempVideo.toAbsolutePath().toString(),
                    "-i", mergedAudio.toAbsolutePath().toString(),
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                    "-shortest",
                    outMp4.toAbsolutePath().toString()
            );
            runOrThrow(mux, "合并音频到视频");
        } finally {
            Files.deleteIfExists(concat);
            Files.deleteIfExists(tempVideo);
        }
    }

    private static Path outPathParent(Path p) {
        Path parent = p.getParent();
        return parent == null ? Path.of(".") : parent;
    }

    private static String escapeConcatPath(String absolutePath) {
        String s = absolutePath.replace("'", "'\\''");
        return "'" + s + "'";
    }

    private static void runOrThrow(ProcessBuilder pb, String step) throws Exception {
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException(step + " 失败（exit=" + code + "）：\n" + out);
        }
    }
}
