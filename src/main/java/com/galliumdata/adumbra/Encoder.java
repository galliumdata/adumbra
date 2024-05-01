package com.galliumdata.adumbra;

import javax.imageio.*;
import java.awt.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Encoding a message into a bitmap.
 */
public class Encoder {
    private final int secLevel;

    /**
     * Create a new Encoder with the given security level.
     * @param secLevel One of: 0=minimum security, 1=moderate security, 2=maximum security
     */
    public Encoder(int secLevel) {
        this.secLevel = secLevel;
    }
    /**
     * Encode a message steganographically into a bitmap, using a key.
     * The key is hashed into a 64-byte buffer and used to encode the message
     * and to distribute its bits into the bitmap.
     * @param inStream The original bitmap
     * @param outStream Where to write the result
     * @param outputFormat The output format, can be png or tiff
     * @param msg The message to be written into the bitmap, should be at most 200 times smaller than
     *            the number of pixels in the image.
     * @param rawKey The key to use
     */
    public void encode(InputStream inStream, OutputStream outStream, String outputFormat, byte[] msg, byte[] rawKey) throws Exception {
        boolean originalHeadless = GraphicsEnvironment.isHeadless();
        System.setProperty("java.awt.headless", "true");

        ImageBitmap bitmap = new ImageBitmap();
        bitmap.readImage(inStream);

        // Get all the randomness we'll need, since getting this one byte at a time is very expensive.
        SecureRandom rand = SecureRandom.getInstanceStrong();
        int numRandom = bitmap.width * bitmap.height;
        if (numRandom > 10_007 && secLevel < 2) {
            // Getting millions of random bytes is expensive
            numRandom = 10_007;
        }
        byte[] randBytes = null;
        if (secLevel > 0) {
            randBytes = new byte[numRandom];
            rand.nextBytes(randBytes);
        }

        // Copy the message
        byte[] msgBytes = new byte[2 + msg.length];
        System.arraycopy(msg, 0, msgBytes, 2, msg.length);
        msgBytes[0] = (byte)(msg.length >> 8);
        msgBytes[1] = (byte)msg.length;

        // Hash the key
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] key = digest.digest(rawKey);

        // Verify that the data will fit
        int total = 0;
        for (int i = 0; i < msgBytes.length * 8; i++) {
                total += Byte.toUnsignedInt(key[i % key.length]) / 4;
        }
        if (total >= (bitmap.width * bitmap.height)) {
            throw new RuntimeException("Image is not large enough to hold this data -- need at least " + total + " pixels");
        }

        // Add the hashed key to the message to randomize it
        for (int i = 0; i < msgBytes.length; i++) {
            msgBytes[i] += key[i % key.length];
        }

        // For each pixel, we flip R, G or B randomly for random bits, or based
        // on key value for data bits
        int keyIdx = 0;
        int pixelIdx = Byte.toUnsignedInt(key[keyIdx]) / 4;
        int markIdx = 0;
        int bitIdx = 0;
        for (int i = 0; i < bitmap.width * bitmap.height; i++) {
            int keyByte = Byte.toUnsignedInt(key[keyIdx % key.length]);
            byte pixelByte = bitmap.getPixelByte(i, keyByte/0x56);
            if (markIdx < msgBytes.length && i == pixelIdx) {
                // Contains a bit
                byte markByte = msgBytes[markIdx];
                int bit = (markByte >> bitIdx) & 0x01;
                if (bit == 0) {
                    pixelByte &= ~1;
                }
                else {
                    pixelByte |= 1;
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
            else if (randBytes != null && secLevel > 0) {
                // Random bit flip
                byte randByte = randBytes[i % numRandom];
                int randomBit = randByte & (0x01 << (keyByte % 8));
                if (randomBit == 0) {
                    pixelByte &= ~1;
                }
                else {
                    pixelByte |= 1;
                }
            }

            bitmap.setPixelByte(i, keyByte/0x56, pixelByte);
            if (markIdx >= msgBytes.length && randBytes == null) {
                break;
            }
        }

        bitmap.image.setData(bitmap.raster);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(bitmap.image, outputFormat, os);
        outStream.write(os.toByteArray());
        outStream.close();

        System.setProperty("java.awt.headless", originalHeadless ? "true" : "false");
    }
}
