import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.Stream;

class Operator implements DoubleBinaryOperator, Comparable<Operator> {
    private final DoubleBinaryOperator operator;
    public final String displayString;
    private final int priority;
    public final boolean isCommutative;

    Operator(DoubleBinaryOperator operator, String displayString, int priority, boolean isCommutative) {
        this.operator = operator;
        this.displayString = displayString;
        this.priority = priority;
        this.isCommutative = isCommutative;
    }

    @Override
    public double applyAsDouble(double left, double right) {
        return operator.applyAsDouble(left, right);
    }

    @Override
    public int compareTo(Operator o) {
        return Integer.compare(this.priority, o.priority);
    }
}

class Add extends Operator {
    Add() {
        super(Double::sum, "+", 10, true);
    }
}

class Subtract extends Operator {
    Subtract() {
        super((double left, double right) -> left - right, "-", 10, false);
    }
}

class Multiply extends Operator {
    Multiply() {
        super((double left, double right) -> left * right, "*", 20, true);
    }
}

class Divide extends Operator {
    Divide() {
        super((double left, double right) -> left / right, "/", 20, false);
    }
}

class OperationResult {
    public final OperationResult left;
    public final OperationResult right;
    public final Double resultValue;
    public final Operator operator;

    public OperationResult(double value) {
        this.left = null;
        operator = null;
        this.right = null;
        resultValue = value;
    }

    private OperationResult(OperationResult previousOperationResult, Operator operator, OperationResult right) {
        this.left = previousOperationResult;
        this.operator = operator;
        this.right = right;
        resultValue = operator.applyAsDouble(previousOperationResult.resultValue, right.resultValue);
    }

    public boolean isFirst() {
        return length() == 1;
    }

    public int length() {
        if (Objects.isNull(left) && Objects.isNull(right)) return 1;
        if (Objects.isNull(left) || Objects.isNull(right))
            throw new IllegalStateException("Both or neither of original OperationResults must be null!");
        return left.length() + right.length();
    }

    public OperationResult apply(Operator operator, OperationResult other) {
        return new OperationResult(this, operator, other);
    }

    public boolean isEquivalent(OperationResult other) {
//        if (true) return false;
        if (Objects.isNull(other)) return false;
        if (!Utils.doubleEquals(this.resultValue, other.resultValue)) return false;
        if (this.length() != other.length()) return false;
        if (this.isFirst() && other.isFirst()) return true;
        if (this.operator == other.operator && (this.left.isEquivalent(other.left) && this.right.isEquivalent(other.right)))
            return true;
        if (this.operator == other.operator && operator.isCommutative && (this.left.isEquivalent(other.right) && this.right.isEquivalent(other.left)))
            return true;
        return this.usedOriginals().equals(other.usedOriginals()) && this.usedOperations().equals(other.usedOperations());
    }

    @Override
    public String toString() {
        if (isFirst()) {
            if (Utils.doubleIsInteger(resultValue)) {
                return Integer.toString(resultValue.intValue());
            }
            return resultValue.toString();
        }
        return String.format("%s %s %s",
                wrapInParenthesesIfNeeded(left, true),
                operator.displayString,
                wrapInParenthesesIfNeeded(right, false));
    }

    private boolean shouldWrapInParentheses(OperationResult operationResult, boolean isLeft) {
//        if (true) return true;
        return !(operationResult.isFirst() ||
                operationResult.operator.compareTo(this.operator) > 0 ||
                (operationResult.operator == this.operator && operator.isCommutative) ||
                (isLeft && operationResult.operator.compareTo(this.operator) == 0));
    }

    private String wrapInParenthesesIfNeeded(OperationResult operationResult, boolean isLeft) {
        if (shouldWrapInParentheses(operationResult, isLeft))
            return Utils.wrapInParentheses(operationResult.toString());
        return operationResult.toString();
    }

    public HashSet<Double> usedOriginals() {
        HashSet<Double> result;
        if (!isFirst()) {
            result = left.usedOriginals();
            result.addAll(right.usedOriginals());
        } else {
            result = new HashSet<>();
            result.add(resultValue);
        }
        return result;
    }

    public HashMap<Operator, Integer> usedOperations() {
        HashMap<Operator, Integer> result;
        if (isFirst()) {
            result = new HashMap<>();
        } else {
            result = left.usedOperations();
            HashMap<Operator, Integer> rightOperations = right.usedOperations();
            for (Operator operator : rightOperations.keySet()) {
                result.put(operator, result.getOrDefault(operator, 0) + rightOperations.get(operator));
            }
            result.put(this.operator, result.getOrDefault(this.operator, 0) + 1);
        }
        return result;
    }

    public boolean containsParentheses() {
        return toString().contains("(") || toString().contains(")");
    }
}

class Utils {
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


public class Main {
    private static final Operator ADD = new Add();
    private static final Operator SUB = new Subtract();
    private static final Operator MUL = new Multiply();
    private static final Operator DIV = new Divide();

    public static void main(String[] args) {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult four = new OperationResult(4);
        OperationResult seven = new OperationResult(7);
        OperationResult nine = new OperationResult(9);
        OperationResult eight = new OperationResult(8);
        OperationResult fortyFour = new OperationResult(44);
        if (false) {
            OperationResult a = nine.apply(ADD, three.apply(MUL, four).apply(SUB, seven));
            OperationResult b = nine.apply(ADD, three.apply(MUL, four)).apply(SUB, seven);

            System.out.println(a);
            System.out.println(b);
            System.out.println(a.isEquivalent(b));
            System.out.println(b.isEquivalent(a));
            return;
        }

        List<Operator> operators = List.of(ADD, SUB, MUL, DIV);

        List<Double> numbers = Stream.of(2, 3, 4, 5, 7, 8, 9, 44, 55).map(Integer::doubleValue).toList();

        Collection<OperationResult> result = operationCombinations(operators, numbers, 4, 14, true, false);

        System.out.printf("Combinations found: %d%n%n", result.size());

        for (OperationResult operationResult : result) {
            System.out.println(operationResult);
        }

        System.out.printf("%nCombinations found: %d%n", result.size());

//        OperationResult pattern = two.apply(ADD, three).apply(ADD, four).apply(ADD, five);  // 2 + 3 + 4 + 5
        OperationResult pattern = eight.apply(MUL, seven).apply(SUB, fortyFour.apply(SUB, two));  // 8 * 7 - (44 - 2)
//        OperationResult pattern = two.apply(SUB, fortyFour).apply(ADD, seven.apply(MUL, eight));  // 2 - 44 + 7 * 8
//        OperationResult pattern = seven.apply(SUB, two.apply(SUB, four)).apply(ADD, five);  // 7 - (2 - 4) + 5
//        OperationResult pattern = seven.apply(DIV, nine.apply(SUB, eight).apply(DIV, two));  // 7 / ((9 - 8) / 2)
        List<OperationResult> l = result.stream().filter(or -> or.isEquivalent(pattern)).toList();
        System.out.println(l);
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