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
        vectors.add(new USBVector(0xF50A1C, 32));
        vectors.add(new USBVector(0xF50938, 64));
        vectors.add(new USBVector(0xF50421, 33));
        vectors.add(new USBVector(0xF50643, 1));
        vectors.add(new USBVector(0xF50123, 127));
        vectors.add(new USBVector(0xF01234, 2));
        vectors.add(new USBVector(0xF50000, 255));
        vectors.add(new USBVector(0xF8FCDE, 254));

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
        vectors.add(new USBVector(0xF50A1C, 32));
        vectors.add(new USBVector(0xF50938, 64));
        vectors.add(new USBVector(0xF5FCAA, 33));
        vectors.add(new USBVector(0xF50643, 1));
        vectors.add(new USBVector(0xF50123, 127));
        vectors.add(new USBVector(0xF01234, 2));
        vectors.add(new USBVector(0xF5CACE, 255));
        vectors.add(new USBVector(0xF8FCDE, 254));

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

}
