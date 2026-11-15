package graphics.rasterizer;

import graphics.Raster;

public class TrivialLineRasterizer extends LineRasterizer {

    public TrivialLineRasterizer(Raster<Integer> raster) {
        super(raster);
    }

    @Override
    protected void rasterize(int x1, int x2, int y1, int y2, int color) {
        if (x1 == x2 && y1 == y2) {
            raster.setPixel(x1, y1, color);
            return;
        }

        if (x1 == x2) {
            if (y2 < y1) {
                int tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            for (int y = y1; y <= y2; y++) {
                raster.setPixel(x1, y, color);
            }
            return;
        }

        if (y1 == y2) {
            if (x2 < x1) {
                int tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            for (int x = x1; x <= x2; x++) {
                raster.setPixel(x, y1, color);
            }
            return;
        }

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        float k = (y2 - y1) / (float) (x2 - x1);
        float q = y1 - k * x1;

        if (dx >= dy) {
            if (x2 < x1) {
                int tmpX = x1;
                x1 = x2;
                x2 = tmpX;
            }
            for (int x = x1; x <= x2; x++) {
                float y = k * x + q;
                raster.setPixel(x, Math.round(y), color);
            }
        } else {
            if (y2 < y1) {
                int tmpY = y1;
                y1 = y2;
                y2 = tmpY;
            }
            for (int y = y1; y <= y2; y++) {
                float x = (y - q) / k;
                raster.setPixel(Math.round(x), y, color);
            }
        }
    }
}
