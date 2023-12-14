package cs451.utils;

/**
 * Utility class.
 */
public class Utils {

    //garbage collector upper bound in Mb
    private static final int GC_UPPER_BOUND = 50;

    /**
     * Check if the garbage collector should be called.
     * (if the memory in use is greater than the upper bound)
     */
    public static void checkGc() {
        Runtime runtime = Runtime.getRuntime();
        // To convert from Bytes to MegaBytes:
        // 1 MB = 1024 KB and 1 KB = 1024 Bytes.
        // Therefore, 1 MB = 1024 * 1024 Bytes.
        long MegaBytes = 1024 * 1024;
        long totalMemory = runtime.totalMemory() / MegaBytes;
        long freeMemory = runtime.freeMemory() / MegaBytes;
        long memoryInUse = totalMemory - freeMemory;
        if (memoryInUse > Utils.GC_UPPER_BOUND) {
            System.gc();
        }
    }

}
