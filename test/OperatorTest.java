import operators.OperationResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static operators.Operators.*;

public class OperatorTest {
    @Test
    public void test1() {
        OperationResult a = new OperationResult(1).apply(ADD, 2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5);  // 1 + 2 + 3 * 4 + 5
        OperationResult b = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4))).apply(ADD, 5);  // 1 + (2 + 3 * 4) + 5
        OperationResult c = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5));  // 1 + (2 + 3 * 4 + 5)

        assertTrue(a.isEquivalent(b));
        assertTrue(a.isEquivalent(c));
        assertTrue(b.isEquivalent(c));
    }

    @Test
    public void test2() {
        OperationResult nine = new OperationResult(9);
        OperationResult three = new OperationResult(3);
        OperationResult four = new OperationResult(4);
        OperationResult seven = new OperationResult(7);

        OperationResult a = nine.apply(ADD, three.apply(MUL, four).apply(SUB, seven));
        OperationResult b = nine.apply(ADD, three.apply(MUL, four)).apply(SUB, seven);

        assertTrue(a.isEquivalent(b));
        assertTrue(b.isEquivalent(a));
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

        assertTrue(a.isEquivalent(b));
    }

    @Test
    public void dev() {
        // 7 / ((9 - 8) / 2)
        OperationResult or = new OperationResult(7).apply(DIV, new OperationResult(9).apply(SUB, 8).apply(DIV, 2));
        System.out.println(or);
        System.out.println(or.getNormalized());
    }
}
