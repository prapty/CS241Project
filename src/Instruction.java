public class Instruction {
    Operators operator;
    Operand firstOp;
    Operand secondOp;
    int IDNum;
    static int instrNum = 1;
    boolean duplicate;

    public Instruction(Operators operator, Operand firstOp, Operand secondOp) {
        this.operator = operator;
        this.firstOp = firstOp;
        this.secondOp = secondOp;
    }

    public Instruction(Operators operator) {
        this.operator = operator;
        firstOp = null;
        secondOp = null;
    }

    public Instruction(Operators operator, Operand opr) {
        this.operator = operator;
        this.firstOp = opr;
        secondOp = null;
    }

    public Instruction(Instruction instruction) {
        this.operator = instruction.operator;
        this.firstOp = instruction.firstOp;
        this.secondOp = instruction.secondOp;
    }

    public String toString() {
        String ts = operator.toString();
        if (firstOp != null) {
            if (firstOp.valGenerator!= null) {
                ts += "(" + firstOp.valGenerator.IDNum + ")";
            } else {
                ts += "#" + firstOp.constVal;
            }
        }
        if (secondOp != null) {
            if (secondOp.valGenerator!= null) {
                ts += "(" + secondOp.valGenerator.IDNum + ")";
            } else {
                ts += "#" + secondOp.constVal;
            }
        }
        if(duplicate){
            ts +=", duplicate";
        }
        return ts;
    }
}
