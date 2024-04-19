package com.galliumdata.stegano;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new RuntimeException("First argument must be \"encode\" or \"decode\"");
        }

        String arg0 = args[0].toLowerCase();
        if ("encode".equals(arg0)) {
            if (args.length < 5) {
                throw new RuntimeException("Parameters for encode must be: encode <input-file> <output-file> <message> <key> [<format>]");
            }
            FileInputStream inStr = new FileInputStream(args[1]);

            String format = "png";

            ImageInputStream input = ImageIO.createImageInputStream(inStr);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(input);
                reader.read(0);  // Read the same image as ImageIO.read
                format = reader.getFormatName();
            } else {
                throw new RuntimeException("Unable to determine file format");
            }
            if (args.length == 6) {
                format = args[5];
            }

            FileOutputStream fos = new FileOutputStream(args[2]);
            String message = args[3];
            String key = args[4];
            Encoder encoder = new Encoder();
            inStr = new FileInputStream(args[1]);
            encoder.encode(inStr, fos,  format, message.getBytes(StandardCharsets.UTF_8), key);
            fos.close();
        }
        else if ("decode".equals(arg0)) {
            if (args.length != 3) {
                throw new RuntimeException("Parameters for decode must be: decode <input-file> <key>");
            }
            FileInputStream inStr = new FileInputStream(args[1]);
            String key = args[2];
            Decoder decoder = new Decoder();
            byte[] message = decoder.decode(inStr, key);
            System.out.println("Secret message is: " + new String(message, StandardCharsets.UTF_8));
        }
        else {
            throw new RuntimeException("First argument must be \"encode\" or \"decode\"");
        }
    }
}
