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
    private OperationResult normalized = null;
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

    public boolean hasBeenNormalized() {
        return Objects.nonNull(normalized);
    }

    public OperationResult getNormalized() {
        if (isNormalized) return this;
        if (!hasBeenNormalized()) {
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
//        if (this.length() != other.length()) return false;
        if (this.isFirst() && other.isFirst()) return true;
        if (this.isFirst() != other.isFirst()) return false;
        if (this.operator.equals(other.operator) && (this.left.isEquivalent(other.left) && this.right.isEquivalent(other.right)))
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
//        HashSet<Double> result;
//        if (!isFirst()) {
//            result = left.usedOriginals();
//            result.addAll(right.usedOriginals());
//        } else {
//            result = new HashSet<>();
//            result.add(resultValue);
//        }
//        return result;
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
