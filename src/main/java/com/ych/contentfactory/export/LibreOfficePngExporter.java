package com.ych.contentfactory.export;

import com.ych.contentfactory.config.PipelineConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 调用 LibreOffice 无界面将 .pptx 导出为 PNG 序列。
 */
public final class LibreOfficePngExporter {

    private static final Pattern SUFFIX_NUM = Pattern.compile(".*_(\\d+)\\.png$", Pattern.CASE_INSENSITIVE);

    private final Path soffice;

    public LibreOfficePngExporter(PipelineConfig cfg) {
        this.soffice = cfg.sofficePath;
    }

    /**
     * 将 pptx 转为 framesDir 下的 slide_01.png ...
     */
    public void exportPngs(Path pptxFile, Path framesDir) throws Exception {
        Files.createDirectories(framesDir);
        Path work = Files.createTempDirectory("ych-lo-");
        try {
            Path single = work.resolve("deck.pptx");
            Files.copy(pptxFile, single);

            ProcessBuilder pb = new ProcessBuilder(
                    soffice.toString(),
                    "--headless",
                    "--nologo",
                    "--nofirststartwizard",
                    "--convert-to",
                    "png",
                    "--outdir",
                    work.toAbsolutePath().toString(),
                    single.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();
            if (code != 0) {
                throw new IllegalStateException("LibreOffice 导出失败（exit=" + code + "）：\n" + out);
            }

            List<Path> pngs = new ArrayList<>(listPngs(work));
            if (pngs.isEmpty()) {
                throw new IllegalStateException("LibreOffice 未生成 PNG。进程输出：\n" + out);
            }
            pngs.sort(Comparator.comparingInt(LibreOfficePngExporter::pngOrderKey));

            int i = 1;
            for (Path src : pngs) {
                String name = String.format(Locale.ROOT, "slide_%02d.png", i++);
                Files.copy(src, framesDir.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            System.out.println("实际生成文件: " + java.util.Arrays.toString(Files.list(work).map(Path::getFileName).toArray()));
            deleteRecursive(work);
        }
    }

    private static List<Path> listPngs(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).toList();
        }
    }

    private static int pngOrderKey(Path p) {
        String n = p.getFileName().toString();
        Matcher m = SUFFIX_NUM.matcher(n);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static void deleteRecursive(Path root) {
        try {
            if (Files.notExists(root)) {
                return;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> paths = new ArrayList<>();
                walk.sorted(Comparator.reverseOrder()).forEach(paths::add);
                for (Path p : paths) {
                    Files.deleteIfExists(p);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
