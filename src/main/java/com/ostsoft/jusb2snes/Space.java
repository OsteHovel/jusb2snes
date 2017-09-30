package com.ostsoft.jusb2snes;

public enum Space {
    USBINT_SERVER_SPACE_FILE(0),
    USBINT_SERVER_SPACE_SNES(1),
    USBINT_SERVER_SPACE_MSU(2);

    private final int i;

    Space(int i) {
        this.i = i;
    }

    public int getI() {
        return i;
    }

    public byte getByte() {
        return (byte) i;
    }

}
