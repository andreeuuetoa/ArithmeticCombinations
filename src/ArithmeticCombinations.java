import operators.OperationResult;
import operators.Operator;
import operators.Utils;

import java.util.*;
import java.util.stream.Stream;

import static operators.Operators.*;


public class ArithmeticCombinations {
    public static void main(String[] args) {
        List<Double> numbers = Stream.of(2, 3, 4, 5, 7, 8, 9, 44, 55).map(Integer::doubleValue).toList();

        Collection<OperationResult> result = operationCombinations(
                DEFAULT_OPERATORS,
                numbers,
                4,
                14,
                false,
                false
        );

        System.out.printf("Combinations found: %d%n%n", result.size());

        for (OperationResult operationResult : result) {
            System.out.println(operationResult);
        }

        System.out.printf("%nCombinations found: %d%n", result.size());
    }

    private static boolean parenthesesCheck(OperationResult operationResult, boolean parenthesesAllowed) {
        return parenthesesAllowed || !operationResult.containsParentheses();
    }

    public static Collection<OperationResult> operationCombinations(List<Operator> operators, List<Double> numbers, int numsAmount, double target, boolean parenthesesAllowed, boolean reUseAllowed) {
        HashSet<OperationResult> potentialArguments = new HashSet<>(numbers.stream().map(OperationResult::new).toList());

        for (int length = 1; length < numsAmount; length++) {
            int lengthForLambda = length;
            for (OperationResult base : potentialArguments.stream().filter(or -> or.length() == lengthForLambda).toList()) {
                for (OperationResult other : potentialArguments.stream().filter(or -> or.length() <= numsAmount - lengthForLambda && (reUseAllowed || Collections.disjoint(base.usedOriginals(), or.usedOriginals()))).toList()) {
                    for (Operator operator : operators) {
                        for (OperationResult newResult : List.of(base.apply(operator, other), other.apply(operator, base))) {
                            if (newResult.length() == numsAmount && !Utils.doubleEquals(newResult.resultValue, target))
                                continue;
                            if (potentialArguments.stream().noneMatch(newResult::isEquivalent) && parenthesesCheck(newResult, parenthesesAllowed))
                                potentialArguments.add(newResult);
                        }
                    }
                }
            }
        }

        return potentialArguments.stream().filter(or -> or.length() == numsAmount && Utils.doubleEquals(or.resultValue, target)).toList();
    }
}