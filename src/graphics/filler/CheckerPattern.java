package graphics.filler;

public class CheckerPattern implements PatternFill {

    private final int primaryColor;
    private final int secondaryColor;
    private final int cellSize;

    public CheckerPattern(int primaryColor, int secondaryColor, int cellSize) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.cellSize = Math.max(1, cellSize);
    }

    @Override
    public int paint(int x, int y) {
        int cx = Math.floorDiv(x, cellSize);
        int cy = Math.floorDiv(y, cellSize);
        return ((cx + cy) & 1) == 0 ? primaryColor : secondaryColor;
    }
}