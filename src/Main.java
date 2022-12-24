import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.Stream;

class Operator implements DoubleBinaryOperator, Comparable<Operator> {
    private final DoubleBinaryOperator operator;
    public final String displayString;
    public final int priority;
    public final boolean isCommutativeAndAssociative;
    protected Operator distributiveToOperator = null;

    Operator(DoubleBinaryOperator operator, String displayString, int priority, boolean isCommutativeAndAssociative) {
        this.operator = operator;
        this.displayString = displayString;
        this.priority = priority;
        this.isCommutativeAndAssociative = isCommutativeAndAssociative;
    }

    protected void addDistributiveOperator(Operator operator) {
        if (!operator.isCommutativeAndAssociative) throw new IllegalArgumentException();
        if (Objects.nonNull(distributiveToOperator)) throw new IllegalStateException();
        distributiveToOperator = operator;
    }

    public boolean isDistributiveTo(Operator other) {
        return isDistributive() && distributiveToOperator.equals(other);
    }

    public boolean isDistributive() {
        return Objects.nonNull(distributiveToOperator);
    }

    @Override
    public double applyAsDouble(double left, double right) {
        return operator.applyAsDouble(left, right);
    }

    @Override
    public int compareTo(Operator o) {
        return Integer.compare(this.priority, o.priority);
    }

    public OperationResult apply(OperationResult left, OperationResult right) {
        return new OperationResult(left, this, right, EOperationResultType.NORMAL);
    }

    public OperationResult postNormalizeApply(OperationResult left, OperationResult right) {
        return apply(left, right);
    }

    @Override
    public boolean equals(Object obj) {
        if (Objects.isNull(obj)) return false;
        return obj.getClass().equals(this.getClass());
    }

    protected void assertValidArgument(OperationResult operationResult) {
        if (Objects.isNull(operationResult)) throw new IllegalArgumentException("OperationResult must not be null!");
        if (!this.equals(operationResult.operator))
            throw new IllegalArgumentException(String.format("Expected Operation %s from OperationResult %s, got %s", this, operationResult, operationResult.operator));
    }

    protected List<OperationResult> sameLevelSwappableElements(OperationResult operationResult) {
        List<OperationResult> elements = new LinkedList<>();
        if (!isCommutativeAndAssociative) {
            elements.add(operationResult);
            return elements;
        }
        class PriorityWrapper implements Comparable<PriorityWrapper> {
            private static final double base = 1024;
            public final OperationResult value;
            private final boolean isLeft;
            public final int depth;
            public final double previousPriority;

            PriorityWrapper(OperationResult value, boolean isLeft, int depth, double previousPriority) {
                this.value = value;
                this.isLeft = true;
                this.depth = depth;
                this.previousPriority = previousPriority;
            }

            private double priority() {
                int multiplier;
                if (isLeft) {
                    multiplier = -1;
                } else {
                    multiplier = 1;
                }
                return previousPriority + multiplier * base / depth;
            }

            @Override
            public int compareTo(PriorityWrapper o) {
                return Double.compare(this.priority(), o.priority());
            }
        }

        Stack<OperationResult> toCheck = new Stack<>();
        toCheck.push(operationResult);
//        PriorityQueue<PriorityWrapper> toCheck = new PriorityQueue<>();
//        toCheck.add(new PriorityWrapper(operationResult, true, 1, 0));
        while (!toCheck.isEmpty()) {
            OperationResult element = toCheck.pop();
//            PriorityWrapper wrappedElement = toCheck.remove();
//            OperationResult element = wrappedElement.value;
            if (element.isFirst()) {
                elements.add(element);
            } else if (this.equals(element.operator)) {
                toCheck.push(element.left);
                toCheck.push(element.right);
//                toCheck.add(new PriorityWrapper(element.left, true, wrappedElement.depth + 1, wrappedElement.priority()));
//                toCheck.add(new PriorityWrapper(element.right, false, wrappedElement.depth + 1, wrappedElement.priority()));
            } else {
                elements.add(element);
            }
        }
        return elements;
    }

