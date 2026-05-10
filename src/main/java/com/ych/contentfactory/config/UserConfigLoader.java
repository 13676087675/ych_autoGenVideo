package com.ych.contentfactory.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 从用户主目录（或指定路径）加载持久化配置。
 * <p>
 * 默认文件：{@code ~/.ych/content-factory.properties}<br>
 * 可通过环境变量 {@code CONTENT_FACTORY_CONFIG} 或 JVM 属性 {@code content.factory.config} 指定其它路径。
 */
public final class UserConfigLoader {

    public static final Path DEFAULT_CONFIG_PATH =
            Path.of(System.getProperty("user.home"), ".ych", "content-factory.properties");

    private UserConfigLoader() {
    }

    public static Path resolvedConfigPath() {
        String fromEnv = trimOrNull(System.getenv("CONTENT_FACTORY_CONFIG"));
        if (fromEnv != null) {
            return Path.of(fromEnv);
        }
        String fromProp = trimOrNull(System.getProperty("content.factory.config"));
        if (fromProp != null) {
            return Path.of(fromProp);
        }
        return DEFAULT_CONFIG_PATH;
    }

    /**
     * 若文件不存在则返回空 Properties（不抛错）。
     */
    public static Properties load() {
        Properties props = new Properties();
        Path path = resolvedConfigPath();
        if (!Files.isRegularFile(path)) {
            return props;
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取配置文件: " + path.toAbsolutePath(), e);
        }
        return props;
    }

    private static String trimOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
