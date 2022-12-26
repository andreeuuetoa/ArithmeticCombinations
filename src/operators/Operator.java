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
        if (Objects.isNull(operationResult))
            throw new IllegalArgumentException("operators.OperationResult must not be null!");
        if (!this.equals(operationResult.operator))
            throw new IllegalArgumentException(String.format("Expected Operation %s from operators.OperationResult %s, got %s", this, operationResult, operationResult.operator));
    }

    protected List<OperationResult> sameLevelSwappableElements(OperationResult operationResult) {
        List<OperationResult> elements = new LinkedList<>();
        if (!isCommutativeAndAssociative) {
            elements.add(operationResult);
            return elements;
        }

        Stack<OperationResult> toCheck = new Stack<>();
        toCheck.push(operationResult);
        while (!toCheck.isEmpty()) {
            OperationResult element = toCheck.pop();
            if (element.isFirst()) {
                elements.add(element);
            } else if (this.equals(element.operator)) {
                toCheck.push(element.left);
                toCheck.push(element.right);
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

    protected List<OperationResult> distributedElements(OperationResult operationResult) {
        List<OperationResult> result = new LinkedList<>();
        if (!isDistributive() || operationResult.isFirst()) {
            result.add(operationResult);
            return result;
        }
        assert operationResult.left != null;
        assert operationResult.right != null;
        assert operationResult.operator != null;
        List<OperationResult> leftElements = distributiveElements(operationResult.left);
        List<OperationResult> rightElements = distributiveElements(operationResult.right);
        if (leftElements.size() == 0 || rightElements.size() == 0) {
            throw new IllegalStateException();
        }
        for (OperationResult leftElement : leftElements) {
            for (OperationResult rightElement : rightElements) {
                OperationResult combinedElement = fixOrder(this.apply(leftElement, rightElement));
                result.add(combinedElement);
            }
        }
        return result;
    }

    protected OperationResult distribute(OperationResult operationResult) {
        List<OperationResult> distributedElements = distributedElements(operationResult);
        return distributedElements.stream()
                .reduce((or1, or2) -> or1.apply(distributiveToOperator, or2))
                .orElseThrow();
    }

    protected boolean shouldNotNormalize(OperationResult operationResult) {
        return operationResult.isNormalized;
    }

    public OperationResult normalize(OperationResult operationResult, NormalizationState normalizationState) {
        operationResult = preNormalize(operationResult, normalizationState);
        assertValidArgument(operationResult);
        if (shouldNotNormalize(operationResult)) return operationResult;
        if (operationResult.isFirst()) {
            return operationResult;
        }
        assert operationResult.left != null;
        assert operationResult.right != null;
        OperationResult leftNormalized = operationResult.left.getNormalized(normalizationState);
        OperationResult rightNormalized = operationResult.right.getNormalized(normalizationState);
        OperationResult reconstructed = leftNormalized.apply(this, rightNormalized);
        OperationResult distributed = distribute(reconstructed);
        OperationResult result = postNormalize(distributed, normalizationState);
        if (result.isFirst()) return result;
        assert result.operator != null;
        return result.operator.fixOrder(result);
    }

    protected OperationResult fixOrder(OperationResult operationResult) {
        return sameLevelSwappableElements(operationResult).stream()
                .sorted()
                .reduce(this::postNormalizeApply)
                .orElseThrow();
    }

    protected OperationResult preNormalize(OperationResult operationResult, NormalizationState normalizationState) {
        return operationResult;
    }

    protected OperationResult postNormalize(OperationResult operationResult, NormalizationState normalizationState) {
        return operationResult;
    }
}