    protected List<OperationResult> distributiveElements(OperationResult operationResult) {
        if (!operationResult.isFirst() && this.isDistributiveTo(operationResult.operator)) {
            assert operationResult.operator != null;
            return operationResult.operator.sameLevelSwappableElements(operationResult);
        } else {
            List<OperationResult> elements = new LinkedList<>();
            elements.add(operationResult);
            return elements;
        }
    }

    protected OperationResult distribute(OperationResult operationResult) {
        assert operationResult.left != null;
        assert operationResult.right != null;
        assert operationResult.operator != null;
        if (!isDistributive()) return operationResult;
        List<OperationResult> leftElements = distributiveElements(operationResult.left);
        List<OperationResult> rightElements = distributiveElements(operationResult.right);
        if (leftElements.size() == 0 || rightElements.size() == 0) {
            throw new IllegalStateException();
        }
        OperationResult result = null;
        for (OperationResult leftElement : leftElements) {
            for (OperationResult rightElement : rightElements) {
                OperationResult combinedElement = fixOrder(this.apply(leftElement, rightElement));
                if (Objects.isNull(result)) {
                    result = combinedElement;
                } else {
                    result = result.apply(distributiveToOperator, combinedElement);
                }
            }
        }
        return result;
    }

    public OperationResult normalize(OperationResult operationResult) {
        assertValidArgument(operationResult);
        if (operationResult.isFirst()) {
            return operationResult;
        }
        assert operationResult.left != null;
        assert operationResult.right != null;
        OperationResult leftNormalized = operationResult.left.getNormalized();
        OperationResult rightNormalized = operationResult.right.getNormalized();
        OperationResult reconstructed = leftNormalized.apply(this, rightNormalized);
//        if (reconstructed.toString().equals("2 - (7 + 3 * 9 + 8 * -1)")) {
        if (reconstructed.toString().equals("-1 * (7 + 3 * 9 + 8 * -1)")) {
            System.out.println("lol");
        }
        OperationResult distributed = distribute(reconstructed);
        return fixOrder(postNormalize(distributed));
    }

    private OperationResult fixOrder(OperationResult operationResult) {
//        Optional<OperationResult> result = sameLevelSwappableElements(operationResult).stream().map(or -> Objects.nonNull(or.operator) ? or.operator.fixOrder(or) : or).sorted().reduce(this::postNormalizeApply);
        Optional<OperationResult> result = sameLevelSwappableElements(operationResult).stream().sorted().reduce(this::postNormalizeApply);
        if (result.isEmpty())
            throw new IllegalStateException(String.format("Normalization result for %s was empty!", operationResult));
        return result.get();
    }

    protected OperationResult postNormalize(OperationResult operationResult) {
        return operationResult;
    }
}

class Add extends Operator {
    Add() {
        super(Double::sum, "+", 10, true);
    }
}

class Subtract extends Operator {
    private static final Add ADD = new Add();
    private static final Multiply MULTIPLY = new Multiply();
    public static final OperationResult MINUS_1_IGNORABLE = new OperationResult(-1);

    Subtract() {
        super((double left, double right) -> left - right, "-", 10, false);
    }

    @Override
    public OperationResult postNormalize(OperationResult operationResult) {
        assert operationResult.left != null;
        assert operationResult.right != null;
        return operationResult.left.apply(ADD, MINUS_1_IGNORABLE.apply(MULTIPLY, operationResult.right)).getNormalized();
    }

    @Override
    public OperationResult postNormalizeApply(OperationResult left, OperationResult right) {
        return ADD.apply(left, right);
    }
}

class Multiply extends Operator {
    private final static Add ADD = new Add();

    Multiply() {
        super((double left, double right) -> left * right, "*", 20, true);
        addDistributiveOperator(ADD);
    }

    @Override
    public OperationResult apply(OperationResult left, OperationResult right) {
        return apply(left, right, true);
    }

