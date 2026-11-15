package graphics.filler;

import graphics.Raster;
import graphics.rasterizer.PolygonRasterizer;
import model.Point;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class ScanlineFiller implements PolygonFiller {

    private static class Edge {
        private final int x1;
        private final int y1;
        private final int y2;
        private final double invSlope;

        public Edge(Point p1, Point p2) {
            if (p1.getY() < p2.getY()) {
                this.x1 = p1.getX();
                this.y1 = p1.getY();
                this.y2 = p2.getY();
                this.invSlope = (double) (p2.getX() - p1.getX()) / (p2.getY() - p1.getY());
            } else {
                this.x1 = p2.getX();
                this.y1 = p2.getY();
                this.y2 = p1.getY();
                this.invSlope = (double) (p1.getX() - p2.getX()) / (p1.getY() - p2.getY());
            }
        }

        public boolean isActive(int y) {
            return y >= y1 && y < y2;
        }

        public double getIntersectionX(int y) {
            return x1 + (y - y1) * invSlope;
        }
    }

    private final Raster<Integer> raster;
    private final PolygonRasterizer polygonRasterizer;
    private Polygon polygon;
    private int fillColor;
    private PatternFill patternFill;

    public ScanlineFiller(Raster<Integer> raster, PolygonRasterizer polygonRasterizer) {
        this.raster = raster;
        this.polygonRasterizer = polygonRasterizer;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public void setPatternFill(PatternFill patternFill) {
        this.patternFill = patternFill;
    }

    @Override
    public void fill() {
        if (polygon == null || polygon.size() < 3) {
            return;
        }

        List<Point> points = polygon.getPoints();
        List<Edge> edges = new ArrayList<>();

        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;

        for (int i = 0; i < points.size(); i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % points.size());

            if (p1.getY() < yMin) yMin = p1.getY();
            if (p1.getY() > yMax) yMax = p1.getY();

            if (p1.getY() != p2.getY()) {
                edges.add(new Edge(p1, p2));
            }
        }

        yMin = Math.max(0, yMin);
        yMax = Math.min(raster.getHeight() - 1, yMax);

        for (int y = yMin; y <= yMax; y++) {
            List<Double> intersections = new ArrayList<>();

            for (Edge edge : edges) {
                if (edge.isActive(y)) {
                    intersections.add(edge.getIntersectionX(y));
                }
            }

            sortIntersections(intersections);

            for (int i = 0; i + 1 < intersections.size(); i += 2) {
                int xStart = (int) Math.round(intersections.get(i));
                int xEnd = (int) Math.round(intersections.get(i + 1));

                for (int x = xStart; x <= xEnd; x++) {
                    int color = patternFill != null ? patternFill.paint(x, y) : fillColor;
                    raster.setPixel(x, y, color);
                }
            }
        }

        if (polygonRasterizer != null) {
            polygonRasterizer.rasterize(polygon);
        }
    }

    private void sortIntersections(List<Double> list) {
        for (int i = 1; i < list.size(); i++) {
            double key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j) > key) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }
}