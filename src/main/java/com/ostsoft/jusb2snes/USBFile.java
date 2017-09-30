package com.ostsoft.jusb2snes;

public class USBFile {
    private String name;
    private int type;

    public USBFile(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDirectory() {
        return type == 0;
    }

    public boolean isFile() {
        return type == 1;
    }

    public boolean isSpecial() {
        return name.equals(".") || name.equals("..");
    }
}
