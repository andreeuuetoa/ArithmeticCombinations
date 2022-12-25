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
    public OperationResult postNormalize(OperationResult operationResult) {
        assert operationResult.left != null;
        assert operationResult.right != null;

        OperationResult left = operationResult.left;
        OperationResult right = operationResult.right;
        if (left.isFirst() && right.isFirst() && right.getRank() > left.getRank()) {
            OperationResult temp = left;
            left = right;
            right = temp;
        }
        return left.apply(ADD, MINUS_1_IGNORABLE.apply(MUL, right)).getNormalized();
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

        for (int i = 0; i < minusOnes.size() / 2; i++) {
            elements.remove(minusOnes.get(i));
            elements.remove(minusOnes.get(i + 1));
        }

        OperationResult result = elements.get(0);
        elements.remove(0);
        for (OperationResult element : elements) {
            result = apply(result, element);
        }
        return result;
    }

    @Override
    public OperationResult postNormalizeApply(OperationResult left, OperationResult right) {
        OperationResult result = super.postNormalizeApply(left, right);
        if (left.isFirst() && Utils.doubleEquals(left.resultValue, 1)) result = right;
        if (right.isFirst() && Utils.doubleEquals(right.resultValue, 1)) result = left;
        result = removeDuplicateMinusOnes(result);
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

    @Override
    protected boolean shouldNotNormalize(OperationResult operationResult) {
        return super.shouldNotNormalize(operationResult) || isOneDivision(operationResult);
    }

    private boolean isOneDivision(OperationResult operationResult) {
        if (operationResult.isFirst()) return false;
        assert operationResult.right != null;
        return MUL.equals(operationResult.operator) && ONE == operationResult.right.left && this.equals(operationResult.right.operator);
    }

    @Override
    protected OperationResult postNormalize(OperationResult operationResult) {
        assert operationResult.left != null;
        assert operationResult.right != null;
        OperationResult result = operationResult.left;

        List<OperationResult> rightElements = MUL.sameLevelSwappableElements(operationResult.right);
        assert rightElements.size() > 0;

        for (OperationResult rightElement : rightElements) {
            if (isOneDivision(rightElement)) {
                result = result.apply(MUL, rightElement.right);
            } else {
                result = result.apply(MUL, ONE.apply(this, rightElement));
            }
        }

        return ADD.fixOrder(
                MUL.distributedElements(result).stream()
                        .reduce((or1, or2) -> MUL.distributiveToOperator.apply(or1, MUL.fixOrder(or2)))
                        .orElseThrow()
        );
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
