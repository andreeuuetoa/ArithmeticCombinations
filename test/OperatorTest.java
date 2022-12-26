import operators.OperationResult;
import operators.Utils;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import static operators.Operators.*;

class EquivalentAssertionFailedError extends AssertionFailedError {
    public EquivalentAssertionFailedError(OperationResult a, OperationResult b) {
        super(
                String.format("Expected '%s' (normalized: '%s') to be equivalent to '%s' (normalized: '%s'), but wasn't!%n",
                        a, a.getNormalized(),
                        b, b.getNormalized()
                )
        );
    }
}

public class OperatorTest {
    private void assertEquivalent(OperationResult a, OperationResult b) {
        if (!a.isEquivalent(b)) throw new EquivalentAssertionFailedError(a, b);
    }

    private void assertNormalizedFormContainsOriginalNumbers(OperationResult original) {
        HashMap<Double, Integer> originalOriginals = original.usedOriginalsWithCounts();
        HashMap<Double, Integer> normalizedOriginals = original.getNormalized().usedOriginalsWithCounts();
        for (Double key : originalOriginals.keySet()) {
            int expectedOccurrences = originalOriginals.get(key);
            int actualOccurrences = normalizedOriginals.getOrDefault(key, 0);
            try {
                if (actualOccurrences < expectedOccurrences) {
                    throw new AssertionFailedError(
                            String.format(
                                    "Normalized form '%s' of '%s' contained fewer original numbers than original expression.",
                                    original.getNormalized(),
                                    original
                            ),
                            String.format("At least %d occurrences of %s", expectedOccurrences, key),
                            String.format("%d occurrences of %s", actualOccurrences, key)
                    );
                }
            } catch (AssertionFailedError e) {
                if (!Utils.doubleEquals(key, 1) && !Utils.doubleEquals(key, -1)) throw e;
                System.out.printf("Insufficient occurrences (%s < %s) of key %s in normalized form '%s' of '%s'%n",
                        actualOccurrences, expectedOccurrences, key, original.getNormalized(), original);
                System.out.printf("Ignoring because reduction of the occurrences of %s can be allowed in normalization%n", key);
            }
        }
    }

    private void assertNormalizationDoesNotChangeOriginal(OperationResult original) {
        String preString = original.toString();
        original.getNormalized();
        String postString = original.toString();
        assertEquals(preString, postString);
    }

    private void assertNormalizationIsReflexive(OperationResult original) {
        assertEquals(original.getNormalized().toString(), original.getNormalized().getNormalized().toString());
    }

    private void assertNormalizationWorksBasic(OperationResult... originals) {
        for (OperationResult original : originals) {
            assertNormalizationDoesNotChangeOriginal(original);
            assertNormalizationIsReflexive(original);
            assertNormalizedFormContainsOriginalNumbers(original);
            System.out.printf("Original: %s%n", original);
            System.out.printf("Normalized: %s%n", original.getNormalized());
            System.out.println();
        }
    }

    private void assertEquivalent(OperationResult... operationResults) {
        for (OperationResult left : operationResults) {
            for (OperationResult right : operationResults) {
                assertEquivalent(left, right);
            }
        }
    }

    private void assertEquivalenceAndNormalization(OperationResult... operationResults) {
        for (OperationResult operationResult : operationResults)
            assertNormalizationWorksBasic(operationResult);
        assertEquivalent(operationResults);
    }

