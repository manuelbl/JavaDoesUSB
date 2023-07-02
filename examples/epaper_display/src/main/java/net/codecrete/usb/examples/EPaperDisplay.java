//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Example program for displaying an image and text on an e-paper display
 * controlled by an IT8951 controller.
 */
public class EPaperDisplay {

    public static void main(String[] args) throws IOException {
        // load image of tiger
        var tigerImage = ImageIO.read(new File("tiger.jpg"));

        // connect to display controller
        var display = new IT8951Driver();
        display.open();

        var width = display.info().width();
        var height = display.info().height();
        System.out.printf("Display size: %d x %d%n", width, height);

        // resize image to fit display (converting it to grayscale)
        var image = resizedImage(tigerImage, width, height);

        // add text to image
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Java Does USB", 30, height - 40);
        g.dispose();

        // display image
        display.displayImage(image, 0, 0);

        display.close();
    }

    /**
     * Returns a copy of the image, resized to the specified width and height.
     * <p>
     * To prevent distortion, the image is cropped to fit the target width and height.
     * </p>
     * <p>
     * The new image will be in 8-bit grayscale.
     * </p>
     * @param image image to resize
     * @param width width, in pixels
     * @param height height, in pixels
     * @return resized image
     */
    static BufferedImage resizedImage(BufferedImage image, int width, int height) {

        var resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        var g = (Graphics2D) resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();
        var targetAspectRatio = (double) width / height;
        var srcAspectRatio = (double) srcWidth / srcHeight;

        if (targetAspectRatio > srcAspectRatio) {
            // target image is wider than source image - fit width and cut top and bottom
            var modifiedHeight = width / srcAspectRatio;
            var cutY = (int) Math.round((modifiedHeight - height) / 2);
            g.drawImage(image, 0, -cutY, width, height + cutY, 0, 0, srcWidth, srcHeight, null);
        } else {
            // target image is narrower than source image - fit height and cut left and right
            var modifiedWidth = height * srcAspectRatio;
            var cutX = (int) Math.round((modifiedWidth - width) / 2);
            g.drawImage(image, -cutX, 0, width + cutX, height, 0, 0, srcWidth, srcHeight, null);
        }

        g.dispose();
        return resizedImage;
    }
}
