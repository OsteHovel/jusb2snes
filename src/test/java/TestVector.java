import com.ostsoft.jusb2snes.*;
import org.junit.jupiter.api.*;
import purejavacomm.*;

import java.io.IOException;
import java.util.*;

public class TestVector {

    @Test
    void read() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();

        List<USBVector> vectors = new ArrayList<>();
        // RG: FA0000-FBFFFF should be safe to perform read and write compares in the menu and games.  Although,
        // It's a function of what games, patches, and the menu use.
        // F00000-F4FFFF is used for saved state.  Ok to read and write + compare, but a write may corrupt the
        // save state.
        // F50000-F9FFFF is used for the current state so that constantly changes and will
        // fail compare on different reads/writes
        vectors.add(new USBVector(0xFA0A1C, 32));
        vectors.add(new USBVector(0xFA0938, 64));
        vectors.add(new USBVector(0xFA0421, 33));
        vectors.add(new USBVector(0xFA0643, 1));
        vectors.add(new USBVector(0xFA0123, 127));
        vectors.add(new USBVector(0xFA1234, 2));
        vectors.add(new USBVector(0xFA0000, 255));
        vectors.add(new USBVector(0xFAFCDE, 254));

        jusb2snes.read(vectors);

        for (USBVector vector : vectors) {
            if (vector.getSize() <= 0) {
                continue;
            }

            byte[] read = jusb2snes.read(vector.getAddress(), vector.getSize());
            Assertions.assertTrue(Arrays.equals(vector.getBytes(), read));
        }
        jusb2snes.close();
    }

    @Test
    void write() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();

        List<USBVector> vectors = new ArrayList<>();
        vectors.add(new USBVector(0xFA0A1C, 32));
        vectors.add(new USBVector(0xFA0938, 64));
        vectors.add(new USBVector(0xFAFCAA, 33));
        vectors.add(new USBVector(0xFA0643, 1));
        vectors.add(new USBVector(0xFA0123, 127));
        vectors.add(new USBVector(0xFA1234, 2));
        vectors.add(new USBVector(0xFACACE, 255));
        vectors.add(new USBVector(0xFAFCDE, 254));

        Random random = new Random();
        for (USBVector vector : vectors) {
            random.nextBytes(vector.getBytes());
        }

        jusb2snes.write(vectors);

        for (USBVector vector : vectors) {
            if (vector.getSize() <= 0) {
                continue;
            }

            byte[] read = jusb2snes.read(vector.getAddress(), vector.getSize());
            Assertions.assertTrue(Arrays.equals(vector.getBytes(), read));
        }
        jusb2snes.close();
    }

    @Test
    void readRandom() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();

        Random random = new Random();
        List<USBVector> vectors = new ArrayList<>();
        // RG: access FA0000-FBFFFE with 1-8 vectors and sizes 1-255
        for (int i = 0; i < 1 + random.nextInt(8); i++) {
            vectors.add(new USBVector(0xFA0000 + random.nextInt(0x20000-0x100), 1 + random.nextInt(255)));
        }

        jusb2snes.read(vectors);

        for (USBVector vector : vectors) {
            if (vector.getSize() <= 0) {
                continue;
            }

            byte[] read = jusb2snes.read(vector.getAddress(), vector.getSize());
            Assertions.assertTrue(Arrays.equals(vector.getBytes(), read));
        }
        jusb2snes.close();
    }

    @Test
    void writeRandom() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();

        Random random = new Random();
        List<USBVector> vectors = new ArrayList<>();
        for (int i = 0; i < 1 + random.nextInt(8); i++) {
            vectors.add(new USBVector(0xFA0000 + random.nextInt(0x20000-0x100), 1 + random.nextInt(255)));
        }

        for (USBVector vector : vectors) {
            random.nextBytes(vector.getBytes());
        }

        jusb2snes.write(vectors);

        for (USBVector vector : vectors) {
            if (vector.getSize() <= 0) {
                continue;
            }

            byte[] read = jusb2snes.read(vector.getAddress(), vector.getSize());
            Assertions.assertTrue(Arrays.equals(vector.getBytes(), read));
        }
        jusb2snes.close();
    }

}
