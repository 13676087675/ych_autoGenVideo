package com.ych.contentfactory;

/**
 * Java 应用入口。未带参数时默认执行 {@code create}（终端多行粘贴 + 以单独一行 ### 结束）。
 */
public final class Application {

    private Application() {
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"create"};
        }
        ContentFactoryCli.main(args);
    }
}
