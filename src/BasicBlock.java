import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

public class BasicBlock {
    List<Instruction> instructions;
    List<BasicBlock> parentBlocks;
    List<BasicBlock> childBlocks;
    List<Integer> declaredVariables;
    //To keep track of assigned variables for phi functions
    Set<Integer> assignedVariables;
    //during assignment, look into map of each parent block for latest value.
    // Join block has multiple parents. Use phi for them. All other blocks have single parents
    Map<Integer, Instruction> valueInstructionMap;
    List<Integer>redundantIndex;
    Multimap<Instruction, Integer>instructionUsage;
    Multimap<Instruction, Integer>instructionValueMap;

    //Array to maintain linked list of basic operation instructions for common subexpression elimination
    //index: 0-add, 1-sub, 2-mul, 3-div, 4-const, 5-cmp
    InstructionLinkedList[] dominatorTree;
    public BasicBlock() {
        instructions = new ArrayList<>();
        valueInstructionMap = new HashMap<>();
        parentBlocks=new ArrayList<>();
        childBlocks=new ArrayList<>();
        declaredVariables=new ArrayList<>();
        assignedVariables=new HashSet<>();
        dominatorTree=new InstructionLinkedList[6];
        redundantIndex = new ArrayList<>();
        instructionUsage = ArrayListMultimap.create();
        instructionValueMap = ArrayListMultimap.create();
    }

    Instruction getLastInstruction(){
        int numInstructions=instructions.size();
        if(numInstructions==0){
            return null;
        }
        int last=instructions.size()-1;
        return instructions.get(last);
    }
    Instruction removeLastInstruction(){
        int last=instructions.size()-1;
        return instructions.remove(last);
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
