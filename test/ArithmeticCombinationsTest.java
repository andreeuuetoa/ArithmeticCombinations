import operators.OperationResult;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static operators.Operators.*;
import static org.junit.jupiter.api.Assertions.*;

class ArithmeticCombinationsTest {
    private boolean containsEquivalentPattern(Collection<OperationResult> expressions, OperationResult pattern) {
        return expressions.stream().anyMatch(or -> or.isEquivalent(pattern));
    }

    private boolean containsExactlyOneEquivalentPattern(Collection<OperationResult> expressions, OperationResult pattern) {
        return expressions.stream().filter(or -> or.isEquivalent(pattern)).count() == 1;
    }

    private void containsEquivalentPatterns(Collection<OperationResult> expressions, OperationResult... patterns) {
        for (OperationResult pattern : patterns) {
            try {
                assertTrue(containsEquivalentPattern(expressions, pattern));
            } catch (AssertionFailedError ignored) {
                throw new AssertionFailedError(
                        null,
                        String.format("Expression equivalent to '%s'", pattern),
                        null
                );
            }
        }
    }

    @Test
    public void testParentheses() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult four = new OperationResult(4);
        OperationResult seven = new OperationResult(7);
        OperationResult nine = new OperationResult(9);
        OperationResult eight = new OperationResult(8);
        OperationResult fortyFour = new OperationResult(44);

        List<Double> numbers = Stream.of(2, 3, 4, 5, 7, 8, 9, 44, 55).map(Integer::doubleValue).toList();

        Collection<OperationResult> result = ArithmeticCombinations.operationCombinations(
                DEFAULT_OPERATORS,
                numbers,
                4,
                14,
                true,
                false
        );

        containsEquivalentPatterns(
                result,
                two.apply(ADD, three).apply(ADD, four).apply(ADD, five),  // 2 + 3 + 4 + 5
                eight.apply(MUL, seven).apply(SUB, fortyFour.apply(SUB, two)),  // 8 * 7 - (44 - 2)
                two.apply(SUB, fortyFour).apply(ADD, seven.apply(MUL, eight)),  // 2 - 44 + 7 * 8
                seven.apply(SUB, two.apply(SUB, four)).apply(ADD, five),  // 7 - (2 - 4) + 5
                seven.apply(DIV, nine.apply(SUB, eight).apply(DIV, two)),  // 7 / ((9 - 8) / 2)
                fortyFour.apply(SUB, two.apply(MUL, seven.apply(ADD, eight))),  // 44 - 2 * (7 + 8)
                five.apply(DIV, three.apply(SUB, two)).apply(ADD, nine)  // 5 / (3 - 2) + 9
        );

        assertEquals(231, result.size());
    }

    @Test
    public void testNoParentheses() {
        List<Double> numbers = Stream.of(2, 3, 4, 5, 7, 8, 9, 44, 55).map(Integer::doubleValue).toList();

        Collection<OperationResult> result = ArithmeticCombinations.operationCombinations(
                DEFAULT_OPERATORS,
                numbers,
                4,
                14,
                false,
                false
        );

        assertEquals(71, result.size());
    }
}