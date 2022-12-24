package operators;

public class Utils {
    private static final double doublePrecision = 0.0001;

    public static boolean doubleEquals(double d1, double d2) {
        return Math.abs(d1 - d2) < doublePrecision;
    }

    public static boolean doubleIsInteger(double d) {
        return doubleEquals(Math.ceil(d), Math.floor(d));
    }

    public static String wrapInParentheses(String string) {
        return String.format("(%s)", string);
    }
}
