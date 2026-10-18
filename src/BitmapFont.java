import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;
import javax.imageio.ImageIO;

/**
 * Loads a GameMaker-style bitmap font from a PNG sprite sheet and a .yy glyph data file.
 * Each glyph's position, size, and advance width are read from the .yy JSON metadata.
 */
public class BitmapFont {

    private BufferedImage sheet;
    private Map<Character, Glyph> glyphs = new HashMap<>();

    private static class Glyph {
        int x, y, w, h, shift, offset;
    }

    /**
     * @param pngPath path to the font sprite sheet PNG
     * @param yyPath  path to the GameMaker .yy file with glyph metadata
     */
    public BitmapFont(String pngPath, String yyPath) {
        try {
            sheet = ImageIO.read(new File(pngPath));
        } catch (Exception e) {
            System.err.println("Failed to load font PNG: " + pngPath);
        }

        try {
            parseYY(new File(yyPath));
        } catch (Exception e) {
            System.err.println("Failed to load font data: " + yyPath);
        }
    }

    /** Parse the GameMaker .yy (JSON) file to extract per-glyph data. */
    private void parseYY(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()));

        // Match each glyph block: "charCode": { ... }
        Pattern blockP = Pattern.compile("\"(\\d+)\":\\s*\\{([^}]+)\\}");
        Pattern fieldP = Pattern.compile("\"(\\w+)\":\\s*(\\d+)");

        Matcher blockM = blockP.matcher(content);
        while (blockM.find()) {
            int charCode = Integer.parseInt(blockM.group(1));
            String block = blockM.group(2);

            Glyph g = new Glyph();
            Matcher fieldM = fieldP.matcher(block);
            while (fieldM.find()) {
                String key = fieldM.group(1);
                int val = Integer.parseInt(fieldM.group(2));
                switch (key) {
                    case "x":      g.x = val; break;
                    case "y":      g.y = val; break;
                    case "w":      g.w = val; break;
                    case "h":      g.h = val; break;
                    case "shift":  g.shift = val; break;
                    case "offset": g.offset = val; break;
                }
            }
            glyphs.put((char) charCode, g);
        }
    }

    /**
     * Draw a string using the bitmap font sprite sheet.
     *
     * @param g     the graphics context
     * @param text  the string to draw
     * @param x     leftmost pixel x
     * @param y     top pixel y
     * @param scale  integer scale factor (2 = 2x, 3 = 3x, etc.)
     */
    public void drawString(Graphics2D g, String text, int x, int y, double scale) {
        if (sheet == null) return;

        double cursorX = x;
        for (char c : text.toCharArray()) {
            Glyph glyph = glyphs.get(c);
            if (glyph != null) {
                int dx = (int) (cursorX + glyph.offset * scale);
                int dw = (int) (glyph.w * scale);
                int dh = (int) (glyph.h * scale);
                g.drawImage(sheet,
                    dx, y, dx + dw, y + dh,
                    glyph.x, glyph.y, glyph.x + glyph.w, glyph.y + glyph.h,
                    null);
                cursorX += glyph.shift * scale;
            }
        }
    }

    /** Width of a string at the given scale, based on per-glyph advance (shift) values. */
    public int stringWidth(String text, double scale) {
        double width = 0;
        for (char c : text.toCharArray()) {
            Glyph glyph = glyphs.get(c);
            if (glyph != null) {
                width += glyph.shift * scale;
            }
        }
        return (int) width;
    }

    /** Tallest glyph height at the given scale — useful for line spacing. */
    public int getHeight(double scale) {
        int max = 0;
        for (Glyph g : glyphs.values()) {
            max = Math.max(max, g.h);
        }
        return (int) (max * scale);
    }

    public boolean isLoaded() {
        return sheet != null && !glyphs.isEmpty();
    }
}
