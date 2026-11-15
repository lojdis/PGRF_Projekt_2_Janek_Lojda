package controller;

import graphics.rasterizer.LineRasterizer;
import graphics.rasterizer.PolygonOutlineRasterizer;
import graphics.rasterizer.PolygonRasterizer;
import graphics.rasterizer.TrivialLineRasterizer;
import model.Line;
import model.Point;
import model.Polygon;
import view.Canvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    private enum Mode {
        POLYGON, LINE
    }

    private Mode currentMode = Mode.POLYGON;

    private Line previewLine;
    private Line closingPreviewLine;

    private final Canvas canvas;
    private final Polygon polygon;
    private final LineRasterizer rasterizer;
    private final PolygonRasterizer polygonRasterizer;

    private final List<Line> independentLines = new ArrayList<>();
    private final List<Line> initialDemoLines = new ArrayList<>();

    private int activeColor = Color.WHITE.getRGB();
    private static final int PREVIEW_COLOR = Color.RED.getRGB();
    private static final int CLOSING_PREVIEW_COLOR = new Color(255, 165, 0).getRGB();
    private static final int VERTEX_COLOR = new Color(0, 220, 255).getRGB();
    private static final double EDIT_RADIUS = 20.0;

    private int draggedPolygonVertexIndex = -1;
    private Line draggedLine = null;
    private boolean draggingLineFirstPoint = false;
    private Point lastMousePos = new Point(0, 0);
    private boolean shiftPressed = false;

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- Constructors -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= */

    public Controller(Canvas canvas) {
        this.canvas = canvas;
        this.polygon = new Polygon();
        this.rasterizer = new TrivialLineRasterizer(canvas.getRaster());
        this.polygonRasterizer = new PolygonOutlineRasterizer(rasterizer);

        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(100, 100), new Point(300, 100)));
        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(300, 100), new Point(350, 250)));
        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(350, 250), new Point(200, 350)));
        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(200, 350), new Point(100, 250)));
    }

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= Main functions -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- */

    public void init() {
        for (Line l : initialDemoLines) {
            polygon.addLine(l);
        }
        polygon.setFinished(false);

        render();

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = getPoint(e);
                shiftPressed = e.isShiftDown();

                if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isControlDown())) {
                    insertVertexOnClosestEdge(getPoint(e));
                    return;
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    startVertexEdit(getPoint(e));
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    startLine(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastMousePos = getPoint(e);
                shiftPressed = e.isShiftDown();

                if (draggedPolygonVertexIndex != -1 || draggedLine != null) {
                    draggedPolygonVertexIndex = -1;
                    draggedLine = null;
                    render();
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e) && previewLine != null) {
                    Point rawEnd = getPoint(e);
                    Point endPoint = shiftPressed ? snapPoint(previewLine.getPoint1(), rawEnd) : rawEnd;
                    Line newLine = new Line(activeColor, previewLine.getPoint1(), endPoint);

                    if (currentMode == Mode.POLYGON) {
                        polygon.addLine(newLine);
                    } else {
                        independentLines.add(newLine);
                    }

                    previewLine = null;
                    closingPreviewLine = null;
                    render();
                }
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                lastMousePos = getPoint(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPos = getPoint(e);
                lastMousePos = currentPos;
                shiftPressed = e.isShiftDown();

                if (draggedPolygonVertexIndex != -1) {
                    polygon.updatePoint(draggedPolygonVertexIndex, currentPos);
                    render();
                    return;
                }
                if (draggedLine != null) {
                    if (draggingLineFirstPoint) {
                        draggedLine.setPoint1(currentPos);
                    } else {
                        draggedLine.setPoint2(currentPos);
                    }
                    render();
                    return;
                }

                if (previewLine != null) {
                    updatePreview(currentPos);
                }
            }
        });

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SHIFT -> {
                        if (!shiftPressed) {
                            shiftPressed = true;
                            updatePreview(lastMousePos);
                        }
                    }
                    case KeyEvent.VK_C -> {
                        polygon.clear();
                        independentLines.clear();
                        previewLine = null;
                        closingPreviewLine = null;
                        draggedPolygonVertexIndex = -1;
                        draggedLine = null;
                        render();
                    }
                    case KeyEvent.VK_ENTER, KeyEvent.VK_F -> finishPolygon();
                    case KeyEvent.VK_P -> {
                        currentMode = Mode.POLYGON;
                        previewLine = null;
                        closingPreviewLine = null;
                        render();
                    }
                    case KeyEvent.VK_L -> {
                        currentMode = Mode.LINE;
                        previewLine = null;
                        closingPreviewLine = null;
                        render();
                    }
                    case KeyEvent.VK_D, KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> deleteClosestVertex(lastMousePos);
                    case KeyEvent.VK_E -> rotatePolygon(-15);
                    case KeyEvent.VK_R -> rotatePolygon(15);
                    case KeyEvent.VK_1 -> {
                        activeColor = Color.WHITE.getRGB();
                        polygon.setColor(activeColor);
                        render();
                    }
                    case KeyEvent.VK_2 -> {
                        activeColor = Color.GREEN.getRGB();
                        polygon.setColor(activeColor);
                        render();
                    }
                    case KeyEvent.VK_3 -> {
                        activeColor = Color.CYAN.getRGB();
                        polygon.setColor(activeColor);
                        render();
                    }
                    case KeyEvent.VK_4 -> {
                        activeColor = Color.YELLOW.getRGB();
                        polygon.setColor(activeColor);
                        render();
                    }
                    case KeyEvent.VK_5 -> {
                        activeColor = Color.MAGENTA.getRGB();
                        polygon.setColor(activeColor);
                        render();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftPressed = false;
                    updatePreview(lastMousePos);
                }
            }
        });
    }

    public void render() {
        canvas.clear();

        for (Line line : independentLines) {
            rasterizer.rasterize(line);
            drawVertexMarker(line.getPoint1());
            drawVertexMarker(line.getPoint2());
        }

        polygonRasterizer.rasterize(polygon);
        for (Point p : polygon.getPoints()) {
            drawVertexMarker(p);
        }

        if (previewLine != null) {
            rasterizer.rasterize(previewLine);
        }
        if (closingPreviewLine != null && currentMode == Mode.POLYGON && polygon.size() >= 2) {
            rasterizer.rasterize(closingPreviewLine);
        }

        String polyState = polygon.isFinished() ? "UZAVŘENÝ" : "OTEVŘENÝ (" + polygon.size() + " vrcholů)";
        canvas.setStatusText(
                "Režim: " + (currentMode == Mode.POLYGON ? "POLYGON [P]" : "SAMOSTATNÉ ÚSEČKY [L]") +
                        " | Stav polygonu: " + polyState + " | Uzavřít polygon: [Enter / F] | Smazat vše: [C]\n" +
                        "Ovládání: [Levé tl.] Kreslit (+[Shift] zarovnat) | [Pravé tl.] Přesunout | [Střední tl.] Přidat | [D] Smazat | [E/R] Rotace | [1-5] Barva"
        );

        canvas.repaint();
    }

    private void startLine(MouseEvent e) {
        Point mousePoint = getPoint(e);

        if (currentMode == Mode.POLYGON) {
            if (polygon.isFinished()) {
                return;
            }
            Point startPoint = polygon.isEmpty() ? mousePoint : polygon.getLastPoint();
            previewLine = new Line(PREVIEW_COLOR, startPoint, mousePoint);
            if (!polygon.isEmpty()) {
                closingPreviewLine = new Line(CLOSING_PREVIEW_COLOR, mousePoint, polygon.getFirstPoint());
            }
        } else {
            previewLine = new Line(PREVIEW_COLOR, mousePoint, mousePoint);
            closingPreviewLine = null;
        }
        render();
    }

    private void updatePreview(Point rawMousePos) {
        if (previewLine == null) {
            return;
        }
        Point startPoint = previewLine.getPoint1();
        Point targetPoint = shiftPressed ? snapPoint(startPoint, rawMousePos) : rawMousePos;
        previewLine = new Line(PREVIEW_COLOR, startPoint, targetPoint);

        if (currentMode == Mode.POLYGON && !polygon.isEmpty()) {
            closingPreviewLine = new Line(CLOSING_PREVIEW_COLOR, targetPoint, polygon.getFirstPoint());
        }
        render();
    }

    private Point snapPoint(Point start, Point end) {
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();

        if (dx == 0 && dy == 0) {
            return end;
        }

        double angle = Math.abs(Math.toDegrees(Math.atan2(dy, dx)));

        if (angle <= 22.5 || angle >= 157.5) {
            return new Point(end.getX(), start.getY());
        }

        if (angle >= 67.5 && angle <= 112.5) {
            return new Point(start.getX(), end.getY());
        }

        int dist = (int) Math.round((Math.abs(dx) + Math.abs(dy)) / 2.0);
        int snappedX = start.getX() + Integer.signum(dx) * dist;
        int snappedY = start.getY() + Integer.signum(dy) * dist;
        return new Point(snappedX, snappedY);
    }

    private void finishPolygon() {
        if (polygon.size() >= 3) {
            polygon.setFinished(true);
        }
        previewLine = null;
        closingPreviewLine = null;
        render();
    }

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= Bonus & Extension Methods -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= */

    private void startVertexEdit(Point clickPoint) {
        double minDistance = EDIT_RADIUS;
        draggedPolygonVertexIndex = -1;
        draggedLine = null;

        List<Point> polyPoints = polygon.getPoints();
        for (int i = 0; i < polyPoints.size(); i++) {
            double dist = clickPoint.distanceTo(polyPoints.get(i));
            if (dist < minDistance) {
                minDistance = dist;
                draggedPolygonVertexIndex = i;
            }
        }

        for (Line line : independentLines) {
            double d1 = clickPoint.distanceTo(line.getPoint1());
            if (d1 < minDistance) {
                minDistance = d1;
                draggedPolygonVertexIndex = -1;
                draggedLine = line;
                draggingLineFirstPoint = true;
            }
            double d2 = clickPoint.distanceTo(line.getPoint2());
            if (d2 < minDistance) {
                minDistance = d2;
                draggedPolygonVertexIndex = -1;
                draggedLine = line;
                draggingLineFirstPoint = false;
            }
        }
    }

    private void insertVertexOnClosestEdge(Point clickPoint) {
        List<Point> pts = polygon.getPoints();
        if (pts.size() < 2) {
            return;
        }

        double minDistance = EDIT_RADIUS * 2;
        int bestInsertIndex = -1;

        int edgeCount = polygon.isFinished() ? pts.size() : pts.size() - 1;
        for (int i = 0; i < edgeCount; i++) {
            Point p1 = pts.get(i);
            Point p2 = pts.get((i + 1) % pts.size());
            Line edge = new Line(activeColor, p1, p2);
            double dist = edge.distanceToPoint(clickPoint);
            if (dist < minDistance) {
                minDistance = dist;
                bestInsertIndex = i + 1;
            }
        }

        if (bestInsertIndex != -1) {
            polygon.insertPoint(bestInsertIndex, clickPoint);
            draggedPolygonVertexIndex = bestInsertIndex;
            render();
        }
    }

    private void deleteClosestVertex(Point mousePoint) {
        double minDistance = EDIT_RADIUS * 1.5;
        int polyIndexToDelete = -1;
        Line lineToDelete = null;

        List<Point> polyPoints = polygon.getPoints();
        for (int i = 0; i < polyPoints.size(); i++) {
            double dist = mousePoint.distanceTo(polyPoints.get(i));
            if (dist < minDistance) {
                minDistance = dist;
                polyIndexToDelete = i;
            }
        }

        for (Line line : independentLines) {
            double d1 = mousePoint.distanceTo(line.getPoint1());
            double d2 = mousePoint.distanceTo(line.getPoint2());
            if (d1 < minDistance || d2 < minDistance) {
                minDistance = Math.min(d1, d2);
                polyIndexToDelete = -1;
                lineToDelete = line;
            }
        }

        if (polyIndexToDelete != -1) {
            polygon.removePoint(polyIndexToDelete);
            render();
        } else if (lineToDelete != null) {
            independentLines.remove(lineToDelete);
            render();
        }
    }

    private void rotatePolygon(double angleDegrees) {
        List<Point> pts = polygon.getPoints();
        if (pts.isEmpty()) return;

        double cx = 0, cy = 0;
        for (Point p : pts) {
            cx += p.getX();
            cy += p.getY();
        }
        cx /= pts.size();
        cy /= pts.size();
        double radians = Math.toRadians(angleDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        for (int i = 0; i < pts.size(); i++) {
            Point p = pts.get(i);
            double dx = p.getX() - cx;
            double dy = p.getY() - cy;

            int newX = (int) Math.round(cx + dx * cos - dy * sin);
            int newY = (int) Math.round(cy + dx * sin + dy * cos);

            polygon.updatePoint(i, new Point(newX, newY));
        }
        render();
    }

    private void drawVertexMarker(Point p) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (Math.abs(dx) == 2 || Math.abs(dy) == 2) {
                    canvas.getRaster().setPixel(p.getX() + dx, p.getY() + dy, VERTEX_COLOR);
                }
            }
        }
    }

    private Point getPoint(MouseEvent e) {
        return new Point(e.getX(), e.getY());
    }
}