package operators;

import java.util.List;

class Add extends Operator {
    Add() {
        super(Double::sum, "+", 10, true);
    }
}

class Subtract extends Operator {
    private static final Add ADD = new Add();
    private static final Multiply MUL = new Multiply();
    public static final OperationResult MINUS_1_IGNORABLE = new OperationResult(-1);

    Subtract() {
        super((double left, double right) -> left - right, "-", 10, false);
    }

    @Override
    public OperationResult postNormalize(OperationResult operationResult, NormalizationState normalizationState) {
        assert operationResult.left != null;
        assert operationResult.right != null;

        OperationResult left = operationResult.left;
        OperationResult right = operationResult.right;
        boolean flipForConsistency = right.getRank() > left.getRank();
        if (flipForConsistency) {
            OperationResult temp = left;
            left = right;
            right = temp;
        }
        OperationResult result = left.apply(ADD, MINUS_1_IGNORABLE.apply(MUL, right)).getNormalized(normalizationState);
        if (flipForConsistency) {
            result = MINUS_1_IGNORABLE.apply(MUL, result);
            if (normalizationState.divideCounter == 0) {
                result = MUL.distribute(result);
            }
            result = MUL.fixOrder(result);
        }
        return super.postNormalize(result, normalizationState);
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

    private OperationResult removeDuplicateMinusOnes(OperationResult operationResult) {
        List<OperationResult> elements = sameLevelSwappableElements(operationResult);
        List<OperationResult> minusOnes = elements.stream().filter(or -> Subtract.MINUS_1_IGNORABLE == or).toList();

        if (elements.isEmpty()) throw new IllegalStateException();

        for (int i = 0; i < minusOnes.size() / 2; i++) {
            elements.remove(minusOnes.get(i));
            elements.remove(minusOnes.get(i + 1));
        }

        OperationResult result;
        if (elements.isEmpty()) {
            result = new OperationResult(1);
        } else {
            result = elements.get(0);
            elements.remove(0);
        }
        for (OperationResult element : elements) {
            result = apply(result, element);
        }
        return result;
    }

    OperationResult reduceOnes(OperationResult operationResult) {
        if (operationResult.isFirst()) return operationResult;
        OperationResult left = operationResult.left;
        OperationResult right = operationResult.right;
        if (left.isFirst() && Utils.doubleEquals(left.resultValue, 1)) return right;
        if (right.isFirst() && Utils.doubleEquals(right.resultValue, 1)) return left;
        return operationResult;
    }

    protected OperationResult fixOrder(OperationResult operationResult, boolean shouldRemoveDuplicateMinusOnes) {
        if (shouldRemoveDuplicateMinusOnes) return super.fixOrder(operationResult);
        return sameLevelSwappableElements(operationResult).stream()
                .sorted()
                .reduce((or1, or2) -> postNormalizeApply(or1, or2, false))
                .orElseThrow();
    }

    @Override
    public OperationResult postNormalizeApply(OperationResult left, OperationResult right) {
        return postNormalizeApply(left, right, true);
    }

    public OperationResult postNormalizeApply(OperationResult left, OperationResult right, boolean shouldRemoveDuplicateMinusOnes) {
        OperationResult result = super.postNormalizeApply(left, right);
        result = reduceOnes(result);
        if (shouldRemoveDuplicateMinusOnes) result = removeDuplicateMinusOnes(result);
        return result;
    }
}

class Divide extends Operator {
    private static final Multiply MUL = new Multiply();
    private static final OperationResult ONE = new OperationResult(1);
    private static final Add ADD = new Add();

    Divide() {
        super((double left, double right) -> left / right, "/", 20, false);
    }

    private boolean isOne(OperationResult operationResult) {
        return operationResult.isFirst() && Utils.doubleEquals(operationResult.resultValue, 1);
    }

    private boolean isMinusOne(OperationResult operationResult) {
        return operationResult.isFirst() && Utils.doubleEquals(operationResult.resultValue, -1);
    }

    private boolean isOneDivision(OperationResult operationResult) {
        if (operationResult.isFirst()) return false;
        assert operationResult.right != null;
        return (MUL.equals(operationResult.operator) && ONE.isEquivalent(operationResult.right.left) && this.equals(operationResult.right.operator));
    }

    private boolean isStraightOneDivision(OperationResult operationResult) {
        if (operationResult.isFirst()) return false;
        assert operationResult.right != null;
        assert operationResult.left != null;
        return this.equals(operationResult.operator) && isOne(operationResult.left);
    }

    @Override
    protected OperationResult preNormalize(OperationResult operationResult, NormalizationState normalizationState) {
        operationResult = super.preNormalize(operationResult, normalizationState);
        normalizationState.divideCounter++;
        return operationResult;
    }

    @Override
    protected OperationResult postNormalize(OperationResult operationResult, NormalizationState normalizationState) {
        assert operationResult.left != null;
        assert operationResult.right != null;
        OperationResult result = operationResult.left;

        List<OperationResult> rightElements = MUL.sameLevelSwappableElements(operationResult.right);
        assert rightElements.size() > 0;

        for (OperationResult rightElement : rightElements) {
            if (isOne(rightElement) || isMinusOne(rightElement)) {
                result = result.apply(MUL, rightElement);
            } else if (isStraightOneDivision(rightElement)) {
                result = result.apply(MUL, rightElement.right);
            } else if (isOneDivision(rightElement)) {
                assert rightElement.right != null;
                result = result.apply(MUL, rightElement.right.right);
            } else  {
                result = result.apply(MUL, ONE.apply(this, rightElement));
            }
        }

        if (normalizationState.divideCounter == 1) {
            result = result.getNormalized(normalizationState);
        } else {
            result = MUL.fixOrder(result);
        }
        normalizationState.divideCounter--;
        return super.postNormalize(result, normalizationState);

        // TODO: convert division by one into multiplication by one
    }

    @Override
    public OperationResult postNormalizeApply(OperationResult left, OperationResult right) {
        return left.apply(MUL, right);
    }
}

public class Operators {
    public final static Add ADD = new Add();
    public final static Subtract SUB = new Subtract();
    public final static Multiply MUL = new Multiply();
    public final static Divide DIV = new Divide();

    public final static List<Operator> DEFAULT_OPERATORS = List.of(ADD, SUB, MUL, DIV);
}
