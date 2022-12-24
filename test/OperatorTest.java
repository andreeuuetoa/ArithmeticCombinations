import operators.OperationResult;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

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
        if (!b.isEquivalent(a)) throw new EquivalentAssertionFailedError(b, a);
    }

    @Test
    public void test1() {
        OperationResult a = new OperationResult(1).apply(ADD, 2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5);  // 1 + 2 + 3 * 4 + 5
        OperationResult b = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4))).apply(ADD, 5);  // 1 + (2 + 3 * 4) + 5
        OperationResult c = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5));  // 1 + (2 + 3 * 4 + 5)

        assertEquivalent(a, b);
        assertEquivalent(a, c);
        assertEquivalent(b, c);
    }

    @Test
    public void test2() {
        OperationResult nine = new OperationResult(9);
        OperationResult three = new OperationResult(3);
        OperationResult four = new OperationResult(4);
        OperationResult seven = new OperationResult(7);

        OperationResult a = nine.apply(ADD, three.apply(MUL, four).apply(SUB, seven));
        OperationResult b = nine.apply(ADD, three.apply(MUL, four)).apply(SUB, seven);

        assertEquivalent(a, b);
        assertEquivalent(b, a);
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

        assertEquivalent(a, b);
    }

    @Test
    public void divisionTest1() {
        OperationResult two = new OperationResult(2);
        OperationResult seven = new OperationResult(7);
        OperationResult eight = new OperationResult(8);
        OperationResult nine = new OperationResult(9);

        OperationResult a = seven.apply(DIV, nine.apply(SUB, eight).apply(DIV, two));  // 7 / ((9 - 8) / 2)
        OperationResult b = two.apply(DIV, nine.apply(SUB, eight).apply(DIV, seven));  // 2 / ((9 - 8) / 7)

        assertEquivalent(a, b);
    }

    @Test
    public void divisionTestNegative() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);

        OperationResult a = two.apply(SUB, three).apply(DIV, five.apply(SUB, seven));  // (2 - 3) / (5 - 7)
        OperationResult b = three.apply(SUB, two).apply(DIV, seven.apply(SUB, five));  // (3 - 2) / (7 - 5)

        assertEquivalent(a, b);
    }

    @Test
    public void divisionTestDistribute() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);

        OperationResult a = two.apply(ADD, three).apply(SUB, 5).apply(DIV, seven);  // (2 + 3 - 5) / 7
        OperationResult b = two.apply(DIV, seven).apply(ADD, three.apply(DIV, seven)).apply(SUB, five.apply(DIV, seven));  // 2 / 7 + 3 / 7 - 5 / 7

        assertEquivalent(a, b);
    }

    // TODO: Test for false positives

    @Test
    public void dev() {
        // 7 / ((9 - 8) / 2)
        OperationResult or = new OperationResult(7).apply(DIV, new OperationResult(9).apply(SUB, 8).apply(DIV, 2));
        System.out.println(or);
        System.out.println(or.getNormalized());
    }
}
