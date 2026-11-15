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
    private PatternFill patternFill;

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

    public void setPatternFill(PatternFill patternFill) {
        this.patternFill = patternFill;
    }

    @Override
    public void fill() {
        if (seed == null) {
            return;
        }

        int width = raster.getWidth();
        int height = raster.getHeight();
        int startX = seed.getX();
        int startY = seed.getY();

        Integer startColor = raster.getPixel(startX, startY);
        if (startColor == null) {
            return;
        }

        if (useBoundaryColor) {
            if (startColor == boundaryColor) {
                return;
            }
        } else if (patternFill == null && startColor == fillColor) {
            return;
        }

        boolean[][] visited = new boolean[width][height];
        Queue<Point> queue = new ArrayDeque<>();

        visited[startX][startY] = true;
        raster.setPixel(startX, startY, resolveColor(startX, startY));
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

                if (nx < 0 || nx >= width || ny < 0 || ny >= height || visited[nx][ny]) {
                    continue;
                }

                Integer pixelColor = raster.getPixel(nx, ny);
                if (pixelColor == null) {
                    continue;
                }

                boolean shouldFill = useBoundaryColor
                        ? pixelColor != boundaryColor
                        : pixelColor.equals(startColor);

                if (shouldFill) {
                    visited[nx][ny] = true;
                    raster.setPixel(nx, ny, resolveColor(nx, ny));
                    queue.add(new Point(nx, ny));
                }
            }
        }
    }

    private int resolveColor(int x, int y) {
        return patternFill != null ? patternFill.paint(x, y) : fillColor;
    }
}