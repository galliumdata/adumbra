package test.adumbra;

import org.junit.Test;

import com.galliumdata.adumbra.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class Test1 {

    private static final String RES_DIR = System.getProperty("user.home") + "/IdeaProjects/adumbra/src/test/java/resources";

    private static class TestCase {
        public String inFile;
        public String outFile;
        public byte[] message = "This is the message".getBytes(StandardCharsets.UTF_8);
        public byte[] key = "Secret key, do not reveal".getBytes(StandardCharsets.UTF_8);
        public String format = "png";
        public int numReps = 1;
    }

    private static final List<TestCase> testCases = new ArrayList<>();

    @Test
    public void test() throws Exception {

        loadTestCases();
        for (TestCase tc: testCases) {
            System.out.println("Testing: " + tc.inFile + " -> " + tc.outFile);
            for (int i = 0; i < tc.numReps; i++) {
                long startTime = System.currentTimeMillis();
                Encoder encoder = new Encoder();
                FileInputStream inStr = new FileInputStream(tc.inFile);
                FileOutputStream fos = new FileOutputStream(tc.outFile);
                encoder.encode(inStr, fos, tc.format, tc.message, tc.key);
                fos.close();
                System.out.println("Time for encoding: " + (System.currentTimeMillis() - startTime));

                startTime = System.currentTimeMillis();
                Decoder decoder = new Decoder();
                FileInputStream inStr2 = new FileInputStream(tc.outFile);
                byte[] decoded = decoder.decode(inStr2, tc.key);
                System.out.println("Time for decoding: " + (System.currentTimeMillis() - startTime));
                assertArrayEquals(tc.message, decoded);
            }
        }

        System.out.println("Test complete");
    }

    private void loadTestCases() {
        TestCase tc = new TestCase();
        tc.inFile = RES_DIR + "/Mexico1.png";
        tc.outFile = "/tmp/Mexico3_modif.png";
        tc.format = "png";
        tc.numReps = 1;
        testCases.add(tc);

        tc = new TestCase();
        tc.inFile = RES_DIR + "/Mexico1.png";
        tc.outFile = "/tmp/Mexico4_modif.tiff";
        tc.format = "tiff";
        tc.numReps = 1;
        testCases.add(tc);

        tc = new TestCase();
        tc.inFile = RES_DIR + "/owl.jpeg";
        tc.outFile = "/tmp/owl1_modif.png";
        tc.format = "png";
        tc.numReps = 1;
        testCases.add(tc);

        tc = new TestCase();
        tc.inFile = RES_DIR + "/owl.tiff";
        tc.outFile = "/tmp/owl2_modif.tiff";
        tc.format = "tiff";
        tc.numReps = 1;
        testCases.add(tc);

        tc = new TestCase();
        tc.inFile = RES_DIR + "/greenland.bmp";
        tc.outFile = "/tmp/greenland_modif.png";
        tc.format = "png";
        tc.numReps = 1;
        testCases.add(tc);
    }
}
