import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Instruction {
    Operators operator;
    Operand firstOp;
    Operand secondOp;
    Operand thirdOp;
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

    public Instruction(Operators operator, Operand firstOp, Operand secondOp, Operand thirdOp) {
        this.operator = operator;
        this.firstOp = firstOp;
        this.secondOp = secondOp;
        this.thirdOp = thirdOp;
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

    public String toString() {
        String ts = operator.toString();
        if (firstOp != null) {
            if (firstOp.arraybase != null) {
                ts += " " + firstOp.arraybase + " ";
                ;
            }
            else if (firstOp.valGenerator != null) {
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
        if (thirdOp != null) {
            if (thirdOp.arraybase != null) {
                ts += thirdOp.arraybase;
                ;
            } else if (thirdOp.valGenerator != null) {
                ts += "(" + thirdOp.valGenerator + ")";
            } else {
                ts += "#" + thirdOp.constVal;
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

    public String toString(Map<Integer, Instruction>idInstructionMap) {
        List<Operators> noChange = new ArrayList<>(Arrays.asList(Operators.bra, Operators.bne, Operators.beq, Operators.ble, Operators.blt, Operators.bge, Operators.bgt, Operators.kill, Operators.jsr));

        String ts = operator.toString();
        if (firstOp != null) {
            if (firstOp.arraybase != null) {
                ts += " " + firstOp.arraybase + " ";
                ;
            }
            else if (firstOp.valGenerator != null) {
                Instruction valInstr = idInstructionMap.get(firstOp.valGenerator);
                if(!noChange.contains(operator) && valInstr!=null && valInstr.storeRegister!=null){
                    ts += " " + valInstr.storeRegister + " ";
                }
                else{
                    ts += "(" + firstOp.valGenerator + ")";
                }
            } else {
                ts += "#" + firstOp.constVal;
            }
        }
        if (secondOp != null) {
            if (secondOp.arraybase != null) {
                ts += secondOp.arraybase;
                ;
            } else if (secondOp.valGenerator != null) {
                Instruction valInstr = idInstructionMap.get(secondOp.valGenerator);
                if(!noChange.contains(operator) && valInstr!=null && valInstr.storeRegister!=null){
                    ts += valInstr.storeRegister;
                }
                else{
                    ts += "(" + secondOp.valGenerator + ")";
                }
            } else {
                ts += "#" + secondOp.constVal;
            }
        }
        if (thirdOp != null) {
            if (thirdOp.arraybase != null) {
                ts += thirdOp.arraybase;
                ;
            } else if (thirdOp.valGenerator != null) {
                Instruction valInstr = idInstructionMap.get(thirdOp.valGenerator);
                if(!noChange.contains(operator) && valInstr!=null && valInstr.storeRegister!=null){
                    ts += valInstr.storeRegister;
                }
                else{
                    ts += "(" + thirdOp.valGenerator + ")";
                }
            } else {
                ts += "#" + thirdOp.constVal;
            }
        }
        if(storeRegister!=null){
            ts += " -- "+storeRegister;
        }
        if(arguments!=null && arguments.size()>0){
            Instruction valInstr = idInstructionMap.get(arguments.get(0));
            if(valInstr!=null && valInstr.storeRegister!=null){
                ts += " - "+valInstr.storeRegister;
            }
            else{
                ts += " - "+"(" +arguments.get(0)+ ")";
            }
            for(int i=1; i<arguments.size(); i++){
                valInstr = idInstructionMap.get(arguments.get(i));
                if(valInstr!=null && valInstr.storeRegister!=null){
                    ts += " - "+valInstr.storeRegister;
                }
                else {
                    ts += ", ( "+arguments.get(i)+ ")";
                }

            }
            ts += " -";
        }
        return ts;
    }
}
