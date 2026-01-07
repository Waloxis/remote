package common;

public final class Protocol {
    private Protocol() {}

    public static final byte AUTH = 2;

    public static final byte FRAME = 1;

    public static final byte MOUSE_MOVE = 10;
    public static final byte MOUSE_BTN  = 11;
    public static final byte MOUSE_WHEEL = 12;
    public static final byte KEY = 13;
}
