package graphics.filler;

public class StripePattern implements PatternFill {

    private final int primaryColor;
    private final int secondaryColor;
    private final int stripeWidth;

    public StripePattern(int primaryColor, int secondaryColor, int stripeWidth) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.stripeWidth = Math.max(1, stripeWidth);
    }

    @Override
    public int paint(int x, int y) {
        int band = Math.floorDiv(x + y, stripeWidth);
        return (band & 1) == 0 ? primaryColor : secondaryColor;
    }
}