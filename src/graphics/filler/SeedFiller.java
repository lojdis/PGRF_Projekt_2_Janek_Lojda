package graphics.filler;

import graphics.Raster;
import model.Point;

import java.util.ArrayDeque;
import java.util.Queue;

public class SeedFiller implements PolygonFiller {

    private final Raster<Integer> raster;
    private Point seed;
    private int fillColor;
    private int boundaryColor;
    private boolean useBoundaryColor;

    public SeedFiller(Raster<Integer> raster) {
        this.raster = raster;
    }

    public void setSeed(Point seed) {
        this.seed = seed;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public void setBoundaryColor(int boundaryColor) {
        this.boundaryColor = boundaryColor;
    }

    public void setUseBoundaryColor(boolean useBoundaryColor) {
        this.useBoundaryColor = useBoundaryColor;
    }

    @Override
    public void fill() {
        if (seed == null) {
            return;
        }

        int startX = seed.getX();
        int startY = seed.getY();
        Integer startColor = raster.getPixel(startX, startY);

        if (startColor == null) {
            return;
        }

        if (useBoundaryColor) {
            if (startColor == boundaryColor || startColor == fillColor) {
                return;
            }
        } else {
            if (startColor == fillColor) {
                return;
            }
        }

        Queue<Point> queue = new ArrayDeque<>();
        raster.setPixel(startX, startY, fillColor);
        queue.add(new Point(startX, startY));

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int cx = current.getX();
            int cy = current.getY();

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];

                Integer pixelColor = raster.getPixel(nx, ny);
                if (pixelColor == null) {
                    continue;
                }

                if (useBoundaryColor) {
                    if (pixelColor != boundaryColor && pixelColor != fillColor) {
                        raster.setPixel(nx, ny, fillColor);
                        queue.add(new Point(nx, ny));
                    }
                } else {
                    if (pixelColor.equals(startColor)) {
                        raster.setPixel(nx, ny, fillColor);
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }
    }
}