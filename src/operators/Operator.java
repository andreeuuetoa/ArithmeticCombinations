package operators;

import java.util.*;
import java.util.function.DoubleBinaryOperator;

public class Operator implements DoubleBinaryOperator, Comparable<Operator> {
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
        return new OperationResult(left, this, right);
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
        if (Objects.isNull(operationResult)) throw new IllegalArgumentException("operators.OperationResult must not be null!");
        if (!this.equals(operationResult.operator))
            throw new IllegalArgumentException(String.format("Expected Operation %s from operators.OperationResult %s, got %s", this, operationResult, operationResult.operator));
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
//            operators.OperationResult element = wrappedElement.value;
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
//        Optional<operators.OperationResult> result = sameLevelSwappableElements(operationResult).stream().map(or -> Objects.nonNull(or.operator) ? or.operator.fixOrder(or) : or).sorted().reduce(this::postNormalizeApply);
        Optional<OperationResult> result = sameLevelSwappableElements(operationResult).stream().sorted().reduce(this::postNormalizeApply);
        if (result.isEmpty())
            throw new IllegalStateException(String.format("Normalization result for %s was empty!", operationResult));
        return result.get();
    }

    protected OperationResult postNormalize(OperationResult operationResult) {
        return operationResult;
    }
}
