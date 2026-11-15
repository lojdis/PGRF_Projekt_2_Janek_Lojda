package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Polygon {

    private final List<Point> points = new ArrayList<>();
    private boolean isFinished = false;
    private int color = Color.WHITE.getRGB();

    public Polygon() {
    }

    public Polygon(List<Line> initialLines) {
        if (initialLines != null && !initialLines.isEmpty()) {
            points.add(initialLines.get(0).getPoint1());
            for (Line line : initialLines) {
                points.add(line.getPoint2());
                this.color = line.getColor();
            }
            this.isFinished = true;
        }
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public void insertPoint(int index, Point point) {
        if (index >= 0 && index <= points.size()) {
            points.add(index, point);
        }
    }

    public void updatePoint(int index, Point newPoint) {
        if (index >= 0 && index < points.size()) {
            points.set(index, newPoint);
        }
    }

    public void removePoint(int index) {
        if (index >= 0 && index < points.size()) {
            points.remove(index);
            if (points.size() < 3) {
                isFinished = false;
            }
        }
    }

    public void addLine(Line line) {
        if (points.isEmpty()) {
            points.add(line.getPoint1());
        }
        points.add(line.getPoint2());
        this.color = line.getColor();
    }

    public List<Point> getPoints() {
        return points;
    }

    public Point getLastPoint() {
        if (points.isEmpty()) {
            return null;
        }
        return points.get(points.size() - 1);
    }

    public Point getFirstPoint() {
        if (points.isEmpty()) {
            return null;
        }
        return points.get(0);
    }

    public int size() {
        return points.size();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        this.isFinished = finished;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void clear() {
        points.clear();
        isFinished = false;
    }

    public List<Line> getLines() {
        List<Line> result = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            result.add(new Line(color, points.get(i), points.get(i + 1)));
        }
        return result;
    }
}
