package com.cong.fishisland.service.impl.emoticon;

import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import io.github.mymonstercat.ocr.config.HardwareConfig;
import io.github.mymonstercat.ocr.config.ParamConfig;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
class EmoticonOcr {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PIXELS = 4_000_000;
    private static final int MAX_SIDE = 4096;
    private static final int MAX_TEXT = 4000;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .dns(EmoticonOcr::lookupPublicAddresses)
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .callTimeout(6, TimeUnit.SECONDS)
            .build();

    String recognize(String url) throws IOException {
        if (url == null) {
            return null;
        }
        byte[] bytes = download(HttpUrl.parse(url));
        if (bytes == null) {
            return null;
        }
        BufferedImage image = decodeFirstFrame(bytes);
        return image == null ? null : recognizeImage(image);
    }

    private byte[] download(HttpUrl url) throws IOException {
        for (int redirect = 0; redirect <= 3 && url != null; redirect++) {
            if (!url.username().isEmpty() || !url.password().isEmpty()) {
                return null;
            }
            Request request = new Request.Builder().url(url).header("Accept", "image/*").build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isRedirect()) {
                    String location = response.header("Location");
                    url = location == null ? null : url.resolve(location);
                    continue;
                }
                if (!response.isSuccessful()) {
                    int status = response.code();
                    if (status == 408 || status == 429 || status >= 500) {
                        throw new IOException("Image server temporarily unavailable");
                    }
                    return null;
                }
                ResponseBody body = response.body();
                if (body == null || body.contentLength() > MAX_BYTES) {
                    return null;
                }
                BufferedSource source = body.source();
                return source.request(MAX_BYTES + 1L) ? null : source.readByteArray();
            }
        }
        return null;
    }

    private static List<InetAddress> lookupPublicAddresses(String hostname) throws UnknownHostException {
        List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname).stream()
                .filter(EmoticonOcr::isPublicAddress)
                .collect(Collectors.toList());
        if (addresses.isEmpty()) {
            throw new UnknownHostException("No public IP address");
        }
        return addresses;
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] ip = address.getAddress();
        int first = ip[0] & 0xff;
        int second = ip[1] & 0xff;
        int third = ip[2] & 0xff;
        if (ip.length == 16) {
            // Only global unicast. Exclude special-use prefixes and IPv4 transition addresses.
            return (first & 0xe0) == 0x20
                    && !(first == 0x20 && second == 1 && (third <= 1
                        || (third == 0x0d && (ip[3] & 0xff) == 0xb8)))
                    && !(first == 0x20 && second == 2)
                    && !(first == 0x3f && second == 0xff);
        }
        return first != 0 && first < 224
                && !(first == 100 && second >= 64 && second <= 127)
                && !(first == 192 && ((second == 0 && (third == 0 || third == 2))
                        || (second == 88 && third == 99)))
                && !(first == 198 && (second == 18 || second == 19 || (second == 51 && third == 100)))
                && !(first == 203 && second == 0 && third == 113);
    }

    static BufferedImage decodeFirstFrame(byte[] bytes) throws IOException {
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SIDE || height > MAX_SIDE
                        || (long) width * height > MAX_PIXELS) {
                    return null;
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    synchronized String recognizeImage(BufferedImage image) throws IOException {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, Color.WHITE, null);
        } finally {
            graphics.dispose();
        }
        Path input = Files.createTempFile("emoticon-ocr-", ".png");
        try {
            ImageIO.write(rgb, "png", input.toFile());
            InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4, new HardwareConfig(1, -1));
            ParamConfig options = ParamConfig.getDefaultConfig();
            options.setMaxSideLen(1280);
            String text = StringUtils.normalizeSpace(engine.runOcr(input.toString(), options).getStrRes());
            int length = Math.min(text.codePointCount(0, text.length()), MAX_TEXT);
            return text.substring(0, text.offsetByCodePoints(0, length));
        } finally {
            Files.deleteIfExists(input);
        }
    }
}
