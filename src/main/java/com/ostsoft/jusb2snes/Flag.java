package com.ostsoft.jusb2snes;

import java.util.*;

public enum Flag {
    USBINT_SERVER_FLAGS_NONE(0),
    USBINT_SERVER_FLAGS_SKIPRESET(1),
    USBINT_SERVER_FLAGS_ONLYRESET(2),
    USBINT_SERVER_FLAGS_CLRX(4),
    USBINT_SERVER_FLAGS_SETX(8),
    USBINT_SERVER_FLAGS_NORESP(64),
    USBINT_SERVER_FLAGS_64BDATA(128);

    private final int value;

    Flag(int value) {
        this.value = value;
    }

    public byte getByte() {
        return (byte) value;
    }

    public Set<Flag> getFlags(int value) {
        Set<Flag> flags = new HashSet<>();
        for (Flag flag : flags) {
            if ((flag.getByte() & value) != 0) {
                flags.add(flag);
            }
        }

        return flags;
    }

    public static byte getByte(Collection<Flag> flags) {
        byte aByte = 0;
        for (Flag flag : flags) {
            aByte |= flag.getByte();
        }
        return (byte) (aByte & 0xFF);
    }
}
