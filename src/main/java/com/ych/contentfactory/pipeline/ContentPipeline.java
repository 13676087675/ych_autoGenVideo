package com.ych.contentfactory.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ych.contentfactory.config.PipelineConfig;
import com.ych.contentfactory.export.LibreOfficePngExporter;
import com.ych.contentfactory.llm.LlmPlanGenerator;
import com.ych.contentfactory.media.FfmpegUtil;
import com.ych.contentfactory.model.PresentationPlan;
import com.ych.contentfactory.ppt.PptxBuilder;
import com.ych.contentfactory.tts.MinimaxTtsClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 编排：LLM 结构化 → PPTX → PNG → 逐页 TTS → 合并音频 → FFmpeg 成片。
 */
public final class ContentPipeline {

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Pattern SAFE_DIR = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5._-]+");

    private final PipelineConfig cfg;

    public ContentPipeline(PipelineConfig cfg) {
        this.cfg = cfg;
    }

    public Path run(String userRequest, Path outputRoot) throws Exception {
        FfmpegUtil.requireFfmpegOnPath();

        PresentationPlan plan = new LlmPlanGenerator(cfg).generate(userRequest);
        String slug = slugDirName(plan.title);
        Path job = outputRoot.resolve(slug);
        Path outlineDir = job.resolve("02_outline");
        Path pptDir = job.resolve("03_ppt");
        Path framesDir = job.resolve("04_slides").resolve("frames");
        Path scriptDir = job.resolve("05_script");
        Path audioDir = job.resolve("06_audio");
        Path videoDir = job.resolve("07_video");

        Files.createDirectories(outlineDir);
        Files.createDirectories(pptDir);
        Files.createDirectories(framesDir);
        Files.createDirectories(scriptDir);
        Files.createDirectories(audioDir);
        Files.createDirectories(videoDir);

        Path planFile = outlineDir.resolve("plan.json");
        JSON.writeValue(planFile.toFile(), plan);
        Files.writeString(scriptDir.resolve("script_per_slide.md"), buildScriptMarkdown(plan), StandardCharsets.UTF_8);

        Path pptx = pptDir.resolve("slides.pptx");
        System.out.println("生成 PPTX…");
        PptxBuilder.write(plan, pptx);

        System.out.println("LibreOffice 导出 PNG…");
        new LibreOfficePngExporter(cfg).exportPngs(pptx, framesDir);

        int frameCount = countSlidePngs(framesDir);
        if (frameCount != plan.totalSlides()) {
            throw new IllegalStateException(
                    "导出帧数量与 PPT 页数不一致：PNG=" + frameCount + "，期望=" + plan.totalSlides()
                            + "。请检查 LibreOffice 导出或 PPTX 页数。"
            );
        }

        Map<String, String> narrations = orderedNarrations(plan);
        if (narrations.size() != plan.totalSlides()) {
            throw new IllegalStateException("旁白页数与幻灯片不一致");
        }

        System.out.println("MiniMax 逐页配音…");
        Map<String, Double> durations = new MinimaxTtsClient(cfg).synthesizeOrdered(narrations, audioDir);

        List<Path> mp3Order = new ArrayList<>();
        for (String key : narrations.keySet()) {
            mp3Order.add(audioDir.resolve(String.format(Locale.ROOT, "slide_%s.mp3", key)));
        }
        Path merged = audioDir.resolve("narration_merged.mp3");
        System.out.println("合并配音…");
        FfmpegUtil.mergeMp3s(mp3Order, merged);

        Path mp4 = videoDir.resolve("final_synced.mp4");
        System.out.println("FFmpeg 合成视频…");
        FfmpegUtil.synthesizeVideoByDurations(framesDir, merged, mp4, durations, 25);

        Files.writeString(job.resolve("meta.txt"), "topic=" + plan.title + "\n", StandardCharsets.UTF_8);
        System.out.println("视频：" + mp4.toAbsolutePath());
        return job;
    }

    private static Map<String, String> orderedNarrations(PresentationPlan plan) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("01", plan.openingNarration);
        int n = plan.bodySlides.size();
        for (int i = 0; i < n; i++) {
            String key = String.format(Locale.ROOT, "%02d", i + 2);
            m.put(key, plan.bodySlides.get(i).narration);
        }
        m.put(String.format(Locale.ROOT, "%02d", n + 2), plan.closingNarration);
        return m;
    }

    private static String buildScriptMarkdown(PresentationPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 逐页旁白\n\n");
        sb.append("## Slide 01 - 封面\n\n").append(plan.openingNarration).append("\n\n---\n\n");
        int idx = 2;
        for (PresentationPlan.BodySlide s : plan.bodySlides) {
            sb.append("## Slide ").append(String.format(Locale.ROOT, "%02d", idx++))
                    .append(" - ").append(s.heading).append("\n\n")
                    .append(s.narration).append("\n\n---\n\n");
        }
        sb.append("## Slide ").append(String.format(Locale.ROOT, "%02d", idx)).append(" - 结束\n\n")
                .append(plan.closingNarration).append("\n");
        return sb.toString();
    }

    private static int countSlidePngs(Path framesDir) throws Exception {
        try (Stream<Path> s = Files.list(framesDir)) {
            return (int) s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .count();
        }
    }


    private static String slugDirName(String title) {
        String t = title == null ? "job" : title.trim();
        if (t.isEmpty()) {
            t = "job";
        }
        String s = SAFE_DIR.matcher(t).replaceAll("-");
        s = s.replaceAll("-{2,}", "-").replaceAll("^-+|-+$", "");
        if (s.isBlank()) {
            s = "job-" + (title != null ? Integer.toHexString(title.hashCode()) : "out");
        }
        if (s.length() > 60) {
            s = s.substring(0, 60);
        }
        return s;
    }
}
