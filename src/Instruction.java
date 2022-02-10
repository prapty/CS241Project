public class Instruction {
    Operators operator;
    Operand firstOp;
    Operand secondOp;

    public Instruction(Operators operator, Operand firstOp, Operand secondOp) {
        this.operator = operator;
        this.firstOp = firstOp;
        this.secondOp = secondOp;
    }

    public Instruction(Operators operator){
        this.operator = operator;
        firstOp = null;
        secondOp = null;
    }

    public Instruction(Operators operator, Operand opr){
        this.operator = operator;
        this.firstOp = opr;
        secondOp = null;
    }

    public Instruction(Instruction instruction){
        this.operator=instruction.operator;
        this.firstOp=instruction.firstOp;
        this.secondOp= instruction.secondOp;
    }
}
