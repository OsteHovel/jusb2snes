import com.ostsoft.jusb2snes.Jusb2snes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestVersion {
    private transient static Logger logger = Logger.getLogger(TestVersion.class.getName());

    @Test
    void getVersion() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();
        int version = jusb2snes.getVersion();
        logger.log(Level.INFO, "Version as HEX: 0x" + Integer.toHexString(version).toUpperCase());
        Assertions.assertFalse(version == 0);
        jusb2snes.close();
    }

    @Test
    void obsoleteVersionCheck() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();
        int version = jusb2snes.getVersion();
        if (version == 0x44534E53) {
            logger.log(Level.WARNING, "Experimental firmware detected");
        }
        int versionNumber = version & 0x7FFFFFFF;
        Assertions.assertTrue(versionNumber >= 1, "Old firmware detected");
        jusb2snes.close();
    }

    @Test
    void getVersionString() throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        Jusb2snes jusb2snes = TestPlatform.getJusb2snes();
        String versionString = jusb2snes.getVersionString();
        Assertions.assertNotNull(versionString);
        logger.log(Level.INFO, "Version as string: " + versionString);
        jusb2snes.close();
    }


}
