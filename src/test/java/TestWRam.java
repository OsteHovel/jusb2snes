import com.ostsoft.jusb2snes.Jusb2snes;
import org.junit.jupiter.api.*;
import purejavacomm.*;

import java.io.IOException;

class TestWRam {

    @Test
    void read() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = new Jusb2snes(Config.portName);
        byte[] read = jusb2snes.read(0xF50000, 64);
        Assertions.assertNotNull(read);
        jusb2snes.close();
    }

}
