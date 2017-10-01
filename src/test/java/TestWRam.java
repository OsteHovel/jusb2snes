import com.ostsoft.jusb2snes.Jusb2snes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

import java.io.IOException;

class TestWRam {

    @Test
    void read() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();

        byte[] read = jusb2snes.read(0xF50000, 64);
        Assertions.assertNotNull(read);
        jusb2snes.close();
    }

}
