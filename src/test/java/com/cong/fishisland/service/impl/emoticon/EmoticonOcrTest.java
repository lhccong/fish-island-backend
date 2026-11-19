package com.cong.fishisland.service.impl.emoticon;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class EmoticonOcrTest {
    @Test
    void recognizesAnImageWithThePackagedNativeRuntime() throws Exception {
        BufferedImage image = new BufferedImage(560, 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.drawString("FISHISLAND", 55, 103);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        Logger logger = (Logger) LoggerFactory.getLogger("io.github.mymonstercat.ocr.InferenceEngine");
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.WARN);
        try {
            assertEquals("FISHISLAND",
                    new EmoticonOcr().recognizeImage(EmoticonOcr.decodeFirstFrame(bytes.toByteArray())));
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void rejectsUnsupportedAndOversizedImages() throws Exception {
        assertNull(EmoticonOcr.decodeFirstFrame(new byte[]{1, 2, 3}));
        BufferedImage image = new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        assertNull(EmoticonOcr.decodeFirstFrame(bytes.toByteArray()));
        assertTrue(ImageIO.getImageReadersByFormatName("webp").hasNext());
    }

    @Test
    void acceptsOnlyPublicAddresses() throws Exception {
        assertFalse(EmoticonOcr.isPublicAddress(InetAddress.getLoopbackAddress()));
        assertFalse(EmoticonOcr.isPublicAddress(InetAddress.getByAddress(new byte[]{10, 0, 0, 1})));
        assertFalse(EmoticonOcr.isPublicAddress(InetAddress.getByAddress(new byte[]{100, 64, 0, 1})));
        assertTrue(EmoticonOcr.isPublicAddress(InetAddress.getByAddress(new byte[]{8, 8, 8, 8})));
    }
}
