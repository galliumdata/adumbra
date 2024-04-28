package com.galliumdata.adumbra;

import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.formats.png.PngImageParser;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class ApacheTest {

    private static final String FILE = "/Users/maxtardiveau/IdeaProjects/adumbra/src/test/java/resources/Photo1.JPG";

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            JpegImageParser parser = new JpegImageParser();
            BufferedImage img = parser.getBufferedImage(new File(FILE), null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            parser.writeImage(img, baos, null);
            byte[] bytes = baos.toByteArray();
            System.out.println("Execution time: " + (System.currentTimeMillis() - start));
        }

        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            BufferedImage img = ImageIO.read(new File(FILE));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpeg", baos);
            byte[] bytes = baos.toByteArray();
            System.out.println("Execution time 2: " + (System.currentTimeMillis() - start));
        }
    }
}
