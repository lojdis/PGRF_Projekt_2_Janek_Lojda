package graphics.filler;

public class GridPattern implements PatternFill {

    private final int lineColor;
    private final int backgroundColor;
    private final int spacing;

    public GridPattern(int lineColor, int backgroundColor, int spacing) {
        this.lineColor = lineColor;
        this.backgroundColor = backgroundColor;
        this.spacing = Math.max(2, spacing);
    }

    @Override
    public int paint(int x, int y) {
        boolean onGrid = Math.floorMod(x, spacing) == 0 || Math.floorMod(y, spacing) == 0;
        return onGrid ? lineColor : backgroundColor;
    }
}