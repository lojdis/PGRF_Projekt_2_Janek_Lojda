package graphics.clipper;

import model.Point;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class SutherlandHodgmanClipper {

    public Polygon clip(Polygon subjectPolygon, Polygon clipPolygon, int resultColor) {
        Polygon result = new Polygon();
        if (subjectPolygon == null || clipPolygon == null
                || subjectPolygon.size() < 3 || clipPolygon.size() < 3) {
            return result;
        }

        List<Point> clipPoints = clipPolygon.getPoints();
        double signedArea = getSignedArea(clipPoints);
        if (Math.abs(signedArea) < 1e-9) {
            return result;
        }
        double orientation = Math.signum(signedArea);

        List<Point> outputList = new ArrayList<>(subjectPolygon.getPoints());

        for (int i = 0; i < clipPoints.size(); i++) {
            Point clipEdgeStart = clipPoints.get(i);
            Point clipEdgeEnd = clipPoints.get((i + 1) % clipPoints.size());

            List<Point> inputList = outputList;
            outputList = new ArrayList<>();

            if (inputList.isEmpty()) {
                break;
            }

            Point s = inputList.get(inputList.size() - 1);

            for (Point e : inputList) {
                boolean sInside = isInside(s, clipEdgeStart, clipEdgeEnd, orientation);
                boolean eInside = isInside(e, clipEdgeStart, clipEdgeEnd, orientation);

                if (sInside && eInside) {
                    addUniquePoint(outputList, e);
                } else if (sInside) {
                    addUniquePoint(outputList, computeIntersection(s, e, clipEdgeStart, clipEdgeEnd));
                } else if (eInside) {
                    addUniquePoint(outputList, computeIntersection(s, e, clipEdgeStart, clipEdgeEnd));
                    addUniquePoint(outputList, e);
                }

                s = e;
            }
        }

        if (outputList.size() >= 3) {
            for (Point p : outputList) {
                result.addPoint(p);
            }
            result.setColor(resultColor);
            result.setFinished(true);
        }

        return result;
    }

    public double getSignedArea(List<Point> points) {
        double area = 0.0;
        for (int i = 0; i < points.size(); i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % points.size());
            area += (double) p1.getX() * p2.getY() - (double) p2.getX() * p1.getY();
        }
        return area / 2.0;
    }

    private boolean isInside(Point p, Point edgeStart, Point edgeEnd, double orientation) {
        double cross = (double) (edgeEnd.getX() - edgeStart.getX()) * (p.getY() - edgeStart.getY())
                - (double) (edgeEnd.getY() - edgeStart.getY()) * (p.getX() - edgeStart.getX());
        return cross * orientation >= 0;
    }

    private Point computeIntersection(Point s, Point e, Point clipStart, Point clipEnd) {
        double x1 = clipStart.getX();
        double y1 = clipStart.getY();
        double x2 = clipEnd.getX();
        double y2 = clipEnd.getY();

        double x3 = s.getX();
        double y3 = s.getY();
        double x4 = e.getX();
        double y4 = e.getY();

        double denom = (x4 - x3) * (y2 - y1) - (y4 - y3) * (x2 - x1);
        if (Math.abs(denom) < 1e-9) {
            return s;
        }

        double u = ((x1 - x3) * (y2 - y1) - (y1 - y3) * (x2 - x1)) / denom;
        int ix = (int) Math.round(x3 + u * (x4 - x3));
        int iy = (int) Math.round(y3 + u * (y4 - y3));
        return new Point(ix, iy);
    }

    private void addUniquePoint(List<Point> list, Point p) {
        if (list.isEmpty()) {
            list.add(p);
            return;
        }
        Point last = list.get(list.size() - 1);
        if (last.getX() != p.getX() || last.getY() != p.getY()) {
            list.add(p);
        }
    }
}