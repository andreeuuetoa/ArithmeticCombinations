package operators;

public class Add extends Operator {
    Add() {
        super(Double::sum, "+", 10, true);
    }
}
