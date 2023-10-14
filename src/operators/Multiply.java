package operators;

import java.util.List;

public class Multiply extends Operator {
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
