package tj.patternhatch.util;

/**
 * [PatternHatch] 控制台日志开关：服务器上默认关闭，避免每个多方块机器
 * 每 tick 刷 setup 日志。config/patternhatch.cfg -> debug.enabled 可重新打开。
 */
public final class PatternHatchDebug {

    public static boolean enabled = false;

    private PatternHatchDebug() {
    }

    public static void log(String message) {
        if (enabled) {
            System.out.println(message);
        }
    }

    /** 重要警告（如缺材料、缓存丢弃）始终打印。 */
    public static void warn(String message) {
        System.out.println(message);
    }
}
