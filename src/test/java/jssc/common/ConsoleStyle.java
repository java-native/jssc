package jssc.common;

import org.junit.runner.Description;

import static jssc.common.ConsoleColor.*;

/**
 * Utility class for coloring a console message similar to JUnit 4
 */
public enum ConsoleStyle {
    INFO,
    WARNING,
    ERROR;

    public String colorize(String message) {
        // e.g. [INFO] --- surefire:3.0.0-M4:test (default-test) @ jssc ---
        return String.format("%s --- %s @ %s",
                getPrefix(),
                styleMessage(ANSI_GREEN, "surefire:" + message),
                styleMessage(ANSI_CYAN, "jssc"));
    }

    public String colorize(Description description) {
        return colorize(description.getMethodName());
    }

    private ConsoleColor getColor() {
        switch(this) {
            case ERROR:
                return ANSI_RED;
            case WARNING:
                return ANSI_YELLOW;
            case INFO:
            default:
                return ANSI_BLUE;
        }
    }

    private String styleSeverity() {
        return styleMessage(getColor(), name());
    }

    private static String styleMessage(ConsoleColor color, String message) {
        return color + message + ANSI_RESET;
    }

    private String getPrefix() {
        return ANSI_RESET + "[" + styleSeverity() + "]";
    }

}
