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
        return operationResult.left.apply(ADD, MINUS_1_IGNORABLE.apply(MUL, operationResult.right)).getNormalized();
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
//    protected operators.OperationResult postNormalize(operators.OperationResult operationResult) {
//        return removeDuplicateMinusOnes(operationResult);
//    }
}

class Divide extends Operator {
    Divide() {
        super((double left, double right) -> left / right, "/", 20, false);
    }
}

public class Operators {
    public final static Add ADD = new Add();
    public final static Subtract SUB = new Subtract();
    public final static Multiply MUL = new Multiply();
    public final static Divide DIV = new Divide();

    public final static List<Operator> DEFAULT_OPERATORS = List.of(ADD, SUB, MUL, DIV);
}
