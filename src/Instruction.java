public class Instruction {
    Operators operator;
    Operand firstOp;
    Operand secondOp;
    boolean duplicate;

    public Instruction(Operators operator, Operand firstOp, Operand secondOp) {
        this.operator = operator;
        this.firstOp = firstOp;
        this.secondOp = secondOp;
        this.duplicate = false;
    }
}
