package com.ych.contentfactory;

import com.ych.contentfactory.config.PipelineConfig;
import com.ych.contentfactory.config.UserConfigLoader;
import com.ych.contentfactory.pipeline.ContentPipeline;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "ych-content-factory",
        mixinStandardHelpOptions = true,
        description = "从粘贴的需求与资料生成 PPT、逐页配音与对齐视频（MiniMax TTS + FFmpeg）",
        subcommands = {ContentFactoryCli.CreateCommand.class, ContentFactoryCli.InitConfigCommand.class}
)
public final class ContentFactoryCli implements Callable<Integer> {

    public static void main(String[] args) {
        int code = new CommandLine(new ContentFactoryCli()).execute(args);
        System.exit(code);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "create", description = "读取多行需求与资料（stdin 或文件），运行完整流水线")
    static final class CreateCommand implements Callable<Integer> {

        @Option(names = {"-o", "--output"}, description = "输出根目录，默认 ./output")
        Path outputRoot = Path.of("output");

        @Parameters(arity = "0..1", paramLabel = "FILE", description = "需求文本文件；省略或填 - 表示从终端/标准输入读取（见下方说明）")
        Path inputFile;

        @Override
        public Integer call() throws Exception {
            String userRequest = readUserRequest();
            if (userRequest.isBlank()) {
                System.err.println("输入为空。终端交互：粘贴后单独一行输入 ### 回车结束；管道/heredoc：用 Ctrl+D 结束。");
                return 2;
            }

            PipelineConfig cfg = PipelineConfig.load();
            cfg.validateOrThrow();

            Path out = new ContentPipeline(cfg).run(userRequest.trim(), outputRoot);
            System.out.println("完成。输出目录: " + out.toAbsolutePath());
            return 0;
        }

        private String readUserRequest() throws Exception {
            if (inputFile != null && !"-".equals(inputFile.toString())) {
                return java.nio.file.Files.readString(inputFile, StandardCharsets.UTF_8);
            }
            if (System.console() != null) {
                return readInteractiveMultiline();
            }
            return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        }

        /**
         * 终端直接运行时的交互输入：多行粘贴，单独一行 {@code ###}（仅三个井号）表示结束。
         */
        private static String readInteractiveMultiline() throws Exception {
            System.out.println();
            System.out.println("请粘贴「需求说明 + 资料」，支持多行；");
            System.out.println("结束后请单独起一行输入 ### 并回车（正文中请勿单独使用仅含 ### 的一行）。");
            System.out.println();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if ("###".equals(line.trim())) {
                        break;
                    }
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
    }

    @Command(name = "init-config", description = "在用户主目录生成默认配置文件模板（若已存在则跳过）")
    static final class InitConfigCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            Path target = UserConfigLoader.DEFAULT_CONFIG_PATH;
            Files.createDirectories(target.getParent());
            if (Files.isRegularFile(target)) {
                System.out.println("配置文件已存在，未覆盖: " + target.toAbsolutePath());
                return 0;
            }
            try (InputStream in = ContentFactoryCli.class.getResourceAsStream("/content-factory.properties.example")) {
                if (in == null) {
                    System.err.println("未找到内置模板 content-factory.properties.example。");
                    return 1;
                }
                Files.copy(in, target);
            }
            System.out.println("已创建配置文件模板，请编辑并填写密钥: " + target.toAbsolutePath());
            return 0;
        }
    }
}
