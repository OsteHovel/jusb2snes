package com.ostsoft.jusb2snes;

public enum Opcode {
    // address space operations
    USBINT_SERVER_OPCODE_GET(0),
    USBINT_SERVER_OPCODE_PUT(1),
    USBINT_SERVER_OPCODE_VGET(2),
    USBINT_SERVER_OPCODE_VPUT(3),

    // file system operations
    USBINT_SERVER_OPCODE_LS(4),
    USBINT_SERVER_OPCODE_MKDIR(5),
    USBINT_SERVER_OPCODE_RM(6),
    USBINT_SERVER_OPCODE_MV(7),

    // special operations
    USBINT_SERVER_OPCODE_RESET(8),
    USBINT_SERVER_OPCODE_BOOT(9),
    USBINT_SERVER_OPCODE_MENU_LOCK(10),
    USBINT_SERVER_OPCODE_INFO(11),
    USBINT_SERVER_OPCODE_MENU_RESET(12),
    USBINT_SERVER_OPCODE_STREAM(13),
    USBINT_SERVER_OPCODE_TIME(14),

    // response
    USBINT_SERVER_OPCODE_RESPONSE(15);

    private final int i;
    private Object aByte;

    Opcode(int i) {
        this.i = i;
    }

    public int getI() {
        return i;
    }

    public byte getByte() {
        return (byte) i;
    }
}
