package com.ostsoft.jusb2snes;

public enum Flag {
    USBINT_SERVER_FLAGS_NONE(0),
    USBINT_SERVER_FLAGS_SKIPRESET(1),
    USBINT_SERVER_FLAGS_ONLYRESET(2),
    USBINT_SERVER_FLAGS_CLRX(4),
    USBINT_SERVER_FLAGS_SETX(8),;

    private final int i;

    Flag(int i) {
        this.i = i;
    }

    public int getI() {
        return i;
    }

    public byte getByte() {
        return (byte) i;
    }
}
