import java.util.*;

public class BasicBlock {
    List<Instruction> instructions;
    List<BasicBlock> parentBlocks;
    List<BasicBlock> childBlocks;
    //To keep track of assigned variables for phi functions
    Set<Integer> assignedVariables;
    //during assignment, look into map of each parent block for latest value.
    // Join block has multiple parents. Use phi for them. All other blocks have single parents
    Map<Integer, Instruction> valueInstructionMap;

    //Array to maintain linked list of basic operation instructions for common subexpression elimination
    //index: 0-add, 1-sub, 2-mul, 3-div, 4-const
    InstructionLinkedList[] dominatorTree;
    public BasicBlock() {
        instructions = new ArrayList<>();
        valueInstructionMap = new HashMap<>();
        parentBlocks=new ArrayList<>();
        childBlocks=new ArrayList<>();
        assignedVariables=new HashSet<>();
        dominatorTree=new InstructionLinkedList[5];
    }
    Instruction getLastInstruction(){
        int numInstructions=instructions.size();
        if(numInstructions==0){
            return null;
        }
        int last=instructions.size()-1;
        return instructions.get(last);
    }
    void setLastInstruction(Instruction instruction){
        int last=instructions.size()-1;
        instructions.set(last, instruction);
    }
    Instruction getAnyInstruction(int index){
        return instructions.get(index);
    }
    void setAnyInstruction(int index, Instruction instruction){
        instructions.set(index, instruction);
    }
}
