package com.ostsoft.jusb2snes;

public class USBVector {
    private int address;
    private final byte[] bytes;

    public USBVector(int address, int size) {
        this.address = address;
        this.bytes = new byte[size];
    }

    public USBVector(int address, byte[] bytes) {
        this.address = address;
        this.bytes = bytes;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getSize() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
