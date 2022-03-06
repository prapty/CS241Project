import java.util.Map;

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

    public void modifyInstruction(Map<Integer, Instruction> copyMap) {
        if(this.firstOp!=null && this.firstOp.valGenerator!=null){
            Instruction newVal = copyMap.get(this.firstOp.valGenerator);
            if(newVal !=null){
                this.firstOp.valGenerator=newVal.IDNum;
            }
        }
        if(this.secondOp!=null && this.secondOp.valGenerator!=null){
            Instruction newVal = copyMap.get(this.secondOp.valGenerator);
            if(newVal !=null){
                this.secondOp.valGenerator=newVal.IDNum;
            }
        }
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
