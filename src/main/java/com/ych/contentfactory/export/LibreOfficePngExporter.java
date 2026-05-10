package com.ych.contentfactory.export;

import com.ych.contentfactory.config.PipelineConfig;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * 稳定导出方案：PPTX → PDF (LibreOffice) → PNG 序列 (pdftoppm)
 * 修复 macOS/Linux 下文件名不匹配与文件系统延迟问题。
 */
public final class LibreOfficePngExporter {

    private final Path soffice;

    public LibreOfficePngExporter(PipelineConfig cfg) {
        this.soffice = cfg.sofficePath;
    }

    public void exportPngs(Path pptxFile, Path framesDir) throws Exception {
        Files.createDirectories(framesDir);
        Path work = Files.createTempDirectory("ych-lo-");
        try {
            // 1️⃣ 复制 PPTX 到临时目录，使用固定名称避免路径解析歧义
            Path srcPptx = work.resolve("input.pptx");
            Files.copy(pptxFile, srcPptx, StandardCopyOption.REPLACE_EXISTING);

            // 2️⃣ LibreOffice 转 PDF
            ProcessBuilder pbPdf = new ProcessBuilder(
                    soffice.toString(), "--headless", "--nologo", "--nofirststartwizard",
                    "--convert-to", "pdf",
                    "--outdir", work.toAbsolutePath().toString(),
                    srcPptx.toAbsolutePath().toString()
            );
            runAndCheck(pbPdf, "LibreOffice PDF 导出");

            // 3️⃣ 动态查找实际生成的 PDF（兼容 LO 不同版本的命名行为 + macOS 延迟）
            Path pdfFile = waitForPdf(work, 10);
            if (pdfFile == null) {
                throw new IllegalStateException("LibreOffice 未生成 PDF。请检查 PPTX 是否加密/损坏，或 LO 是否完整安装。");
            }

            // 4️⃣ PDF → PNG 序列
            String pdftoppm = findCommand("pdftoppm");
            if (pdftoppm == null) {
                throw new IllegalStateException("未找到 pdftoppm 命令。\n" +
                        "  macOS: brew install poppler\n" +
                        "  Ubuntu: sudo apt install poppler-utils");
            }

            ProcessBuilder pbPng = new ProcessBuilder(
                    pdftoppm, "-png", "-r", "150", "-f", "1",
                    pdfFile.toAbsolutePath().toString(),
                    framesDir.resolve("slide").toAbsolutePath().toString()
            );
            runAndCheck(pbPng, "pdftoppm PNG 拆分");

            // 5️⃣ 重命名对齐流水线预期：slide-1.png → slide_01.png
            renameToExpectedPattern(framesDir);

            long count = Files.list(framesDir).filter(p -> p.toString().endsWith(".png")).count();
            System.out.println("✅ 成功导出 " + count + " 页幻灯片 PNG。");

        } finally {
            deleteRecursive(work);
        }
    }

    private void runAndCheck(ProcessBuilder pb, String stepName) throws Exception {
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        boolean ok = p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
        if (!ok || p.exitValue() != 0) {
            p.destroyForcibly();
            throw new IllegalStateException(stepName + " 失败（exit=" + p.exitValue() + "）:\n" + out);
        }
    }

    /** 等待 PDF 文件出现（兼容 macOS FSEvents 延迟） */
    private Path waitForPdf(Path dir, int maxRetries) throws Exception {
        for (int i = 0; i < maxRetries; i++) {
            try (Stream<Path> s = Files.list(dir)) {
                Optional<Path> pdf = s.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).findFirst();
                if (pdf.isPresent()) return pdf.get();
            }
            Thread.sleep(100); // 每次等 100ms，最多 1s
        }
        return null;
    }

    private String findCommand(String cmd) {
        for (String path : System.getenv("PATH").split(File.pathSeparator)) {
            Path p = Paths.get(path, cmd);
            if (Files.exists(p) && Files.isExecutable(p)) return cmd;
        }
        return null;
    }

    private void renameToExpectedPattern(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().matches("slide-\\d+\\.png"))
                    .sorted(Comparator.comparingInt(p -> {
                        Matcher m = Pattern.compile("slide-(\\d+)\\.png").matcher(p.getFileName().toString());
                        return m.find() ? Integer.parseInt(m.group(1)) : 0;
                    }))
                    .forEach(p -> {
                        Matcher m = Pattern.compile("slide-(\\d+)\\.png").matcher(p.getFileName().toString());
                        if (m.find()) {
                            int idx = Integer.parseInt(m.group(1));
                            Path target = p.resolveSibling(String.format(Locale.ROOT, "slide_%02d.png", idx));
                            try { Files.move(p, target, StandardCopyOption.REPLACE_EXISTING); }
                            catch (Exception ignored) {}
                        }
                    });
        }
    }

    private static void deleteRecursive(Path root) {
        try {
            if (Files.notExists(root)) return;
            try (Stream<Path> walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
            }
        } catch (Exception ignored) {}
    }
}
