package operators;

import java.util.List;

public class Divide extends Operator {
    private static final Multiply MUL = new Multiply();
    private static final OperationResult ONE = new OperationResult(1);

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
    OperationResult preNormalize(OperationResult operationResult, NormalizationState normalizationState) {
        operationResult = super.preNormalize(operationResult, normalizationState);
        normalizationState.divideCounter++;
        return operationResult;
    }

    @Override
    OperationResult postNormalize(OperationResult operationResult, NormalizationState normalizationState) {
        assert operationResult.left != null;
        assert operationResult.right != null;
        OperationResult result = operationResult.left;

        List<OperationResult> rightElements = MUL.sameLevelSwappableElements(operationResult.right);
        assert !rightElements.isEmpty();

        for (OperationResult rightElement : rightElements) {
            if (isOne(rightElement) || isMinusOne(rightElement)) {
                result = result.apply(MUL, rightElement);
            } else if (isStraightOneDivision(rightElement)) {
                result = result.apply(MUL, rightElement.right);
            } else if (isOneDivision(rightElement)) {
                assert rightElement.right != null;
                result = result.apply(MUL, rightElement.right.right);
            } else {
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
    }

    @Override
    public OperationResult postNormalizeApply(OperationResult left, OperationResult right) {
        return left.apply(MUL, right);
    }
}