    private OperationResult apply(OperationResult left, OperationResult right, boolean removeDuplicateMinusOnes) {
        OperationResult result = super.apply(left, right);
        if (removeDuplicateMinusOnes) result = removeDuplicateMinusOnes(result);
        return result;
    }

    private OperationResult removeDuplicateMinusOnes(OperationResult operationResult) {
//        System.out.println("removing dupls");
        List<OperationResult> elements = sameLevelSwappableElements(operationResult);
        List<OperationResult> minusOnes = elements.stream().filter(or -> Subtract.MINUS_1_IGNORABLE == or).toList();
//        System.out.println(operationResult.left);
//        System.out.println(operationResult.right);
//        System.out.println(elements);
//        System.out.println(minusOnes);
        for (int i = 0; i < minusOnes.size() / 2; i++) {
            elements.remove(minusOnes.get(i));
            elements.remove(minusOnes.get(i + 1));
        }

        OperationResult result = elements.get(0);
        elements.remove(0);
        for (OperationResult element : elements) {
            result = apply(result, element, false);
        }
        return result;
    }

//    @Override
//    protected OperationResult postNormalize(OperationResult operationResult) {
//        return removeDuplicateMinusOnes(operationResult);
//    }
}

class Divide extends Operator {
    Divide() {
        super((double left, double right) -> left / right, "/", 20, false);
    }
}

enum EOperationResultType {
    NORMAL,
    INTERMEDIATE_WRAPPER,
    INTERMEDIATE_CONTENTS
}

class OperationResult implements Comparable<OperationResult> {
    public final OperationResult left;
    public final OperationResult right;
    public final Double resultValue;
    public final Operator operator;
    private final EOperationResultType type;
    private OperationResult normalized = null;
    boolean isNormalized = false;

    public OperationResult(double value) {
        this.left = null;
        operator = null;
        this.right = null;
        resultValue = value;
        type = EOperationResultType.NORMAL;
        isNormalized = true;
    }

    public OperationResult(OperationResult left, Operator operator, OperationResult right, EOperationResultType type) {
        this.left = left;
        this.operator = operator;
        this.right = right;
        resultValue = operator.applyAsDouble(left.resultValue, right.resultValue);
        this.type = type;
    }

    public boolean isFirst() {
        if (Objects.isNull(left) && Objects.isNull(right)) return true;
        if (Objects.isNull(left) || Objects.isNull(right))
            throw new IllegalStateException("Both or neither of original OperationResults must be null!");
        return false;
    }

    public int length() {
        if (isFirst()) return 1;
        if (type == EOperationResultType.INTERMEDIATE_CONTENTS) {
            return right.length();
        }
        return left.length() + right.length();
    }

    public OperationResult apply(Operator operator, double other) {
        return apply(operator, new OperationResult(other));
    }

    public OperationResult apply(Operator operator, OperationResult other) {
        return operator.apply(this, other);
    }

    public int getRank() {
        return toString().hashCode();
    }

    public OperationResult getNormalized() {
        if (isNormalized) return this;
        if (Objects.isNull(normalized)) {
            if (this.toString().equals("2 - (3 * 9 - 8 + 7)")) {
                System.out.println("l");
            }
            normalized = operator.normalize(this);
            normalized.isNormalized = true;
            if (!isNormalized && normalized.toString().equals(this.toString())) isNormalized = true;
        }
        return normalized;
    }

    public boolean isEquivalent(OperationResult other) {
//        if (true) return false;
        if (Objects.isNull(other)) return false;
        if (!Utils.doubleEquals(this.resultValue, other.resultValue)) return false;
        if (this.length() != other.length()) return false;
        if (this.isFirst() && other.isFirst()) return true;
        if (this.operator.equals(other.operator) && (this.left.isEquivalent(other.left) && this.right.isEquivalent(other.right)))
            return true;
        return this.getNormalized().toString().equals(other.getNormalized().toString());
    }

