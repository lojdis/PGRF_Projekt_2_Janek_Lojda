package graphics.clipper;

import model.Line;
import model.Point;

import java.util.ArrayList;
import java.util.List;

public class CohenSutherlandClipper {

    private static final int INSIDE = 0; // 0000
    private static final int LEFT = 1;   // 0001
    private static final int RIGHT = 2;  // 0010
    private static final int BOTTOM = 4; // 0100
    private static final int TOP = 8;    // 1000

    private int xMin;
    private int yMin;
    private int xMax;
    private int yMax;

    public CohenSutherlandClipper(int x1, int y1, int x2, int y2) {
        setBounds(x1, y1, x2, y2);
    }

    public void setBounds(int x1, int y1, int x2, int y2) {
        this.xMin = Math.min(x1, x2);
        this.yMin = Math.min(y1, y2);
        this.xMax = Math.max(x1, x2);
        this.yMax = Math.max(y1, y2);
    }

    public int getXMin() {
        return xMin;
    }

    public int getYMin() {
        return yMin;
    }

    public int getXMax() {
        return xMax;
    }

    public int getYMax() {
        return yMax;
    }

    public List<Point> getCorners() {
        List<Point> corners = new ArrayList<>(4);
        corners.add(new Point(xMin, yMin)); // 0: levý horní
        corners.add(new Point(xMax, yMin)); // 1: pravý horní
        corners.add(new Point(xMax, yMax)); // 2: pravý dolní
        corners.add(new Point(xMin, yMax)); // 3: levý dolní
        return corners;
    }

    private int computeOutCode(double x, double y) {
        int code = INSIDE;

        if (x < xMin) {
            code |= LEFT;
        } else if (x > xMax) {
            code |= RIGHT;
        }

        if (y < yMin) {
            code |= TOP;
        } else if (y > yMax) {
            code |= BOTTOM;
        }

        return code;
    }

    public Line clip(Line line, int clippedColor) {
        if (line == null) {
            return null;
        }

        double x1 = line.getPoint1().getX();
        double y1 = line.getPoint1().getY();
        double x2 = line.getPoint2().getX();
        double y2 = line.getPoint2().getY();

        int outcode1 = computeOutCode(x1, y1);
        int outcode2 = computeOutCode(x2, y2);

        while (true) {
            if ((outcode1 | outcode2) == 0) {
                return new Line(
                        clippedColor,
                        new Point((int) Math.round(x1), (int) Math.round(y1)),
                        new Point((int) Math.round(x2), (int) Math.round(y2))
                );
            } else if ((outcode1 & outcode2) != 0) {
                return null;
            } else {
                int outcodeOut = (outcode1 != 0) ? outcode1 : outcode2;
                double x = 0;
                double y = 0;

                if ((outcodeOut & TOP) != 0) {
                    x = x1 + (x2 - x1) * (yMin - y1) / (y2 - y1);
                    y = yMin;
                } else if ((outcodeOut & BOTTOM) != 0) {
                    x = x1 + (x2 - x1) * (yMax - y1) / (y2 - y1);
                    y = yMax;
                } else if ((outcodeOut & RIGHT) != 0) {
                    y = y1 + (y2 - y1) * (xMax - x1) / (x2 - x1);
                    x = xMax;
                } else if ((outcodeOut & LEFT) != 0) {
                    y = y1 + (y2 - y1) * (xMin - x1) / (x2 - x1);
                    x = xMin;
                }

                if (outcodeOut == outcode1) {
                    x1 = x;
                    y1 = y;
                    outcode1 = computeOutCode(x1, y1);
                } else {
                    x2 = x;
                    y2 = y;
                    outcode2 = computeOutCode(x2, y2);
                }
            }
        }
    }
}