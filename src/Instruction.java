import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Instruction {
    Operators operator;
    Operand firstOp;
    Operand secondOp;
    int IDNum;
    static int instrNum = 1;
    boolean noDuplicateCheck;
    int arrayID;
    String storeRegister;
    List<Integer> arguments;
    int cost;

    public Instruction(Operators operator, Operand firstOp, Operand secondOp) {
        this.operator = operator;
        this.firstOp = firstOp;
        this.secondOp = secondOp;
        storeRegister = null;
        IDNum = instrNum;
        instrNum++;
        arguments = new ArrayList<>();
        cost = 0;
    }

    public Instruction(Operators operator) {
        this.operator = operator;
        firstOp = null;
        secondOp = null;
        IDNum = instrNum;
        instrNum++;
        arguments = new ArrayList<>();
    }

    public Instruction(Operators operator, Operand opr) {
        this.operator = operator;
        this.firstOp = opr;
        secondOp = null;
        IDNum = instrNum;
        instrNum++;
        arrayID = 0;
        arguments = new ArrayList<>();
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
        if(storeRegister!=null){
            ts += " -- "+storeRegister;
        }
        if(arguments!=null && arguments.size()>0){
            ts += " - "+"(" +arguments.get(0)+ ")";
            for(int i=1; i<arguments.size(); i++){
                ts += ", ( "+arguments.get(i)+ ")";
            }
            ts += " -";
        }
        return ts;
    }
}
