package com.galliumdata.adumbra;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new RuntimeException("First argument must be \"encode\" or \"decode\"");
        }

        String arg0 = args[0].toLowerCase().trim();
        if ("encode".equals(arg0)) {
            if (args.length < 5) {
                throw new RuntimeException("Parameters for encode must be: encode <input-file> <output-file> <message> <key> [<format>]");
            }

            String format = null;
            if (args.length == 6) {
                format = args[5];
            }

            FileOutputStream fos = new FileOutputStream(args[2]);
            String message = args[3];
            String key = args[4];
            Encoder encoder = new Encoder();
            FileInputStream inStr = new FileInputStream(args[1]);
            encoder.encode(inStr, fos,  format, message.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
            fos.close();
        }
        else if ("decode".equals(arg0)) {
            if (args.length != 3) {
                throw new RuntimeException("Parameters for decode must be: decode <input-file> <key>");
            }
            FileInputStream inStr = new FileInputStream(args[1]);
            String key = args[2];
            Decoder decoder = new Decoder();
            byte[] message = decoder.decode(inStr, key.getBytes(StandardCharsets.UTF_8));
            System.out.println("Hidden message: " + new String(message, StandardCharsets.UTF_8));
        }
        else {
            throw new RuntimeException("First argument must be either \"encode\" or \"decode\"");
        }
    }
}
