package com.ostsoft.jusb2snes;

import purejavacomm.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class Jusb2snes {
    private static final boolean DEBUG = false;
    private transient static Logger logger = Logger.getLogger(Jusb2snes.class.getName());
    private transient final SerialPort port;
    private OutputStream portOut;
    private InputStream portIn;

    public Jusb2snes(String portName) throws NoSuchPortException, PortInUseException, IOException, UnsupportedCommOperationException {
        CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(portName);
        port = (SerialPort) portId.open(Jusb2snes.class.getSimpleName(), 3000);
        port.notifyOnDataAvailable(true);
        port.notifyOnOutputEmpty(true);
        port.setSerialPortParams(9600, 8, 1, 0);
        port.setDTR(true);
        port.enableReceiveTimeout(5555);

        portOut = port.getOutputStream();
        portIn = port.getInputStream();
    }

    private static String toHex(int value) {
        String s = Integer.toHexString(value & 0xFF).toUpperCase();
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

    public byte[] read(int offset, int size) throws IOException {
        return read(offset, size, Arrays.asList(Flag.USBINT_SERVER_FLAGS_64BDATA));
    }

    public byte[] read(int offset, int size, List<Flag> flags) throws IOException {
        byte[] requestBytes = getHeader(Opcode.USBINT_SERVER_OPCODE_GET, Space.USBINT_SERVER_SPACE_SNES, flags);

        requestBytes[252] = (byte) ((size >> 24) & 0xFF);
        requestBytes[253] = (byte) ((size >> 16) & 0xFF);
        requestBytes[254] = (byte) ((size >> 8) & 0xFF);
        requestBytes[255] = (byte) (size & 0xFF);

        requestBytes[256] = (byte) ((offset >> 24) & 0xFF);
        requestBytes[257] = (byte) ((offset >> 16) & 0xFF);
        requestBytes[258] = (byte) ((offset >> 8) & 0xFF);
        requestBytes[259] = (byte) (offset & 0xFF);
        writeBytes(requestBytes);

        if (!flags.contains(Flag.USBINT_SERVER_FLAGS_NORESP)) {
            readBytes(512);
        }

        byte[] responseBytes = readBytes(size);
        int align = flags.contains(Flag.USBINT_SERVER_FLAGS_64BDATA) ? 64 : 512;
        int paddingToRead = getAligned(size, align) - size;
        if (paddingToRead > 0) {
            readBytes(paddingToRead);
        }

        if (portIn.available() > 0) {
            logger.log(Level.SEVERE, "There is " + portIn.available() + " bytes left in the buffer from SNES");
        }
        return responseBytes;
    }

    public void write(int offset, byte[] buffer) throws IOException {
        List<Flag> flags = Arrays.asList(Flag.USBINT_SERVER_FLAGS_64BDATA);
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_PUT, Space.USBINT_SERVER_SPACE_SNES, flags);

        long size = buffer.length;
        bytes[252] = (byte) ((size >> 24) & 0xFF);
        bytes[253] = (byte) ((size >> 16) & 0xFF);
        bytes[254] = (byte) ((size >> 8) & 0xFF);
        bytes[255] = (byte) (size & 0xFF);

        bytes[256] = (byte) ((offset >> 24) & 0xFF);
        bytes[257] = (byte) ((offset >> 16) & 0xFF);
        bytes[258] = (byte) ((offset >> 8) & 0xFF);
        bytes[259] = (byte) (offset & 0xFF);

        writeBytes(bytes);

        if (!flags.contains(Flag.USBINT_SERVER_FLAGS_NORESP)) {
            readBytes(512);
        }

        writeBytes(buffer);
        if (buffer.length % 64 != 0) {
            byte[] padding = new byte[64 - (buffer.length % 64)];
            writeBytes(padding);
        }
    }

    public void read(List<USBVector> vectors) throws IOException {
        byte[] requestBytes = new byte[64];
        requestBytes[0] = 'U';
        requestBytes[1] = 'S';
        requestBytes[2] = 'B';
        requestBytes[3] = 'A';
        requestBytes[4] = Opcode.USBINT_SERVER_OPCODE_VGET.getByte(); // opcode
        requestBytes[5] = Space.USBINT_SERVER_SPACE_SNES.getByte(); // space
        List<Flag> flags = Arrays.asList(Flag.USBINT_SERVER_FLAGS_64BDATA);
        requestBytes[6] = Flag.getByte(flags); // flags

        int numberOfVectors = vectors.size();
        if (numberOfVectors > 8) {
            numberOfVectors = 8;
        }

        int totalSize = 0;
        for (int i = 0; i < numberOfVectors; i++) {
            USBVector vector = vectors.get(i);
            totalSize += vector.getSize();

            requestBytes[32 + i * 4] = (byte) (vector.getSize() & 0xFF);
            requestBytes[33 + i * 4] = (byte) ((vector.getAddress() >> 16) & 0xFF);
            requestBytes[34 + i * 4] = (byte) ((vector.getAddress() >> 8) & 0xFF);
            requestBytes[35 + i * 4] = (byte) ((vector.getAddress()) & 0xFF);
        }
        writeBytes(requestBytes);

        if (!flags.contains(Flag.USBINT_SERVER_FLAGS_NORESP)) {
            readBytes(512);
        }

        int aligned = getAligned(totalSize, 64);
        byte[] bytes = readBytes(aligned);
        int offset = 0;
        for (int i = 0; i < numberOfVectors; i++) {
            USBVector vector = vectors.get(i);
            System.arraycopy(bytes, offset, vector.getBytes(), 0, vector.getBytes().length);
            offset += vector.getBytes().length;
        }

        int available = portIn.available();
        if (available > 0) {
            if (DEBUG) {
                System.out.println(available + " bytes available to read on the input stream:");
                printBytes(readBytes(available));
            }
            logger.log(Level.SEVERE, "There is " + available + " bytes left in the buffer from SNES");
        }
    }

    public void write(List<USBVector> vectors) throws IOException {
        byte[] requestBytes = new byte[64];
        requestBytes[0] = 'U';
        requestBytes[1] = 'S';
        requestBytes[2] = 'B';
        requestBytes[3] = 'A';
        requestBytes[4] = Opcode.USBINT_SERVER_OPCODE_VPUT.getByte(); // opcode
        requestBytes[5] = Space.USBINT_SERVER_SPACE_SNES.getByte(); // space
        List<Flag> flags = Arrays.asList(Flag.USBINT_SERVER_FLAGS_64BDATA);
        requestBytes[6] = Flag.getByte(flags); // flags

        int numberOfVectors = vectors.size();
        if (numberOfVectors > 8) {
            numberOfVectors = 8;
        }

        int totalSize = 0;
        for (int i = 0; i < numberOfVectors; i++) {
            USBVector vector = vectors.get(i);
            totalSize += vector.getSize();
            requestBytes[32 + i * 4] = (byte) (vector.getSize() & 0xFF);
            requestBytes[33 + i * 4] = (byte) ((vector.getAddress() >> 16) & 0xFF);
            requestBytes[34 + i * 4] = (byte) ((vector.getAddress() >> 8) & 0xFF);
            requestBytes[35 + i * 4] = (byte) ((vector.getAddress()) & 0xFF);
        }
        writeBytes(requestBytes);

        if (!flags.contains(Flag.USBINT_SERVER_FLAGS_NORESP)) {
            readBytes(512);
        }

        for (int i = 0; i < numberOfVectors; i++) {
            USBVector vector = vectors.get(i);
            writeBytes(vector.getBytes());
        }
        writeBytes(new byte[getAligned(totalSize, 64) - totalSize]);
    }

    private int getAligned(int value, int align) {
        int adjusted = ((value) / align) * align;
        if (value % align != 0) {
            adjusted += align;
        }
        return adjusted;
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

        writeBytes(bytes);
        readBytes(512);
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

        writeBytes(bytes);
        readBytes(512);
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

        writeBytes(bytes);
        readBytes(512);
    }

    public boolean readFile(String remotePath, File file) throws IOException {
        byte[] requestBytes = getHeader(Opcode.USBINT_SERVER_OPCODE_GET, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);

        byte[] fileNameBytes = remotePath.getBytes("ASCII");
        System.arraycopy(fileNameBytes, 0, requestBytes, 256, fileNameBytes.length);

        writeBytes(requestBytes);
        byte[] response = readBytes(512);// Read response
        int size = ((response[252] & 0xFF) << 24) + ((response[253] & 0xFF) << 16) + ((response[254] & 0xFF) << 8) + (response[255] & 0xFF);


        FileOutputStream fileOutputStream = new FileOutputStream(file);

        int bytesRead = 0;
        int responseSize = getAligned(size, 512);

        byte[] receiveBytes = new byte[512];
        while (bytesRead < responseSize) {
            int clamped = responseSize - bytesRead;
            if (clamped > receiveBytes.length) {
                clamped = receiveBytes.length;
            }

            int bytesReadThisRequest = portIn.read(receiveBytes, 0, clamped);
            if (bytesReadThisRequest == 0) {
                System.out.println("Got 0 bytes");
                fileOutputStream.close();
                return false;
            }

            if (bytesReadThisRequest == -1) {
                System.out.println("The end has been reached");
                fileOutputStream.close();
                return false;
            }

            if (bytesRead < size) {
                int bytesToWrite = bytesReadThisRequest;
                if (bytesRead + bytesReadThisRequest > size) {
                    bytesToWrite = size - bytesRead;
                }
//                System.out.println("We are writing " + bytesToWrite);
                fileOutputStream.write(receiveBytes, 0, bytesToWrite);
            }

            bytesRead = bytesRead + bytesReadThisRequest;
//            System.out.println("Bytes read: " + bytesRead + " / " + responseSize + " / " + size + " (this turn we got " + bytesReadThisRequest + "  bytes)" + portIn.available());
        }
//        System.out.println("Dunzo");
        fileOutputStream.close();
        return true;
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

        writeBytes(bytes);
        readBytes(512);

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

    public List<USBFile> list(String remotePath) throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_LS, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);

        byte[] fileNameBytes = remotePath.getBytes("ASCII");
        System.arraycopy(fileNameBytes, 0, bytes, 256, fileNameBytes.length);
        writeBytes(bytes);
        readBytes(512);

        int type = 0;

        List<USBFile> files = new ArrayList<>();
        byte[] receiveBytes = new byte[512];
        while (type != 0xFF) {
            int bytesReadThisRequest = portIn.read(receiveBytes);
            if (bytesReadThisRequest == 0) {
                break;
            }
            // parse strings
            for (int i = 0; i < 512; ) {
                type = receiveBytes[i++] & 0xFF;
                if (type == 0 || type == 1) {
                    StringBuilder name = new StringBuilder(20);
                    while (receiveBytes[i] != 0x0) {
                        name.append((char) receiveBytes[i++]);
                    }
                    i++;
                    files.add(new USBFile(type, name.toString()));
                }
                else if (type == 2 || type == 0xFF) {
                    // continued on the next packet
                    break;
                }
                else {
                    logger.log(Level.SEVERE, "During listing of a directory an error occurred, this should never happen");
                }
            }

        }
        return files;
    }

    private byte[] getHeader(Opcode opcode, Space space, Flag flag) {
        return getHeader(opcode, space, Collections.singleton(flag));
    }

    private byte[] getHeader(Opcode opcode, Space space, Collection<Flag> flags) {
        byte[] bytes = new byte[512];
        bytes[0] = 'U';
        bytes[1] = 'S';
        bytes[2] = 'B';
        bytes[3] = 'A';
        bytes[4] = opcode.getByte(); // opcode
        bytes[5] = space.getByte(); // space
        bytes[6] = Flag.getByte(flags); // flags
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

        writeBytes(bytes);
        readBytes(512);
    }

    private byte[] readBytes(int size) throws IOException {
        int bytesRead = 0;
        byte[] readBytes = new byte[size];
        while (bytesRead < size) {
            int len = Math.min(size - bytesRead, 64);

            int bytesReadThisRequest = portIn.read(readBytes, bytesRead, len);
            if (bytesReadThisRequest == 0) {
                logger.log(Level.WARNING, "While reading it only returned 0 bytes");
                throw new IOException("While reading it only returned 0 bytes");
            }

            if (bytesReadThisRequest == -1) {
                logger.log(Level.WARNING, "The end has been reached (got -1 while reading)");
                throw new IOException("The end has been reached (got -1 while reading)");
            }

//            System.out.println("Bytes read: " + bytesRead + " -> " + (bytesRead + bytesReadThisRequest) + " / " + size + ", this turn we got " + bytesReadThisRequest + "  bytes of max read per request at " + len + ", its " + portIn.available() + " bytes left in the que");
            bytesRead = bytesRead + bytesReadThisRequest;
        }

        if (DEBUG) {
            System.out.print("Read " + size + " bytes: ");
            printBytes(readBytes);
            System.out.println("Bytes available to read on the inputstream: " + portIn.available());
        }
        return readBytes;
    }

    /**
     * Boots to menu on the SNES
     *
     * @throws IOException
     */
    public void bootMenu() throws IOException {
        byte[] bytes = getHeader(Opcode.USBINT_SERVER_OPCODE_MENU_RESET, Space.USBINT_SERVER_SPACE_FILE, Flag.USBINT_SERVER_FLAGS_NONE);
        writeBytes(bytes);
        readBytes(512);
    }

    /**
     * Firmware version as int
     * Experimental versions is 0x44534E53
     *
     * @return 0x80000000 + version number
     * @throws IOException
     */
    public int getVersion() throws IOException {
        byte[] requestBytes = getHeader(Opcode.USBINT_SERVER_OPCODE_INFO, Space.USBINT_SERVER_SPACE_SNES, Flag.USBINT_SERVER_FLAGS_NONE);
        writeBytes(requestBytes);
        byte[] responseBytes = readBytes(512);
        return getWord(responseBytes, 256);
    }

    /**
     * Gives you the firmware version as string
     *
     * @return Firmware version string
     * @throws IOException
     */
    public String getVersionString() throws IOException {
        byte[] requestBytes = getHeader(Opcode.USBINT_SERVER_OPCODE_INFO, Space.USBINT_SERVER_SPACE_SNES, Flag.USBINT_SERVER_FLAGS_NONE);
        writeBytes(requestBytes);
        byte[] responseBytes = readBytes(512);
        StringBuilder stringBuilder = new StringBuilder(25);
        int offset = 260;
        while (responseBytes[offset] != 0 && offset < 512) {
            stringBuilder.append((char) responseBytes[offset]);
            offset++;
        }

        return stringBuilder.toString();
    }

    private void writeBytes(byte[] bytes) throws IOException {
        if (DEBUG) {
            System.out.print("Write: ");
            printBytes(bytes);
        }
        portOut.write(bytes);
    }

    private void printBytes(byte[] bytes) {
        for (byte aByte : bytes) {
            System.out.print(toHex(aByte) + " ");
        }
        System.out.println();
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

    private int getWord(byte[] bytes, int offset) {
        return (Byte.toUnsignedInt(bytes[offset]) << 24 | Byte.toUnsignedInt(bytes[offset + 1]) << 16 | Byte.toUnsignedInt(bytes[offset + 2]) << 8 | Byte.toUnsignedInt(bytes[offset + 3]));
    }
}
