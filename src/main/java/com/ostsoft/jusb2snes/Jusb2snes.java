package com.ostsoft.jusb2snes;

import purejavacomm.*;

import java.io.*;

public class Jusb2snes {
    private OutputStream portOut;
    private InputStream portIn;
    private final SerialPort port;

    public Jusb2snes(String portName) throws NoSuchPortException, PortInUseException, IOException, UnsupportedCommOperationException {
        CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(portName);
        port = (SerialPort) portId.open(Jusb2snes.class.getSimpleName(), 3000);
        port.setSerialPortParams(916200, 8, 1, 0);
//        port.setSerialPortParams(9600, 8, 1, 0);
        port.setDTR(true);
        portOut = port.getOutputStream();
        portIn = port.getInputStream();
    }

    public byte[] readAddress(int offset, int size) throws IOException {
        byte[] requestBytes = getHeader(Opcode.USBINT_SERVER_OPCODE_GET, Space.USBINT_SERVER_SPACE_SNES, Flag.USBINT_SERVER_FLAGS_NONE);

        requestBytes[252] = (byte) ((size >> 24) & 0xFF);
        requestBytes[253] = (byte) ((size >> 16) & 0xFF);
        requestBytes[254] = (byte) ((size >> 8) & 0xFF);
        requestBytes[255] = (byte) (size & 0xFF);

        requestBytes[256] = (byte) ((offset >> 24) & 0xFF);
        requestBytes[257] = (byte) ((offset >> 16) & 0xFF);
        requestBytes[258] = (byte) ((offset >> 8) & 0xFF);
        requestBytes[259] = (byte) (offset & 0xFF);
        portOut.write(requestBytes);

        readRequestResponse();

        byte[] responseBytes = new byte[size];
        int n;
        int bytesRead = 0;
        while (bytesRead == 0 || (bytesRead % 512) != 0) {
            while ((n = portIn.available()) > 0) {
                for (int i = 0; i < n; ++i) {
                    byte read = (byte) portIn.read();
                    if (bytesRead >= 0 && bytesRead < responseBytes.length) {
                        responseBytes[bytesRead] = read;
                    }
                    bytesRead++;
                }
            }
        }
        return responseBytes;
    }

    public void writeAddress(int offset, byte[] buffer) throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_PUT, Space.USBINT_SERVER_SPACE_SNES, Flag.USBINT_SERVER_FLAGS_NONE);

        long size = buffer.length;
        bytes[252] = (byte) ((size >> 24) & 0xFF);
        bytes[253] = (byte) ((size >> 16) & 0xFF);
        bytes[254] = (byte) ((size >> 8) & 0xFF);
        bytes[255] = (byte) (size & 0xFF);

        bytes[256] = (byte) ((offset >> 24) & 0xFF);
        bytes[257] = (byte) ((offset >> 16) & 0xFF);
        bytes[258] = (byte) ((offset >> 8) & 0xFF);
        bytes[259] = (byte) (offset & 0xFF);

        portOut.write(bytes); // Write the request

        readRequestResponse(); // Read response

        portOut.write(buffer); // Write out data
        if (buffer.length % 512 != 0) {
            byte[] padding = new byte[512 - (buffer.length % 512)];
            portOut.write(padding);
        }
    }

    private void readRequestResponse() throws IOException {
        int n;
        int bytesRead = 0;

        while (bytesRead == 0 || (bytesRead % 512) != 0) {
            while ((n = portIn.available()) > 0) {
                for (int i = 0; i < n; ++i) {
                    if (bytesRead >= 512) {
                        return;
                    }
                    portIn.read();
                    bytesRead++;
                }
            }
        }
    }

    /**
     * Removes either a file or a folder from the SD-card
     *
     * @param remotePath Full path to the file or folder
     * @throws IOException
     */
    public void remove(String remotePath) throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_RM, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);
        byte[] fileNameBytes = remotePath.getBytes("ASCII");
        System.arraycopy(fileNameBytes, 0, bytes, 256, fileNameBytes.length);

        portOut.write(bytes); // Write the request
        readRequestResponse(); // Read response
    }

    /**
     * Making a new directory
     *
     * @param remotePath Path to new directory
     * @throws IOException
     */
    public void mkdir(String remotePath) throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_MKDIR, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);
        byte[] fileNameBytes = remotePath.getBytes("ASCII");
        System.arraycopy(fileNameBytes, 0, bytes, 256, fileNameBytes.length);

        portOut.write(bytes); // Write the request
        readRequestResponse(); // Read response
    }

    /**
     * Move a file or a folder to a new place
     *
     * @param remotePath            Source path starting with /
     * @param remoteDestinationPath Destination path starting with /
     * @throws IOException
     */
    public void move(String remotePath, String remoteDestinationPath) throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_MV, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);

        byte[] fileNameBytes = remotePath.getBytes("ASCII");
        System.arraycopy(fileNameBytes, 0, bytes, 256, fileNameBytes.length);

        byte[] destinationFileNameBytes = remoteDestinationPath.getBytes("ASCII");
        System.arraycopy(destinationFileNameBytes, 0, bytes, 8, destinationFileNameBytes.length);

        portOut.write(bytes); // Write the request
        readRequestResponse(); // Read response
    }

    /**
     * Writing a file to the SD-card, this overwrites the file if its already existing on the SD-card
     *
     * @param remotePath Full path to the destination file on SD-card starting with /
     * @param file       Source file on the host filesystem
     * @throws IOException
     */
    public void writeFile(String remotePath, File file) throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_PUT, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);

        byte[] fileNameBytes = remotePath.getBytes("ASCII");
        System.arraycopy(fileNameBytes, 0, bytes, 256, fileNameBytes.length);

        long size = file.length();
        bytes[252] = (byte) ((size >> 24) & 0xFF);
        bytes[253] = (byte) ((size >> 16) & 0xFF);
        bytes[254] = (byte) ((size >> 8) & 0xFF);
        bytes[255] = (byte) (size & 0xFF);

        portOut.write(bytes); // Write the request
        readRequestResponse(); // Read response

        FileInputStream fileInputStream = new FileInputStream(file);
        int writtenBytes = 0;
        while (writtenBytes < size) {
            int available = fileInputStream.available();
            int read = fileInputStream.read(bytes, 0, Math.min(available, bytes.length));
            portOut.write(bytes);
            writtenBytes += read;
        }
        fileInputStream.close();
    }

    private byte[] getHeader(Opcode opcode, Space space, Flag flag) {
        byte[] bytes = new byte[512];
        bytes[0] = 'U';
        bytes[1] = 'S';
        bytes[2] = 'B';
        bytes[3] = 'A';
        bytes[4] = opcode.getByte(); // opcode
        bytes[5] = space.getByte(); // space
        bytes[6] = flag.getByte(); // flags
        return bytes;
    }

    /**
     * Boots specified file on the SNES
     *
     * @param remotePath Full path to executable starts with /
     * @throws IOException
     */
    public void boot(String remotePath) throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_BOOT, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);
        byte[] fileNameBytes = remotePath.getBytes("ASCII");
        System.arraycopy(fileNameBytes, 0, bytes, 256, fileNameBytes.length);

        portOut.write(bytes); // Write the request
        readRequestResponse(); // Read response
    }

    public void close() {
        port.setDTR(false);
        try {
            portOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            portIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        port.close();
    }
}
