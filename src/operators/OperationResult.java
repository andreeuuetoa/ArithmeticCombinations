package operators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class OperationResult implements Comparable<OperationResult> {
    public final OperationResult left;
    public final OperationResult right;
    public final Double resultValue;
    public final Operator operator;
    boolean isNormalized = false;

    public OperationResult(double value) {
        this.left = null;
        operator = null;
        this.right = null;
        resultValue = value;
        isNormalized = true;
    }

    public OperationResult(OperationResult left, Operator operator, OperationResult right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
        resultValue = operator.applyAsDouble(left.resultValue, right.resultValue);
    }

    public boolean isFirst() {
        if (Objects.isNull(left) && Objects.isNull(right)) return true;
        if (Objects.isNull(left) || Objects.isNull(right))
            throw new IllegalStateException("Both or neither of original OperationResults must be null!");
        return false;
    }

    public int length() {
        if (isFirst()) return 1;
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
        return getNormalized(new NormalizationState());
    }

    OperationResult getNormalized(NormalizationState normalizationState) {
        if (isNormalized) return this;
        return operator.normalize(this, normalizationState);
    }

    public boolean isEquivalent(OperationResult other) {
        if (Objects.isNull(other)) return false;
        if (!Utils.doubleEquals(this.resultValue, other.resultValue)) return false;
        if (this.isFirst() && other.isFirst()) return true;
        if (!this.isFirst() && !other.isFirst()
                && this.operator.equals(other.operator)
                && this.left.isEquivalent(other.left) && this.right.isEquivalent(other.right))
            return true;
        return this.getNormalized().toString().equals(other.getNormalized().toString());
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
                operationResult.operator.priority > this.operator.priority ||
                (operationResult.operator.equals(this.operator) && operator.isCommutativeAndAssociative) ||
                (isLeft && operationResult.operator.priority == this.operator.priority));
    }

    private String wrapInParenthesesIfNeeded(OperationResult operationResult, boolean isLeft) {
        if (shouldWrapInParentheses(operationResult, isLeft))
            return Utils.wrapInParentheses(operationResult.toString());
        return operationResult.toString();
    }

    public HashMap<Double, Integer> usedOriginalsWithCounts() {
        HashMap<Double, Integer> result;
        if (isFirst()) {
            result = new HashMap<>();
            result.put(resultValue, 1);
        } else {
            result = left.usedOriginalsWithCounts();
            HashMap<Double, Integer> rightUsedOriginals = right.usedOriginalsWithCounts();
            for (Double key : rightUsedOriginals.keySet()) {
                result.put(key, result.getOrDefault(key, 0) + rightUsedOriginals.get(key));
            }
        }
        return result;
    }

    public Set<Double> usedOriginals() {
        return usedOriginalsWithCounts().keySet();
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OperationResult other)) return false;
        return resultValue.equals(other.resultValue);
    }
}
