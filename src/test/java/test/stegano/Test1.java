package test.stegano;

import org.junit.Test;

import com.galliumdata.stegano.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class Test1 {

    private static final String RES_DIR = "/Users/maxtardiveau/IdeaProjects/steganosaurus/src/test/java/resources";
    private static final String ORIGINAL_FILE_1 = RES_DIR + "/Mexico1.png";
    private static final String MODIFIED_FILE_1 = "/Users/maxtardiveau/Desktop/Mexico3_modif.png";
    private static final String KEY = "This is the key";
    private static final byte[] MESSAGE_BYTES = "This is a test".getBytes(StandardCharsets.UTF_8);

    @Test
    public void test() throws Exception {
        Encoder encoder = new Encoder();
        FileInputStream inStr = new FileInputStream(ORIGINAL_FILE_1);
        FileOutputStream fos = new FileOutputStream(MODIFIED_FILE_1);
        encoder.encode(inStr, fos,  "png", MESSAGE_BYTES, KEY);
        fos.close();

        Decoder decoder = new Decoder();
        FileInputStream inStr2 = new FileInputStream(MODIFIED_FILE_1);
        byte[] decoded = decoder.decode(inStr2, KEY);
        assertArrayEquals(MESSAGE_BYTES, decoded);

        System.out.println("Test complete");
    }
}
