package ee.taltech.wiseones.geotank.shared.util;

import com.esotericsoftware.minlog.Log;

/**
 * Logging utility that wraps Kryonet's Log and automatically detects the calling class.
 * Reduces boilerplate and eliminates manual class name strings.
 * Used by both client and server modules.
 * Usage:
 *   Logger.info("Player joined");
 *   Logger.debug("Current state: " + state);
 *   Logger.warn("Unexpected condition");
 *   Logger.error("Critical error", exception);
 */
public class Logger {
    private static final int STACK_TRACE_CALLER_INDEX = 2; // 0=getCallerClass, 1=log method, 2=actual caller

    private Logger() {
        // Utility class - no instantiation
    }

    /**
     * Logs an info-level message. Caller class is automatically detected.
     */
    public static void info(String message) {
        Log.info(getCallerClass(), message);
    }

    /**
     * Logs an info-level message with a Throwable.
     */
    public static void info(String message, Throwable exception) {
        Log.info(getCallerClass(), message, exception);
    }

    /**
     * Logs a debug-level message. Caller class is automatically detected.
     */
    public static void debug(String message) {
        Log.debug(getCallerClass(), message);
    }

    /**
     * Logs a debug-level message with a Throwable.
     */
    public static void debug(String message, Throwable exception) {
        Log.debug(getCallerClass(), message, exception);
    }

    /**
     * Logs a warning-level message. Caller class is automatically detected.
     */
    public static void warn(String message) {
        Log.warn(getCallerClass(), message);
    }

    /**
     * Logs a warning-level message with a Throwable.
     */
    public static void warn(String message, Throwable exception) {
        Log.warn(getCallerClass(), message, exception);
    }

    /**
     * Logs an error-level message. Caller class is automatically detected.
     */
    public static void error(String message) {
        Log.error(getCallerClass(), message);
    }

    /**
     * Logs an error-level message with a Throwable.
     */
    public static void error(String message, Throwable exception) {
        Log.error(getCallerClass(), message, exception);
    }

    /**
     * Gets the simple name of the calling class by examining the stack trace.
     * This eliminates the need to manually provide class names to every log call.
     */
    private static String getCallerClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > STACK_TRACE_CALLER_INDEX) {
            String fullClassName = stackTrace[STACK_TRACE_CALLER_INDEX].getClassName();
            String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);

            // Optionally include method name for better context
            String methodName = stackTrace[STACK_TRACE_CALLER_INDEX].getMethodName();
            return simpleClassName + "." + methodName;
        }
        return "Unknown";
    }
}
