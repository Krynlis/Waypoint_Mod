package de.kryn.waypoints.head;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Minimaler PNG-Decoder für Skins (8 Bit pro Kanal, ohne Interlacing). Liefert ARGB-Pixel. */
final class PngDecoder {
    final int width;
    final int height;
    final int[] argb;

    private PngDecoder(int width, int height, int[] argb) {
        this.width = width;
        this.height = height;
        this.argb = argb;
    }

    static PngDecoder decode(byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(data);
        if (data.length < 8 || buf.getLong() != 0x89504E470D0A1A0AL) throw new IOException("Kein PNG");
        int width = 0, height = 0, bitDepth = 0, colorType = 0, interlace = 0;
        int[] palette = null;
        byte[] paletteAlpha = null;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        while (buf.remaining() >= 12) {
            int length = buf.getInt();
            String type = new String(new byte[]{buf.get(), buf.get(), buf.get(), buf.get()}, java.nio.charset.StandardCharsets.US_ASCII);
            byte[] chunk = new byte[length];
            buf.get(chunk);
            buf.getInt(); // CRC
            switch (type) {
                case "IHDR" -> {
                    ByteBuffer h = ByteBuffer.wrap(chunk);
                    width = h.getInt();
                    height = h.getInt();
                    bitDepth = h.get() & 0xFF;
                    colorType = h.get() & 0xFF;
                    h.get();
                    h.get();
                    interlace = h.get() & 0xFF;
                }
                case "PLTE" -> {
                    palette = new int[length / 3];
                    for (int i = 0; i < palette.length; i++) {
                        palette[i] = ((chunk[i * 3] & 0xFF) << 16) | ((chunk[i * 3 + 1] & 0xFF) << 8) | (chunk[i * 3 + 2] & 0xFF);
                    }
                }
                case "tRNS" -> paletteAlpha = chunk;
                case "IDAT" -> idat.write(chunk);
                default -> {
                }
            }
            if (type.equals("IEND")) break;
        }
        if (bitDepth != 8 || interlace != 0 || width <= 0 || height <= 0 || width > 1024 || height > 1024) {
            throw new IOException("Nicht unterstütztes PNG");
        }
        int channels = switch (colorType) {
            case 0, 3 -> 1;
            case 2 -> 3;
            case 4 -> 2;
            case 6 -> 4;
            default -> throw new IOException("Unbekannter Farbtyp " + colorType);
        };
        int stride = width * channels;
        byte[] raw = new byte[(stride + 1) * height];
        Inflater inflater = new Inflater();
        inflater.setInput(idat.toByteArray());
        try {
            int read = 0;
            while (read < raw.length && !inflater.finished()) {
                int n = inflater.inflate(raw, read, raw.length - read);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break;
                read += n;
            }
        } catch (DataFormatException e) {
            throw new IOException(e);
        } finally {
            inflater.end();
        }

        byte[] prev = new byte[stride];
        byte[] cur = new byte[stride];
        int[] argb = new int[width * height];
        for (int y = 0; y < height; y++) {
            int filter = raw[y * (stride + 1)] & 0xFF;
            System.arraycopy(raw, y * (stride + 1) + 1, cur, 0, stride);
            for (int i = 0; i < stride; i++) {
                int a = i >= channels ? cur[i - channels] & 0xFF : 0;
                int b = prev[i] & 0xFF;
                int c = i >= channels ? prev[i - channels] & 0xFF : 0;
                int x = cur[i] & 0xFF;
                x += switch (filter) {
                    case 1 -> a;
                    case 2 -> b;
                    case 3 -> (a + b) / 2;
                    case 4 -> paeth(a, b, c);
                    default -> 0;
                };
                cur[i] = (byte) x;
            }
            for (int px = 0; px < width; px++) {
                int o = px * channels;
                int r, g, bl, al = 255;
                switch (colorType) {
                    case 0 -> r = g = bl = cur[o] & 0xFF;
                    case 4 -> {
                        r = g = bl = cur[o] & 0xFF;
                        al = cur[o + 1] & 0xFF;
                    }
                    case 2 -> {
                        r = cur[o] & 0xFF;
                        g = cur[o + 1] & 0xFF;
                        bl = cur[o + 2] & 0xFF;
                    }
                    case 3 -> {
                        int idx = cur[o] & 0xFF;
                        int rgb = palette != null && idx < palette.length ? palette[idx] : 0;
                        r = (rgb >> 16) & 0xFF;
                        g = (rgb >> 8) & 0xFF;
                        bl = rgb & 0xFF;
                        if (paletteAlpha != null && idx < paletteAlpha.length) al = paletteAlpha[idx] & 0xFF;
                    }
                    default -> {
                        r = cur[o] & 0xFF;
                        g = cur[o + 1] & 0xFF;
                        bl = cur[o + 2] & 0xFF;
                        al = cur[o + 3] & 0xFF;
                    }
                }
                argb[y * width + px] = (al << 24) | (r << 16) | (g << 8) | bl;
            }
            byte[] t = prev;
            prev = cur;
            cur = t;
        }
        return new PngDecoder(width, height, argb);
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) return a;
        return pb <= pc ? b : c;
    }
}
