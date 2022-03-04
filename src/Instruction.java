public class Instruction {
    Operators operator;
    Operand firstOp;
    Operand secondOp;
    int IDNum;
    static int instrNum = 1;
    boolean duplicate;

    public Instruction(int id) {
        this.operator = Operators.empty;
        firstOp = null;
        secondOp = null;
        IDNum = id;
    }

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
    }

    public Instruction(Instruction instruction) {
        this.operator = instruction.operator;
        if(instruction.firstOp==null){
            this.firstOp = null;
        }
        else{
            this.firstOp = new Operand(instruction.firstOp);
        }
        if(instruction.secondOp==null){
            this.secondOp = null;
        }
        else{
            this.secondOp = new Operand(instruction.secondOp);
        }
        IDNum = instrNum;
        instrNum++;
    }

    public String toString() {
        String ts = operator.toString();
        if (firstOp != null) {
            if (firstOp.valGenerator!= null) {
                ts += "(" + firstOp.valGenerator + ")";
            } else {
                ts += "#" + firstOp.constVal;
            }
        }
        if (secondOp != null) {
            if (secondOp.valGenerator!= null) {
                ts += "(" + secondOp.valGenerator + ")";
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
