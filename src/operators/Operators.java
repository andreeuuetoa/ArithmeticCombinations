package operators;

import java.util.List;

public class Operators {
    public final static Add ADD = new Add();
    public final static Subtract SUB = new Subtract();
    public final static Multiply MUL = new Multiply();
    public final static Divide DIV = new Divide();

    public final static List<Operator> DEFAULT_OPERATORS = List.of(ADD, SUB, MUL, DIV);
}
