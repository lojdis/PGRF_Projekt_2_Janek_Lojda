package graphics.rasterizer;

import graphics.Raster;
import model.Line;

public abstract class LineRasterizer {

    protected final Raster<Integer> raster;

    public LineRasterizer(Raster<Integer> raster) {
        this.raster = raster;
    }

    public void rasterize(Line line) {
        if (line == null) {
            return;
        }
        rasterize(
                line.getPoint1().getX(),
                line.getPoint2().getX(),
                line.getPoint1().getY(),
                line.getPoint2().getY(),
                line.getColor()
        );
    }

    protected abstract void rasterize(int x1, int x2, int y1, int y2, int color);
}