package jssc.common;

public enum ConsoleColor {
    ANSI_RESET(0),
    ANSI_BLACK(30),
    ANSI_RED(31),
    ANSI_GREEN(32),
    ANSI_YELLOW(33),
    ANSI_BLUE(34),
    ANSI_PURPLE(35),
    ANSI_CYAN(36),
    ANSI_WHITE(37);

    String colorCode;
    ConsoleColor(int colorCode) {
        this.colorCode = "\u001B[" + colorCode + "m";
    }

    @Override
    public String toString() {
        return colorCode;
    }
}
