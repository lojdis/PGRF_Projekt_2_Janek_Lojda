package graphics.rasterizer;

import model.Line;
import model.Polygon;

import java.util.List;

public class PolygonOutlineRasterizer implements PolygonRasterizer {

    private final LineRasterizer lineRasterizer;

    public PolygonOutlineRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    @Override
    public void rasterize(Polygon polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return;
        }

        if (polygon.size() == 1) {
            lineRasterizer.rasterize(new Line(polygon.getColor(), polygon.getFirstPoint(), polygon.getFirstPoint()));
            return;
        }

        List<Line> lines = polygon.getLines();
        rasterizeLines(lines);

        if (polygon.isFinished() && polygon.size() >= 3 && !lines.isEmpty()) {
            Line last = lines.get(lines.size() - 1);
            Line first = lines.get(0);
            lineRasterizer.rasterize(new Line(polygon.getColor(), last.getPoint2(), first.getPoint1()));
        }
    }

    private void rasterizeLines(List<Line> lines) {
        for (Line line : lines) {
            lineRasterizer.rasterize(line);
        }
    }
}