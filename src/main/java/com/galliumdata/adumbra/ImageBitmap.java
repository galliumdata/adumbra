package com.galliumdata.adumbra;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Read a bitmap as a byte array, and allow access to individual pixel bytes, regardless of byte order.
 */
public class ImageBitmap {

    public BufferedImage image;
    public WritableRaster raster;
    public String format;
    public int width;
    public int height;
    public int pixelSize;
    public int[] bandOffsets;
    public byte[] bytes;

    public void readImage(InputStream inStream) throws IOException {

        ImageInputStream imageInputStr = ImageIO.createImageInputStream(inStream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStr);
        if ( ! readers.hasNext()) {
            throw new RuntimeException("Unsupported input file format");
        }
        ImageReader imageReader = readers.next();
        imageReader.setInput(imageInputStr);
        image = imageReader.read(0);
        if (image == null) {
            throw new RuntimeException("Unable to read input file");
        }
        format = imageReader.getFormatName();
        width = image.getWidth();
        height = image.getHeight();
        raster = image.getRaster();
        bytes = ((DataBufferByte)raster.getDataBuffer()).getData();
        imageInputStr.close();

        ComponentSampleModel model = (ComponentSampleModel)image.getSampleModel();
        pixelSize = model.getNumBands();
        bandOffsets = model.getBandOffsets();
    }

    /**
     * Get the value of one byte of one pixel
     * @param pixelIdx The pixel index
     * @param color The required color, 0=R, 1=G, 2=B
     * @return The value of the byte
     */
    public byte getPixelByte(int pixelIdx, int color) {
        if (color < 0 || color >= bandOffsets.length) {
            throw new RuntimeException("Invalid color index: " + color);
        }
        return bytes[(pixelIdx * pixelSize) + bandOffsets[color]];
    }

    /**
     * Set the value of one byte of one pixel.
     * @param idx The pixel index
     * @param color The required color, 0=R, 1=G, 2=B
     * @param val The value of the byte
     */
    public void setPixelByte(int idx, int color, byte val) {
        bytes[(idx * pixelSize) + bandOffsets[color]] = val;
    }
}
