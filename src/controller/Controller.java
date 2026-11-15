package controller;

import graphics.clipper.CohenSutherlandClipper;
import graphics.clipper.SutherlandHodgmanClipper;
import graphics.filler.*;
import graphics.rasterizer.LineRasterizer;
import graphics.rasterizer.PolygonOutlineRasterizer;
import graphics.rasterizer.PolygonRasterizer;
import graphics.rasterizer.TrivialLineRasterizer;
import model.Line;
import model.Point;
import model.Polygon;
import view.Canvas;
import view.Window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Controller {

    private enum Mode {
        POLYGON, LINE, SEED_FILL
    }

    private enum FillPatternMode {
        SOLID("Plná barva"),
        CHECKER("Šachovnice"),
        STRIPES("Pruhy"),
        GRID("Mřížka");

        private final String label;

        FillPatternMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private Mode currentMode = Mode.POLYGON;
    private FillPatternMode patternMode = FillPatternMode.SOLID;

    private Line previewLine;
    private Line closingPreviewLine;

    private final Canvas canvas;
    private final Polygon polygon;
    private final Polygon clipPolygon;
    private final LineRasterizer rasterizer;
    private final PolygonRasterizer polygonRasterizer;
    private final SeedFiller seedFiller;
    private final ScanlineFiller scanlineFiller;
    private final CohenSutherlandClipper lineClipper;
    private final SutherlandHodgmanClipper polygonClipper;

    private final List<Line> independentLines = new ArrayList<>();
    private final List<Line> freehandLines = new ArrayList<>();
    private final List<Line> initialDemoLines = new ArrayList<>();

    private static final int[] BOUNDARY_PALETTE = {
            Color.WHITE.getRGB(),
            Color.GREEN.getRGB(),
            Color.CYAN.getRGB(),
            Color.YELLOW.getRGB(),
            Color.MAGENTA.getRGB()
    };
    private int boundaryPaletteIndex = 0;
    private int activeColor = BOUNDARY_PALETTE[0];

    private static final int[] FILL_PALETTE = {
            new Color(255, 140, 0).getRGB(),
            new Color(0, 200, 120).getRGB(),
            new Color(80, 170, 255).getRGB(),
            new Color(220, 80, 220).getRGB(),
            new Color(255, 215, 0).getRGB()
    };
    private int fillPaletteIndex = 0;
    private int fillColor = FILL_PALETTE[0];

    private Point seedPoint = null;
    private boolean useBoundaryColor = false;
    private boolean scanlineEnabled = false;
    private boolean lineClippingEnabled = false;
    private boolean polygonClippingEnabled = false;
    private Point lastFreehandPoint = null;
    private boolean freehandDragged = false;

    private JButton boundaryColorBtn;
    private JButton fillColorBtn;
    private JButton patternBtn;

    private static final int PREVIEW_COLOR = Color.RED.getRGB();
    private static final int CLOSING_PREVIEW_COLOR = new Color(255, 165, 0).getRGB();
    private static final int VERTEX_COLOR = new Color(0, 220, 255).getRGB();
    private static final int CLIP_WINDOW_COLOR = new Color(255, 215, 0).getRGB();
    private static final int CLIPPED_LINE_COLOR = new Color(0, 230, 118).getRGB();
    private static final int CLIPPED_FILL_COLOR = new Color(0, 130, 70).getRGB();
    private static final int DIMMED_LINE_COLOR = new Color(105, 105, 105).getRGB();
    private static final double EDIT_RADIUS = 20.0;

    private int draggedPolygonVertexIndex = -1;
    private int draggedClipPolyVertexIndex = -1;
    private Line draggedLine = null;
    private boolean draggingLineFirstPoint = false;
    private int draggedClipCornerIndex = -1;
    private Point clipAnchorPoint = null;
    private Point clipDragStartPoint = null;
    private Point lastMousePos = new Point(0, 0);
    private boolean shiftPressed = false;

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- Constructors -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= */

    public Controller(Canvas canvas) {
        this.canvas = canvas;
        this.polygon = new Polygon();
        this.clipPolygon = new Polygon();
        this.rasterizer = new TrivialLineRasterizer(canvas.getRaster());
        this.polygonRasterizer = new PolygonOutlineRasterizer(rasterizer);
        this.seedFiller = new SeedFiller(canvas.getRaster());
        this.scanlineFiller = new ScanlineFiller(canvas.getRaster(), polygonRasterizer);
        this.lineClipper = new CohenSutherlandClipper(180, 110, 620, 460);
        this.polygonClipper = new SutherlandHodgmanClipper();

        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(100, 100), new Point(300, 100)));
        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(300, 100), new Point(350, 250)));
        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(350, 250), new Point(200, 350)));
        initialDemoLines.add(new Line(Color.WHITE.getRGB(), new Point(200, 350), new Point(100, 250)));

        resetClipPolygon();
    }

    /* -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= Main functions -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- */

    public void init() {
        setupToolbar();

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
                    if (lineClippingEnabled && e.isAltDown()) {
                        clipDragStartPoint = getPoint(e);
                        lineClipper.setBounds(clipDragStartPoint.getX(), clipDragStartPoint.getY(),
                                clipDragStartPoint.getX(), clipDragStartPoint.getY());
                        render();
                        return;
                    }
                    if (currentMode == Mode.SEED_FILL) {
                        lastFreehandPoint = getPoint(e);
                        freehandDragged = false;
                        return;
                    }
                    startLine(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastMousePos = getPoint(e);
                shiftPressed = e.isShiftDown();

                if (draggedPolygonVertexIndex != -1 || draggedClipPolyVertexIndex != -1
                        || draggedLine != null || draggedClipCornerIndex != -1) {
                    draggedPolygonVertexIndex = -1;
                    draggedClipPolyVertexIndex = -1;
                    draggedLine = null;
                    draggedClipCornerIndex = -1;
                    clipAnchorPoint = null;
                    render();
                    return;
                }

                if (clipDragStartPoint != null) {
                    clipDragStartPoint = null;
                    render();
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e) && currentMode == Mode.SEED_FILL) {
                    if (!freehandDragged) {
                        seedPoint = getPoint(e);
                        render();
                    }
                    lastFreehandPoint = null;
                    freehandDragged = false;
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

                if (draggedClipCornerIndex != -1 && clipAnchorPoint != null) {
                    lineClipper.setBounds(
                            clipAnchorPoint.getX(),
                            clipAnchorPoint.getY(),
                            currentPos.getX(),
                            currentPos.getY()
                    );
                    render();
                    return;
                }

                if (clipDragStartPoint != null) {
                    lineClipper.setBounds(
                            clipDragStartPoint.getX(),
                            clipDragStartPoint.getY(),
                            currentPos.getX(),
                            currentPos.getY()
                    );
                    render();
                    return;
                }

                if (draggedClipPolyVertexIndex != -1) {
                    clipPolygon.updatePoint(draggedClipPolyVertexIndex, currentPos);
                    render();
                    return;
                }

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

                if (currentMode == Mode.SEED_FILL && lastFreehandPoint != null) {
                    freehandDragged = true;
                    freehandLines.add(new Line(activeColor, lastFreehandPoint, currentPos));
                    lastFreehandPoint = currentPos;
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
                    case KeyEvent.VK_C -> resetAll();
                    case KeyEvent.VK_ENTER, KeyEvent.VK_F -> finishPolygon();
                    case KeyEvent.VK_G -> toggleScanline();
                    case KeyEvent.VK_K -> toggleLineClipping();
                    case KeyEvent.VK_H -> togglePolygonClipping();
                    case KeyEvent.VK_O -> {
                        Collections.reverse(clipPolygon.getPoints());
                        render();
                    }
                    case KeyEvent.VK_P -> switchMode(Mode.POLYGON);
                    case KeyEvent.VK_L -> switchMode(Mode.LINE);
                    case KeyEvent.VK_S -> switchMode(Mode.SEED_FILL);
                    case KeyEvent.VK_B -> {
                        useBoundaryColor = !useBoundaryColor;
                        render();
                    }
                    case KeyEvent.VK_V -> {
                        cycleFillColor();
                        render();
                    }
                    case KeyEvent.VK_M -> {
                        cyclePatternMode();
                        render();
                    }
                    case KeyEvent.VK_D, KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> deleteClosestVertex(lastMousePos);
                    case KeyEvent.VK_E -> rotatePolygon(-15);
                    case KeyEvent.VK_R -> rotatePolygon(15);
                    case KeyEvent.VK_1 -> setBoundaryColor(BOUNDARY_PALETTE[0]);
                    case KeyEvent.VK_2 -> setBoundaryColor(BOUNDARY_PALETTE[1]);
                    case KeyEvent.VK_3 -> setBoundaryColor(BOUNDARY_PALETTE[2]);
                    case KeyEvent.VK_4 -> setBoundaryColor(BOUNDARY_PALETTE[3]);
                    case KeyEvent.VK_5 -> setBoundaryColor(BOUNDARY_PALETTE[4]);
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

    private void setupToolbar() {
        java.awt.Window ancestor = SwingUtilities.getWindowAncestor(canvas);
        if (!(ancestor instanceof Window appWindow)) {
            return;
        }
        JPanel toolbar = appWindow.getToolbarPanel();

        toolbar.add(createToolbarButton("Polygon [P]", () -> switchMode(Mode.POLYGON)));
        toolbar.add(createToolbarButton("Úsečka [L]", () -> switchMode(Mode.LINE)));
        toolbar.add(createToolbarButton("Seed-Fill [S]", () -> switchMode(Mode.SEED_FILL)));
        toolbar.add(createToolbarButton("Scan-Line [G]", this::toggleScanline));

        boundaryColorBtn = createToolbarButton("Okraj", () -> {
            boundaryPaletteIndex = (boundaryPaletteIndex + 1) % BOUNDARY_PALETTE.length;
            setBoundaryColor(BOUNDARY_PALETTE[boundaryPaletteIndex]);
        });
        toolbar.add(boundaryColorBtn);

        fillColorBtn = createToolbarButton("Výplň [V]", () -> {
            cycleFillColor();
            render();
        });
        toolbar.add(fillColorBtn);

        patternBtn = createToolbarButton("Vzor [M]: Plná barva", () -> {
            cyclePatternMode();
            render();
        });
        toolbar.add(patternBtn);

        toolbar.add(createToolbarButton("Ořez úsečky [K]", this::toggleLineClipping));
        toolbar.add(createToolbarButton("Ořez poly [H]", this::togglePolygonClipping));
        toolbar.add(createToolbarButton("Smazat [C]", this::resetAll));

        updateToolbarIndicators();
        appWindow.pack();
    }

    private JButton createToolbarButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setMargin(new Insets(3, 7, 3, 7));
        button.setFont(new Font("SansSerif", Font.PLAIN, 11));
        button.addActionListener(e -> {
            action.run();
            canvas.requestFocusInWindow();
        });
        return button;
    }

    private void updateToolbarIndicators() {
        if (boundaryColorBtn != null) {
            boundaryColorBtn.setBackground(new Color(activeColor));
            boundaryColorBtn.setForeground(Color.BLACK);
        }
        if (fillColorBtn != null) {
            fillColorBtn.setBackground(new Color(fillColor));
            fillColorBtn.setForeground(Color.BLACK);
        }
        if (patternBtn != null) {
            patternBtn.setText("Vzor [M]: " + patternMode.getLabel());
        }
    }

    private void switchMode(Mode mode) {
        currentMode = mode;
        previewLine = null;
        closingPreviewLine = null;
        render();
    }

    private void toggleScanline() {
        if (!scanlineEnabled && polygon.size() >= 3) {
            polygon.setFinished(true);
            previewLine = null;
            closingPreviewLine = null;
        }
        scanlineEnabled = !scanlineEnabled;
        render();
    }

    private void toggleLineClipping() {
        lineClippingEnabled = !lineClippingEnabled;
        if (lineClippingEnabled && independentLines.isEmpty()) {
            independentLines.add(new Line(activeColor, new Point(100, 160), new Point(700, 410)));
            independentLines.add(new Line(activeColor, new Point(240, 200), new Point(520, 360)));
            independentLines.add(new Line(activeColor, new Point(80, 480), new Point(720, 520)));
        }
        render();
    }

    private void togglePolygonClipping() {
        polygonClippingEnabled = !polygonClippingEnabled;
        if (polygonClippingEnabled && polygon.size() >= 3) {
            polygon.setFinished(true);
            previewLine = null;
            closingPreviewLine = null;
        }
        render();
    }

    private void resetAll() {
        polygon.clear();
        independentLines.clear();
        freehandLines.clear();
        seedPoint = null;
        scanlineEnabled = false;
        lineClippingEnabled = false;
        polygonClippingEnabled = false;
        lineClipper.setBounds(180, 110, 620, 460);
        resetClipPolygon();
        previewLine = null;
        closingPreviewLine = null;
        draggedPolygonVertexIndex = -1;
        draggedClipPolyVertexIndex = -1;
        draggedLine = null;
        draggedClipCornerIndex = -1;
        render();
    }

    private void resetClipPolygon() {
        clipPolygon.clear();
        clipPolygon.addPoint(new Point(350, 120));
        clipPolygon.addPoint(new Point(540, 220));
        clipPolygon.addPoint(new Point(480, 430));
        clipPolygon.addPoint(new Point(220, 430));
        clipPolygon.addPoint(new Point(160, 220));
        clipPolygon.setColor(CLIP_WINDOW_COLOR);
        clipPolygon.setFinished(true);
    }

    private void setBoundaryColor(int color) {
        activeColor = color;
        polygon.setColor(activeColor);
        if (fillColor == activeColor) {
            cycleFillColor();
        }
        render();
    }

    private void cycleFillColor() {
        do {
            fillPaletteIndex = (fillPaletteIndex + 1) % FILL_PALETTE.length;
            fillColor = FILL_PALETTE[fillPaletteIndex];
        } while (fillColor == activeColor);
    }

    private void cyclePatternMode() {
        FillPatternMode[] modes = FillPatternMode.values();
        patternMode = modes[(patternMode.ordinal() + 1) % modes.length];
    }

    private PatternFill buildActivePattern() {
        int secondary = new Color(fillColor).darker().darker().getRGB();
        return switch (patternMode) {
            case SOLID -> null;
            case CHECKER -> new CheckerPattern(fillColor, secondary, 10);
            case STRIPES -> new StripePattern(fillColor, secondary, 8);
            case GRID -> new GridPattern(fillColor, secondary, 10);
        };
    }

    public void render() {
        canvas.clear();
        updateToolbarIndicators();
        PatternFill activePattern = buildActivePattern();

        for (Line line : freehandLines) {
            rasterizer.rasterize(line);
        }

        if (polygonClippingEnabled) {
            drawDimmedPolygonOutline(polygon);

            if (polygon.isFinished() && polygon.size() >= 3) {
                Polygon clippedPoly = polygonClipper.clip(polygon, clipPolygon, CLIPPED_LINE_COLOR);
                if (clippedPoly.size() >= 3) {
                    scanlineFiller.setPolygon(clippedPoly);
                    scanlineFiller.setFillColor(CLIPPED_FILL_COLOR);
                    scanlineFiller.setPatternFill(activePattern);
                    scanlineFiller.fill();
                    for (Point p : clippedPoly.getPoints()) {
                        drawVertexMarker(p, CLIPPED_LINE_COLOR);
                    }
                }
            }

            polygonRasterizer.rasterize(clipPolygon);
            for (Point p : clipPolygon.getPoints()) {
                drawVertexMarker(p, CLIP_WINDOW_COLOR);
            }
        } else if (scanlineEnabled && polygon.isFinished() && polygon.size() >= 3) {
            scanlineFiller.setPolygon(polygon);
            scanlineFiller.setFillColor(fillColor);
            scanlineFiller.setPatternFill(activePattern);
            scanlineFiller.fill();
        } else {
            polygonRasterizer.rasterize(polygon);
        }

        if (seedPoint != null) {
            seedFiller.setSeed(seedPoint);
            seedFiller.setFillColor(fillColor);
            seedFiller.setBoundaryColor(activeColor);
            seedFiller.setUseBoundaryColor(useBoundaryColor);
            seedFiller.setPatternFill(activePattern);
            seedFiller.fill();
        }

        if (lineClippingEnabled) {
            drawClipWindow();
            for (Line line : independentLines) {
                rasterizer.rasterize(new Line(DIMMED_LINE_COLOR, line.getPoint1(), line.getPoint2()));

                Line clipped = lineClipper.clip(line, CLIPPED_LINE_COLOR);
                if (clipped != null) {
                    rasterizer.rasterize(clipped);
                    drawVertexMarker(clipped.getPoint1(), CLIPPED_LINE_COLOR);
                    drawVertexMarker(clipped.getPoint2(), CLIPPED_LINE_COLOR);
                }
            }
        } else {
            for (Line line : independentLines) {
                rasterizer.rasterize(line);
            }
        }

        for (Line line : independentLines) {
            drawVertexMarker(line.getPoint1());
            drawVertexMarker(line.getPoint2());
        }
        for (Point p : polygon.getPoints()) {
            drawVertexMarker(p);
        }

        if (previewLine != null) {
            rasterizer.rasterize(previewLine);
        }
        if (closingPreviewLine != null && currentMode == Mode.POLYGON && polygon.size() >= 2) {
            rasterizer.rasterize(closingPreviewLine);
        }

        String modeLabel = switch (currentMode) {
            case POLYGON -> "POLYGON [P]";
            case LINE -> "ÚSEČKY [L]";
            case SEED_FILL -> "SEED FILL [S]";
        };
        String seedCondLabel = useBoundaryColor ? "HRANICE [B]" : "POZADÍ [B]";
        String orientLabel = polygonClipper.getSignedArea(clipPolygon.getPoints()) >= 0 ? "+" : "-";

        canvas.setStatusText(
                "Režim: " + modeLabel + " | Vzor [M]: " + patternMode.getLabel() +
                        " | Seed-Fill podmínka: " + seedCondLabel + " | Orientace ořezu [" + orientLabel + "]: [O]\n" +
                        "Ovládání: [Levé tl.] Kreslit (+[Shift]) | [Pravé] Přesun vrcholů | [Střední] Přidat vrchol | [D] Smazat vrchol | [E/R] Rotace"
        );

        canvas.repaint();
    }

    private void drawDimmedPolygonOutline(Polygon poly) {
        List<Point> pts = poly.getPoints();
        if (pts.size() < 2) {
            return;
        }
        for (int i = 0; i < pts.size() - 1; i++) {
            rasterizer.rasterize(new Line(DIMMED_LINE_COLOR, pts.get(i), pts.get(i + 1)));
        }
        if (poly.isFinished() && pts.size() >= 3) {
            rasterizer.rasterize(new Line(DIMMED_LINE_COLOR, pts.getLast(), pts.getFirst()));
        }
    }

    private void drawClipWindow() {
        List<Point> corners = lineClipper.getCorners();
        for (int i = 0; i < 4; i++) {
            Point p1 = corners.get(i);
            Point p2 = corners.get((i + 1) % 4);
            rasterizer.rasterize(new Line(CLIP_WINDOW_COLOR, p1, p2));
            drawVertexMarker(p1, CLIP_WINDOW_COLOR);
        }
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
        draggedClipPolyVertexIndex = -1;
        draggedLine = null;
        draggedClipCornerIndex = -1;
        clipAnchorPoint = null;

        if (lineClippingEnabled) {
            List<Point> corners = lineClipper.getCorners();
            for (int i = 0; i < corners.size(); i++) {
                double dist = clickPoint.distanceTo(corners.get(i));
                if (dist < minDistance) {
                    minDistance = dist;
                    draggedClipCornerIndex = i;
                    clipAnchorPoint = corners.get((i + 2) % 4);
                }
            }
        }

        if (polygonClippingEnabled) {
            List<Point> clipPts = clipPolygon.getPoints();
            for (int i = 0; i < clipPts.size(); i++) {
                double dist = clickPoint.distanceTo(clipPts.get(i));
                if (dist < minDistance) {
                    minDistance = dist;
                    draggedClipCornerIndex = -1;
                    clipAnchorPoint = null;
                    draggedClipPolyVertexIndex = i;
                }
            }
        }

        List<Point> polyPoints = polygon.getPoints();
        for (int i = 0; i < polyPoints.size(); i++) {
            double dist = clickPoint.distanceTo(polyPoints.get(i));
            if (dist < minDistance) {
                minDistance = dist;
                draggedClipCornerIndex = -1;
                clipAnchorPoint = null;
                draggedClipPolyVertexIndex = -1;
                draggedPolygonVertexIndex = i;
            }
        }

        for (Line line : independentLines) {
            double d1 = clickPoint.distanceTo(line.getPoint1());
            if (d1 < minDistance) {
                minDistance = d1;
                draggedClipCornerIndex = -1;
                clipAnchorPoint = null;
                draggedClipPolyVertexIndex = -1;
                draggedPolygonVertexIndex = -1;
                draggedLine = line;
                draggingLineFirstPoint = true;
            }
            double d2 = clickPoint.distanceTo(line.getPoint2());
            if (d2 < minDistance) {
                minDistance = d2;
                draggedClipCornerIndex = -1;
                clipAnchorPoint = null;
                draggedClipPolyVertexIndex = -1;
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
        drawVertexMarker(p, VERTEX_COLOR);
    }

    private void drawVertexMarker(Point p, int color) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (Math.abs(dx) == 2 || Math.abs(dy) == 2) {
                    canvas.getRaster().setPixel(p.getX() + dx, p.getY() + dy, color);
                }
            }
        }
    }

    private Point getPoint(MouseEvent e) {
        return new Point(e.getX(), e.getY());
    }
}