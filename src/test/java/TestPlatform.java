import com.ostsoft.jusb2snes.Jusb2snes;
import org.junit.jupiter.api.Assertions;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

import java.io.IOException;

class TestPlatform {
    public static final String portName = "COM25";

    public static Jusb2snes getJusb2snes() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Assertions.assertNotNull(portName);
        return new Jusb2snes(TestPlatform.portName);
    }

}
