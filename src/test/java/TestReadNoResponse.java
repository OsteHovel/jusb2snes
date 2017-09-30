import com.ostsoft.jusb2snes.Flag;
import com.ostsoft.jusb2snes.Jusb2snes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.UnsupportedCommOperationException;

import java.io.IOException;
import java.util.Arrays;

public class TestReadNoResponse {

    @Test
    void read() throws PortInUseException, UnsupportedCommOperationException, NoSuchPortException, IOException {
        Jusb2snes jusb2snes = null;
        try {
            jusb2snes = new Jusb2snes(Config.portName);

            int[] lengthRanges = new int[]{512, 128, 256, 1024, 2048, 64, 128, 32, 16, 8, 3, 33, 95, 63, 65, 2040};
            for (int lengthRange : lengthRanges) {
//                System.out.println("\n\nRead request for " + lengthRange + " bytes");
//                System.out.println("With Response header:");
                byte[] readWithResponse = jusb2snes.read(0x000000, lengthRange, Arrays.asList(Flag.USBINT_SERVER_FLAGS_64BDATA));
//                System.out.println("\nWith NO Response header:");
                byte[] readNoResponse = jusb2snes.read(0x000000, lengthRange, Arrays.asList(Flag.USBINT_SERVER_FLAGS_64BDATA, Flag.USBINT_SERVER_FLAGS_NORESP));
                Assertions.assertTrue(Arrays.equals(readWithResponse, readNoResponse));
            }
        }
        finally {
            if (jusb2snes != null) {
                jusb2snes.close();
            }
        }
    }

}
