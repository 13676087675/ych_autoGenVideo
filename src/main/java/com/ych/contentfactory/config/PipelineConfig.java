package com.ych.contentfactory.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * 配置来源：环境变量（优先）→ 用户配置文件 → 默认值。
 * 默认配置文件：{@code ~/.ych/content-factory.properties}，见 {@link UserConfigLoader}。
 */
public final class PipelineConfig {

    public final String minimaxApiKey;
    public final String llmApiKey;
    public final String llmBaseUrl;
    public final String llmModel;
    public final String minimaxVoiceId;
    public final double minimaxSpeed;
    public final Path sofficePath;
    /** 是否在 Chat Completions 请求里附带 DeepSeek 的 thinking / reasoning_effort（仅建议在 DeepSeek 端点使用）。 */
    public final boolean deepSeekReasoningExtras;

    private PipelineConfig(
            String minimaxApiKey,
            String llmApiKey,
            String llmBaseUrl,
            String llmModel,
            String minimaxVoiceId,
            double minimaxSpeed,
            Path sofficePath,
            boolean deepSeekReasoningExtras
    ) {
        this.minimaxApiKey = minimaxApiKey;
        this.llmApiKey = llmApiKey;
        this.llmBaseUrl = llmBaseUrl;
        this.llmModel = llmModel;
        this.minimaxVoiceId = minimaxVoiceId;
        this.minimaxSpeed = minimaxSpeed;
        this.sofficePath = sofficePath;
        this.deepSeekReasoningExtras = deepSeekReasoningExtras;
    }

    public static PipelineConfig load() {
        Properties props = UserConfigLoader.load();

        String mm = firstNonBlank(
                System.getenv("MINIMAX_API_KEY"),
                System.getenv("CONTENT_FACTORY_MINIMAX_KEY"),
                propAny(props, "minimax.api.key", "MINIMAX_API_KEY"),
                "sk-api-dEAUDCXRIVFfMzF6PKxcjLw4pdPHzM3nluiuWRKKneApHQZ21g02olwaOLgS9zfsw7gKwKIXowgmX9hzzIRSazfemBNJIJkO8TlFmM6W62Qt-M2dFWucLsA"
        );
        String llmKey = firstNonBlank(
                System.getenv("DEEPSEEK_API_KEY"),
                System.getenv("CONTENT_FACTORY_LLM_KEY"),
                System.getenv("OPENAI_API_KEY"),
                propAny(props, "deepseek.api.key", "DEEPSEEK_API_KEY", "llm.api.key", "OPENAI_API_KEY"),
                "sk-f13fa0df2a534706a39937c4b8e602cb"
        );
        String base = trimTrailingSlash(firstNonBlank(
                System.getenv("CONTENT_FACTORY_LLM_BASE"),
                propAny(props, "llm.base.url", "CONTENT_FACTORY_LLM_BASE"),
                "https://api.deepseek.com"
        ));
        String model = firstNonBlank(
                System.getenv("CONTENT_FACTORY_LLM_MODEL"),
                propAny(props, "llm.model", "CONTENT_FACTORY_LLM_MODEL"),
                "deepseek-v4-pro"
        );
        String voice = firstNonBlank(
                System.getenv("CONTENT_FACTORY_MINIMAX_VOICE"),
                propAny(props, "minimax.voice", "CONTENT_FACTORY_MINIMAX_VOICE"),
                "male-qn-qingse"
        );
        double speed = parseDoubleSetting("CONTENT_FACTORY_MINIMAX_SPEED", props, "minimax.speed", 1.0);
        Path soffice = resolveSoffice(firstNonBlank(
                System.getenv("CONTENT_FACTORY_SOFFICE"),
                propAny(props, "soffice.path", "CONTENT_FACTORY_SOFFICE")
        ));
        boolean deepSeekExtras = resolveDeepSeekReasoningExtras(base, props);
        return new PipelineConfig(mm, llmKey, base, model, voice, speed, soffice, deepSeekExtras);
    }

