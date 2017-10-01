import com.ostsoft.jusb2snes.Jusb2snes;
import org.junit.jupiter.api.*;
import purejavacomm.*;

import java.io.IOException;
import java.util.Arrays;

public class TestRom {

    @Test
    void read() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();
        byte[] read = jusb2snes.read(0xF50000, 64);
        Assertions.assertNotNull(read);
        jusb2snes.close();
    }

    @Test
    void writeAndVerifyEvenSizes() throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        int[] lengthRanges = new int[]{8, 16, 32, 64, 1024};
        for (int lengthRange : lengthRanges) {
            writeAndRead(lengthRange);
        }
    }

    @Test
    void writeAndVerifyOddSizes() throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        int[] lengthRanges = new int[]{7, 15, 17, 29, 33, 65, 63, 1023, 1025};
        for (int lengthRange : lengthRanges) {
            writeAndRead(lengthRange);
        }
    }

    private void writeAndRead(int length) throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();
        byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        jusb2snes.write(0x000000, bytes);
        byte[] readBack = jusb2snes.read(0x000000, bytes.length);

        Assertions.assertTrue(Arrays.equals(bytes, readBack));
        jusb2snes.close();
    }
}