    @Test
    public void test1() {
        OperationResult a = new OperationResult(1).apply(ADD, 2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5);  // 1 + 2 + 3 * 4 + 5
        OperationResult b = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4))).apply(ADD, 5);  // 1 + (2 + 3 * 4) + 5
        OperationResult c = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5));  // 1 + (2 + 3 * 4 + 5)

        assertEquivalenceAndNormalization(a, b, c);
    }

    @Test
    public void test2() {
        OperationResult nine = new OperationResult(9);
        OperationResult three = new OperationResult(3);
        OperationResult four = new OperationResult(4);
        OperationResult seven = new OperationResult(7);

        OperationResult a = nine.apply(ADD, three.apply(MUL, four).apply(SUB, seven));  // 9 + (3 * 4 - 7)
        OperationResult b = nine.apply(ADD, three.apply(MUL, four)).apply(SUB, seven);  // 9 + (3 * 4) - 7

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void test3() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult seven = new OperationResult(7);
        OperationResult eight = new OperationResult(8);
        OperationResult nine = new OperationResult(9);

        OperationResult a = two.apply(SUB, three.apply(MUL, nine).apply(SUB, eight).apply(ADD, seven)).apply(SUB, 2);  // 2 - (3 * 9 - 8 + 7) - 2;
        OperationResult b = eight.apply(SUB, three.apply(MUL, nine)).apply(SUB, seven.apply(ADD, two)).apply(ADD, two);  // 8 - 3 * 9 - (7 + 2) + 2;

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void multiplicationTestDistributeLeft() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);

        // (2 - 3) * 5
        OperationResult a = two.apply(SUB, three).apply(MUL, five);
        // 2 * 5 - 5 * 3
        OperationResult b = two.apply(MUL, five).apply(SUB, five.apply(MUL, three));

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void multiplicationTestDistributeRight() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);

        // 2 * (3 - 5)
        OperationResult a = two.apply(MUL, three.apply(SUB, five));
        // 2 * 3 - 2 * 5
        OperationResult b = two.apply(MUL, three).apply(SUB, two.apply(MUL, five));

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void multiplicationTestDistribute2() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);

        // (2 + 3) * (5 - 7)
        OperationResult a = two.apply(ADD, three).apply(MUL, five.apply(SUB, seven));
        // 2 * 5 - 7 * 3
        // + 5 * 3 - 7 * 2
        OperationResult b = two.apply(MUL, five).apply(SUB, seven.apply(MUL, three)).
                apply(ADD, five.apply(MUL, three)).apply(SUB, seven.apply(MUL, two));

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void multiplicationTestDistribute3() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);
        OperationResult eleven = new OperationResult(11);
        OperationResult thirteen = new OperationResult(13);

        // (2 - 3 + 5) * (7 + 11 - 13)
        OperationResult a = two.apply(SUB, three).apply(ADD, five).apply(MUL,
                seven.apply(ADD, eleven).apply(SUB, thirteen));
        // 2 * 7 - 3 * 7 + 5 * 7
        // + 2 * 11 - 3 * 11 + 5 * 11
        // - 2 * 13 + 3 * 13 - 5 * 13
        OperationResult b = two.apply(MUL, seven).apply(SUB, three.apply(MUL, seven)).apply(ADD, five.apply(MUL, seven))
                .apply(ADD, two.apply(MUL, eleven)).apply(SUB, three.apply(MUL, eleven)).apply(ADD, five.apply(MUL, eleven))
                .apply(SUB, two.apply(MUL, thirteen)).apply(ADD, three.apply(MUL, thirteen)).apply(SUB, five.apply(MUL, thirteen));

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void divisionTest1() {
        OperationResult two = new OperationResult(2);
        OperationResult seven = new OperationResult(7);
        OperationResult eight = new OperationResult(8);
        OperationResult nine = new OperationResult(9);

        OperationResult a = seven.apply(DIV, nine.apply(SUB, eight).apply(DIV, two));  // 7 / ((9 - 8) / 2)
        OperationResult b = two.apply(DIV, nine.apply(SUB, eight).apply(DIV, seven));  // 2 / ((9 - 8) / 7)

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void divisionTestInvertSimple() {
        OperationResult one = new OperationResult(1);
        OperationResult two = new OperationResult(2);
        OperationResult seven = new OperationResult(7);

        OperationResult a = two.apply(DIV, seven).getNormalized();

        assertEquals(two, a.right);
        assertEquals(MUL, a.operator);
        assertNotNull(a.left);
        assert a.left.left != null;
        assertEquals(one, a.left.left);
        assertEquals(DIV, a.left.operator);
        assertEquals(seven, a.left.right);
    }

    @Test
    public void divisionTestDoubleInvertSimple() {
        OperationResult one = new OperationResult(1);
        OperationResult seven = new OperationResult(7);

        OperationResult a = one.apply(DIV, one.apply(DIV, seven));

        assertEquivalenceAndNormalization(a, seven);
    }

    @Test
    public void divisionTestNegative() {
        OperationResult two = new OperationResult(2);
        OperationResult five = new OperationResult(5);
        OperationResult eleven = new OperationResult(11);
        OperationResult twentyThree = new OperationResult(23);

        OperationResult a = two.apply(SUB, five).apply(DIV, eleven.apply(SUB, twentyThree));  // (2 - 5) / (11 - 23)
        OperationResult b = five.apply(SUB, two).apply(DIV, twentyThree.apply(SUB, eleven));  // (5 - 2) / (23 - 11)

        assertEquivalenceAndNormalization(a, b);
    }

    @Test public void divisionTestNegative2() {
        OperationResult two = new OperationResult(2);
        OperationResult five = new OperationResult(5);
        OperationResult eight = new OperationResult(8);
        OperationResult fortyFour = new OperationResult(44);

        // (2 - 44) / (5 - 8)
        OperationResult a = two.apply(SUB, fortyFour).apply(DIV, five.apply(SUB, eight));
        // 44 * (1 / (5 + -1 * 8)) * -1 + 2 * (1 / (5 + -1 * 8))

        // (44 - 2) / (8 - 5)
        OperationResult b = fortyFour.apply(SUB, two).apply(DIV, eight.apply(SUB, five));
        // -1 * (1 / (8 + -1 * 5)) * 2 + 44 * (1 / (8 + -1 * 5))

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void divisionTestDistribute() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);

        OperationResult a = two.apply(ADD, three).apply(SUB, 5).apply(DIV, seven);  // (2 + 3 - 5) / 7
        OperationResult b = two.apply(DIV, seven).apply(ADD, three.apply(DIV, seven)).apply(SUB, five.apply(DIV, seven));  // 2 / 7 + 3 / 7 - 5 / 7

        assertEquivalenceAndNormalization(a, b);
    }

    // TODO: Test for false positives

    @Test
    public void divisionBasic() {
        OperationResult a = new OperationResult(5).apply(DIV, 3);  // 5 / 3
        OperationResult b = new OperationResult(5).apply(MUL, new OperationResult(1).apply(DIV, 3));  // 5 * (1 / 3)

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void divisionMore() {
        OperationResult two = new OperationResult(2);
        OperationResult four = new OperationResult(4);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);

        OperationResult a = seven.apply(DIV, five.apply(SUB, four).apply(DIV, two));  // 7 / ((5 - 4) / 2)
        OperationResult b = two.apply(MUL, seven.apply(DIV, five.apply(SUB, four)));  // 2 * (7 / (5 - 4))
        OperationResult c = two.apply(DIV, five.apply(SUB, four).apply(DIV, seven));  // 2 / ((5 - 4) / 7)

        assertEquivalenceAndNormalization(a, b, c);
    }
}