    /**
     * 默认：若 Base URL 指向 DeepSeek，则开启与官方示例一致的 thinking / reasoning_effort。
     * 可通过环境变量 {@code CONTENT_FACTORY_DEEPSEEK_EXTRAS} 或配置项 {@code deepseek.extras} 覆盖。
     */
    private static boolean resolveDeepSeekReasoningExtras(String llmBaseUrl, Properties props) {
        String flag = firstNonBlank(
                System.getenv("CONTENT_FACTORY_DEEPSEEK_EXTRAS"),
                propAny(props, "deepseek.extras", "CONTENT_FACTORY_DEEPSEEK_EXTRAS")
        );
        if ("0".equals(flag) || "false".equalsIgnoreCase(flag) || "no".equalsIgnoreCase(flag)) {
            return false;
        }
        if ("1".equals(flag) || "true".equalsIgnoreCase(flag) || "yes".equalsIgnoreCase(flag)) {
            return true;
        }
        String h = llmBaseUrl.toLowerCase();
        return h.contains("deepseek.com");
    }

    public void validateOrThrow() {
        Path cfgPath = UserConfigLoader.resolvedConfigPath();
        if (isBlank(minimaxApiKey)) {
            throw new IllegalStateException(
                    "未配置 MiniMax 密钥：请设置环境变量 MINIMAX_API_KEY / CONTENT_FACTORY_MINIMAX_KEY，"
                            + "或在配置文件 minimax.api.key 中填写。配置文件路径: " + cfgPath.toAbsolutePath()
            );
        }
        if (isBlank(llmApiKey)) {
            throw new IllegalStateException(
                    "未配置大模型密钥：请设置 DEEPSEEK_API_KEY / CONTENT_FACTORY_LLM_KEY / OPENAI_API_KEY，"
                            + "或在配置文件中填写 deepseek.api.key（或 llm.api.key）。配置文件路径: " + cfgPath.toAbsolutePath()
            );
        }
        if (sofficePath == null || !Files.isExecutable(sofficePath)) {
            throw new IllegalStateException(
                    "未找到可执行的 LibreOffice（soffice）。请安装 LibreOffice，或设置环境变量 CONTENT_FACTORY_SOFFICE / 配置项 soffice.path。"
            );
        }
    }

    private static Path resolveSoffice(String explicit) {
        if (!explicit.isBlank()) {
            Path p = Path.of(explicit);
            if (Files.isExecutable(p)) {
                return p;
            }
        }
        Path mac = Path.of("/Applications/LibreOffice.app/Contents/MacOS/soffice");
        if (Files.isExecutable(mac)) {
            return mac;
        }
        String[] candidates = {"soffice", "/usr/bin/soffice", "/usr/local/bin/soffice"};
        for (String c : candidates) {
            Path p = whichOnPath(c);
            if (p != null && Files.isExecutable(p)) {
                return p;
            }
        }
        return null;
    }

    private static Path whichOnPath(String cmd) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }
        for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
            Path p = Path.of(dir, cmd);
            if (Files.isExecutable(p)) {
                return p;
            }
        }
        return null;
    }

    private static double parseDoubleSetting(String envName, Properties props, String propKey, double def) {
        String e = System.getenv(envName);
        if (!isBlank(e)) {
            try {
                return Double.parseDouble(e.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        String p = propAny(props, propKey);
        if (!isBlank(p)) {
            try {
                return Double.parseDouble(p.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private static String propAny(Properties props, String... keys) {
        if (props == null) {
            return "";
        }
        for (String k : keys) {
            String v = props.getProperty(k);
            if (!isBlank(v)) {
                return v.trim();
            }
        }
        return "";
    }

    private static String firstNonBlank(String... parts) {
        for (String p : parts) {
            if (!isBlank(p)) {
                return p.trim();
            }
        }
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
