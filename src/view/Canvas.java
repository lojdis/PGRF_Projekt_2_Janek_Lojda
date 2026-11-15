package view;

import graphics.Raster;
import graphics.RasterImage;

import javax.swing.*;
import java.awt.*;

public class Canvas extends JPanel {

    private final RasterImage raster;
    private static final Color BACKGROUND_COLOR = new Color(0x2F2F2F);
    private String statusText = "";

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- Constructors -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= */

    public Canvas(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        raster = new RasterImage(width, height);
        clear();
    }

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- Rendering -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- */

    public void clear() {
        raster.clear(BACKGROUND_COLOR.getRGB());
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        raster.present(graphics);

        if (statusText != null && !statusText.isEmpty()) {
            graphics.setColor(new Color(220, 220, 220));
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String[] lines = statusText.split("\n");
            int y = 18;
            for (String line : lines) {
                graphics.drawString(line, 10, y);
                y += 16;
            }
        }
    }

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= Getters -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= */

    public Raster<Integer> getRaster() {
        return raster;
    }
}