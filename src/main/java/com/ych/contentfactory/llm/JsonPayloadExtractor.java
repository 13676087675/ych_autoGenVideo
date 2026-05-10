package com.ych.contentfactory.llm;

/**
 * 从模型输出中截取第一个完整 JSON 对象（兼容偶发的 ```json 围栏）。
 */
final class JsonPayloadExtractor {

    private JsonPayloadExtractor() {
    }

    static String extractJsonObject(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1).trim();
            }
            int endFence = s.lastIndexOf("```");
            if (endFence > 0) {
                s = s.substring(0, endFence).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("无法在模型输出中定位 JSON 对象");
        }
        return s.substring(start, end + 1);
    }
}
