package com.galliumdata.adumbra;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Extract the data hidden in a bitmap.
 */
public class Decoder {

    /**
     * Given a bitmap that contains some hidden data, and the corresponding key,
     * returns the hidden data.
     * @param inStream The bitmap that contains the hidden data
     * @param keyBytes The key used to encode the hidden data
     * @return The hidden data
     */
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
        int numRead = 0;
        int numBytes = 0;
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
                    if (numRead == 0) {
                        numBytes = (currentByte - key[0]) << 8;
                    }
                    else if (numRead == 1) {
                        numBytes |= (currentByte - key[1]);
                    }
                    numRead++;
                    bitIdx = 0;
                    currentByte = 0;
                }
                keyIdx++;
                int keyIdxIncrement = Byte.toUnsignedInt(key[keyIdx % key.length]) / 4;
                if (keyIdxIncrement == 0) {
                    keyIdxIncrement = 1;
                }
                pixelIdx += keyIdxIncrement;

                if (numBytes > 0 && numRead >= numBytes + 2) {
                    break;
                }
            }
        }

        byte[] msgBytes = baos.toByteArray();
        for (int i = 0; i < msgBytes.length; i++) {
            msgBytes[i] -= key[i % key.length];
        }

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
