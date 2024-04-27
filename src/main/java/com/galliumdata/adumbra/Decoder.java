package com.galliumdata.adumbra;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Extract the data hidden in a bitmap.
 */
public class Decoder {
    public byte[] decode(InputStream inStream, byte[] keyBytes) throws Exception {
        boolean originalHeadless = GraphicsEnvironment.isHeadless();
        System.setProperty("java.awt.headless", "true");

        ImageBitmap bitmap = new ImageBitmap();
        bitmap.readImage(inStream);

        // Hash the key
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] key = digest.digest(keyBytes);

        int keyIdx = 0;
        int pixelIdx = Byte.toUnsignedInt(key[keyIdx]) / 4;
        int bitIdx = 0;
        byte currentByte = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < bitmap.width * bitmap.height; i++) {
            if (i == pixelIdx) {
                int keyByte = Byte.toUnsignedInt(key[keyIdx % key.length]);
                byte pixelByte = bitmap.getPixelByte(i, keyByte/0x56);

                int bit = pixelByte & 1;
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
        if (numBytes < 0 || numBytes > (bitmap.width * bitmap.height)/400) {
            // This should catch most garbled values
            throw new RuntimeException("Invalid message size");
        }
        byte[] actualBytes = new byte[numBytes];
        System.arraycopy(msgBytes, 2, actualBytes, 0, numBytes);

        System.setProperty("java.awt.headless", originalHeadless ? "true" : "false");
        return actualBytes;
    }
}
