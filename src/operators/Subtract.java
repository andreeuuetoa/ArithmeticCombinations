package operators;

public class Subtract extends Operator {
    private static final Add ADD = new Add();
    private static final Multiply MUL = new Multiply();
    public static final OperationResult MINUS_1_IGNORABLE = new OperationResult(-1);

    Subtract() {
        super((double left, double right) -> left - right, "-", 10, false);
    }

    @Override
    OperationResult postNormalize(OperationResult operationResult, NormalizationState normalizationState) {
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
