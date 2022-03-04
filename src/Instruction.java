public class Instruction {
    Operators operator;
    Operand firstOp;
    Operand secondOp;
    int IDNum;
    static int instrNum = 1;
    boolean duplicate;
    int arrayID;

    public Instruction(Operators operator, Operand firstOp, Operand secondOp) {
        this.operator = operator;
        this.firstOp = firstOp;
        this.secondOp = secondOp;
        IDNum = instrNum;
        instrNum++;
    }

    public Instruction(Operators operator) {
        this.operator = operator;
        firstOp = null;
        secondOp = null;
        IDNum = instrNum;
        instrNum++;
    }

    public Instruction(Operators operator, Operand opr) {
        this.operator = operator;
        this.firstOp = opr;
        secondOp = null;
        IDNum = instrNum;
        instrNum++;
        arrayID = 0;
    }

    public Instruction(Instruction instruction) {
        this.operator = instruction.operator;
        this.firstOp = new Operand(instruction.firstOp);
        this.secondOp = new Operand(instruction.secondOp);
        IDNum = instrNum;
        instrNum++;
    }

    public String toString() {
        String ts = operator.toString();
        if (firstOp != null) {
            if (firstOp.arraybase != null) {
                ts += " " + firstOp.arraybase + " ";
                ;
            } else if (firstOp.valGenerator != null) {
                ts += "(" + firstOp.valGenerator + ")";
            } else {
                ts += "#" + firstOp.constVal;
            }
        }
        if (secondOp != null) {
            if (secondOp.arraybase != null) {
                ts += secondOp.arraybase;
                ;
            } else if (secondOp.valGenerator != null) {
                ts += "(" + secondOp.valGenerator + ")";
            } else {
                ts += "#" + secondOp.constVal;
            }
        }
        if (duplicate) {
            ts += ", duplicate";
        }
        return ts;
    }
}
