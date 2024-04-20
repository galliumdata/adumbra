package com.galliumdata.adumbra;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Extract the data hidden in a bitmap.
 */
public class Decoder {
    public byte[] decode(InputStream inStream, String keyPhrase) throws Exception {
        boolean originalHeadless = GraphicsEnvironment.isHeadless();
        System.setProperty("java.awt.headless", "true");

        BufferedImage img = ImageIO.read(inStream);
        int width = img.getWidth();
        int height = img.getHeight();

        // Hash the key
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] key = digest.digest(keyPhrase.getBytes(StandardCharsets.UTF_8));

        int keyIdx = 0;
        int pixelIdx = Byte.toUnsignedInt(key[keyIdx]) / 4;
        int bitIdx = 0;
        byte currentByte = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < width * height; i++) {
            if (i == pixelIdx) {
                int pixelVal = img.getRGB(i % width, i / width);

                int bitShift = 0;
                int keyByte = Byte.toUnsignedInt(key[keyIdx % key.length]);
                if (keyByte > 85) {
                    bitShift = 8;
                }
                if (keyByte > 170) {
                    bitShift = 16;
                }
                int bit = pixelVal & (1 << bitShift);
                bit >>= bitShift;
                currentByte |= bit << bitIdx;

                bitIdx++;
                if (bitIdx >= 8) {
                    baos.write(currentByte);
                    bitIdx = 0;
                    currentByte = 0;
                }
                keyIdx++;
                int keyIdxIncrement = Byte.toUnsignedInt(key[keyIdx % key.length]) / 4;
                if (keyIdxIncrement == 0) {
                    keyIdxIncrement = 1;
                }
                pixelIdx += keyIdxIncrement;
            }
        }

        byte[] msgBytes = baos.toByteArray();
        for (int i = 0; i < msgBytes.length; i++) {
            msgBytes[i] -= key[i % key.length];
        }

        int numBytes = (msgBytes[0] << 8) + msgBytes[1];
        if (numBytes < 0 || numBytes > (width * height)/800) {
            throw new RuntimeException("Invalid message size");
        }
        byte[] actualBytes = new byte[numBytes];
        System.arraycopy(msgBytes, 2, actualBytes, 0, numBytes);

        System.setProperty("java.awt.headless", originalHeadless ? "true" : "false");
        return actualBytes;
    }
}