    @Override
    public String toString() {
        switch (type) {
            case NORMAL -> {
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
            case INTERMEDIATE_WRAPPER -> {
                if (right.type != EOperationResultType.INTERMEDIATE_CONTENTS) throw new IllegalStateException();
                if (isFirst()) throw new IllegalStateException();
                return String.format("%s %s %s",
                        wrapInParenthesesIfNeeded(left, true),
                        operator.displayString,
                        right);
            }
            case INTERMEDIATE_CONTENTS -> {
                if (isFirst()) throw new IllegalStateException();
                return wrapInParenthesesIfNeeded(right, false);
            }
            default ->
                    throw new IllegalStateException(String.format("Unknown %s: '%s'", EOperationResultType.class.getName(), type));
        }
    }

    private boolean shouldWrapInParentheses(OperationResult operationResult, boolean isLeft) {
//        if (true) return true;
        return !(operationResult.isFirst() ||
                operationResult.operator.priority > this.operator.priority ||
                (operationResult.operator.equals(this.operator) && operator.isCommutativeAndAssociative) ||
                (isLeft && operationResult.operator.priority == this.operator.priority));
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

    @Override
    public int compareTo(OperationResult o) {
        if (Objects.isNull(o))
            throw new IllegalArgumentException(String.format("Can't compare %s to null!", this.getClass().getName()));
        return Integer.compare(getRank(), o.getRank());
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
            OperationResult a = new OperationResult(1).apply(ADD, 2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5);  // 1 + 2 + 3 * 4 + 5
            OperationResult b = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4))).apply(ADD, 5);  // 1 + (2 + 3 * 4) + 5
            OperationResult c = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5));  // 1 + (2 + 3 * 4 + 5)
            System.out.println(a.isEquivalent(b));
            System.out.println(a.isEquivalent(c));
            System.out.println(b.isEquivalent(c));
            return;
        }
        if (false) {
            OperationResult a = nine.apply(ADD, three.apply(MUL, four).apply(SUB, seven));
            OperationResult b = nine.apply(ADD, three.apply(MUL, four)).apply(SUB, seven);

            System.out.println(a);
            System.out.println(a.getNormalized());
            System.out.println(b);
            System.out.println(b.getNormalized());
            System.out.println(a.isEquivalent(b));
            System.out.println(b.isEquivalent(a));
            return;
        }
        if (false) {
            OperationResult a = two.apply(SUB, three.apply(MUL, nine).apply(SUB, eight).apply(ADD, seven)).apply(SUB, 2);  // 2 - (3 * 9 - 8 + 7) - 2;
            OperationResult b = eight.apply(SUB, three.apply(MUL, nine)).apply(SUB, seven.apply(ADD, two)).apply(ADD, two);  // 8 - 3 * 9 - (7 + 2) + 2;

            System.out.println(a);
            System.out.println(a.getNormalized());
            System.out.println(b);
            System.out.println(b.getNormalized());
            System.out.println(a.isEquivalent(b));
            return;
        }

        List<Operator> operators = List.of(ADD, SUB, MUL, DIV);

        List<Double> numbers = Stream.of(2, 3, 4, 5, 7, 8, 9, 44, 55).map(Integer::doubleValue).toList();

        Collection<OperationResult> result = operationCombinations(
                operators,
                numbers,
                4,
                14,
                true,
                false
        );

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
            System.out.println(length);
            System.out.println(potentialArguments.size());
            int lengthForLambda = length;
            for (OperationResult base : potentialArguments.stream().filter(or -> or.length() == lengthForLambda).toList()) {
                for (OperationResult other : potentialArguments.stream().filter(or -> or.length() <= numsAmount - lengthForLambda && (reUseAllowed || Collections.disjoint(base.usedOriginals(), or.usedOriginals()))).toList()) {
                    for (Operator operator : operators) {
                        for (OperationResult newResult : List.of(base.apply(operator, other))) {
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