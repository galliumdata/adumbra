package com.galliumdata.stegano;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Iterator;

/**
 * Encoding a message into a bitmap.
 */
public class Encoder {
    /**
     * Encode a message steganographically into a bitmap, using a key.
     * The key is hashed into a 64-byte buffer.
     * @param inStream The original bitmap
     * @param outStream Where to write the result
     * @param format The output format, can be png
     * @param msg The message to be written into the bitmap, should be at most 200 times smaller than
     *            the number of pixels in the image.
     * @param keyPhrase The key to use
     */
    public void encode(InputStream inStream, OutputStream outStream, String format, byte[] msg, String keyPhrase) throws Exception {
        boolean originalHeadless = GraphicsEnvironment.isHeadless();
        System.setProperty("java.awt.headless", "true");

        // If no format specified, determine from the image
        if (format == null) {
            ImageInputStream input = ImageIO.createImageInputStream(inStream);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(input);
                reader.read(0);  // Read the same image as ImageIO.read
                format = reader.getFormatName();
            } else {
                throw new RuntimeException("Unable to determine file format");
            }
        }
        if ( ! "png".equalsIgnoreCase(format) && ! "tiff".equalsIgnoreCase(format)) {
            throw new RuntimeException("Unsupported output file format: " + format);
        }

        SecureRandom rand = SecureRandom.getInstanceStrong();

        BufferedImage img = ImageIO.read(inStream);
        if (img == null) {
            throw new RuntimeException("Unable to read input bitmap");
        }
        int width = img.getWidth();
        int height = img.getHeight();

        byte[] markBytes = new byte[2 + msg.length];
        System.arraycopy(msg, 0, markBytes, 2, msg.length);
        markBytes[0] = (byte)(msg.length >> 8);
        markBytes[1] = (byte)msg.length;

        // Hash the key
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] key = digest.digest(keyPhrase.getBytes(StandardCharsets.UTF_8));

        // Verify that the data will fit
        int total = 0;
        for (int i = 0; i < markBytes.length * 8; i++) {
                total += Byte.toUnsignedInt(key[i % key.length]) / 4;
        }
        if (total >= (width * height)) {
            throw new RuntimeException("Image is not large enough to hold this data -- need at least " + total + " pixels");
        }

        // Add the hashed key to the message to randomize it
        for (int i = 0; i < markBytes.length; i++) {
            markBytes[i] += key[i % key.length];
        }

        // We flip every pixel randomly, except the ones that contain our data.
        // For each pixel, we flip R, G or B randomly for random bits, or based
        // on key value for data bits
        int keyIdx = 0;
        int pixelIdx = Byte.toUnsignedInt(key[keyIdx]) / 4;
        int markIdx = 0;
        int bitIdx = 0;
        for (int i = 0; i < width * height; i++) {
            int pixelVal = img.getRGB(i % width, i / width);
            if (markIdx < markBytes.length && i == pixelIdx) {
                // Contains a bit
                int bitShift = 0;
                int keyByte = Byte.toUnsignedInt(key[keyIdx % key.length]);
                if (keyByte > 85) {
                    bitShift = 8;
                }
                if (keyByte > 170) {
                    bitShift = 16;
                }

                byte markByte = markBytes[markIdx];
                int bit = (markByte >> bitIdx) & 0x01;
                if (bit == 0) {
                    pixelVal &= ~(1 << bitShift);
                }
                else {
                    pixelVal |= (bit << bitShift);
                }

                bitIdx++;
                if (bitIdx >= 8) {
                    bitIdx = 0;
                    markIdx++;
                }
                keyIdx++;
                int increment = Byte.toUnsignedInt(key[keyIdx % key.length]) / 4;
                if (increment == 0) {
                    increment = 1;
                }
                pixelIdx += increment;
            }
            else {
                // Random bit flip
                int randomBit = rand.nextInt(2);
                int randomBitShift = rand.nextInt(3) * 8;
                if (randomBit == 0) {
                    pixelVal &= ~(1 << randomBitShift);
                }
                else {
                    pixelVal |= randomBit << randomBitShift;
                }
            }
            img.setRGB(i % width, i / width, pixelVal);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        if ("png".equalsIgnoreCase(format)) {
            ImageIO.write(img, format, os);
        }
        else if ("jpeg".equalsIgnoreCase(format)) {

            if (img.getColorModel().hasAlpha()) {
                BufferedImage target = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = target.createGraphics();
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.drawImage(img, 0, 0, null);
                g.dispose();
                img = target;
            }

            Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName(format);
            ImageWriter writer = imageWriters.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] compTypes = writeParam.getCompressionTypes();
            writeParam.setCompressionType("JPEG");
            writeParam.setCompressionQuality(1.0f);
            writer.prepareWriteSequence(null);
            writer.write(img);
            ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(img);
            javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(spec, writeParam);
            IIOImage iioImage = new IIOImage(img, null, metadata);
            writer.writeToSequence(iioImage, writeParam);
            img.flush();
            writer.endWriteSequence();
            writer.dispose();
            os.close();
        }
        else if ("tiff".equalsIgnoreCase(format)) {
            ImageIO.write(img, format, os);
//            Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName(format);
//            ImageWriter writer = imageWriters.next();
//            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
//            writer.setOutput(ios);
//            ImageWriteParam writeParam = writer.getDefaultWriteParam();
//            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//            String[] compTypes = writeParam.getCompressionTypes();
//            writeParam.setCompressionType("LZW");
//            writeParam.setCompressionQuality(1.0f);
//            writer.prepareWriteSequence(null);
//            writer.write(img);
//            ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(img);
//            javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(spec, writeParam);
//            IIOImage iioImage = new IIOImage(img, null, metadata);
//            writer.writeToSequence(iioImage, writeParam);
//            img.flush();
//            writer.endWriteSequence();
//            writer.dispose();
//            os.close();
        }
        else if ("wbmp".equalsIgnoreCase(format)) {
            ImageIO.write(img, format, os);
        }

        outStream.write(os.toByteArray());
        outStream.close();

        System.setProperty("java.awt.headless", originalHeadless ? "true" : "false");
    }
}
