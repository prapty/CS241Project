import javax.swing.*;
import java.util.*;

public class BasicBlock {
    int IDNum;
    static int instrNum = 1;

    List<Instruction> instructions;
    List<BasicBlock> parentBlocks;
    List<BasicBlock> childBlocks;
    List<Integer> declaredVariables;
    List<Integer> instructionIDs;
    //To keep track of assigned variables for phi functions
    Set<Integer> assignedVariables;
    //during assignment, look into map of each parent block for latest value.
    // Join block has multiple parents. Use phi for them. All other blocks have single parents
    Map<Integer, Instruction> valueInstructionMap;

    //Array to maintain linked list of basic operation instructions for common subexpression elimination
    //index: 0-add, 1-sub, 2-mul, 3-div, 4-const, 5-neg, 6-adda, 7-load
    InstructionLinkedList[] dominatorTree;
    //indicates whether current block is a while block
    boolean whileBlock; //if it is part of a while loop
    boolean isCond; // if it is a condblock
    BasicBlock condBlock; // points to the direct upper cond block. if nested, the condblock points to outer condblock
    int nested; // 1 if simple loop, increase by 1 for each nested while


    //    List<ArrayIdent> ArrayIdentifiers;
    HashMap<Integer, ArrayIdent> arrayMap;

    boolean makeDuplicate;
    boolean nestedBlock;
    //indicates the index where phi instruction should be added
    int phiIndex;

    //variables for creating dot file, vis/visbranch to not go through same block twice
    BasicBlock dominatorBlock;
    boolean vis;
    boolean visbranch;
    Operand returnValue;

    public BasicBlock() {
        IDNum = instrNum;
        instrNum++;
        vis = false;
        visbranch = false;
        instructions = new ArrayList<>();
        instructionIDs = new ArrayList<>();
        valueInstructionMap = new HashMap<>();
        parentBlocks = new ArrayList<>();
        childBlocks = new ArrayList<>();
        declaredVariables = new ArrayList<>();
        assignedVariables = new HashSet<>();
        dominatorTree = new InstructionLinkedList[9];
        phiIndex = 0;
        whileBlock = false;
        condBlock = null;
        isCond = false;
        nested = 0;
        dominatorBlock = null;
//        ArrayIdentifiers = new ArrayList<>();
        arrayMap = new HashMap<>();
    }

    public BasicBlock(BasicBlock block, Map<Integer, BasicBlock> copyMap, Map<Integer, Instruction>copyInstructionMap) {
        instructions = new ArrayList<>();
        instructionIDs = new ArrayList<>();
        for(int i=0; i<block.instructions.size(); i++){
            //instructions.add(new Instruction(block.instructions.get(i), copyInstructionMap));
            instructions.add(new Instruction(block.instructions.get(i)));
            instructionIDs.add(instructions.get(i).IDNum);
            copyInstructionMap.put(block.instructions.get(i).IDNum, instructions.get(i));
        }

        valueInstructionMap = new HashMap<>();
        for(int id: block.valueInstructionMap.keySet()){
            valueInstructionMap.put(id, copyInstructionMap.get(block.valueInstructionMap.get(id).IDNum));
        }

        if(returnValue!=null){
            returnValue.valGenerator = copyInstructionMap.get(block.returnValue.valGenerator).IDNum;
            returnValue.returnVal = copyInstructionMap.get(block.returnValue.valGenerator);
        }

        parentBlocks = new ArrayList<>();
        childBlocks = new ArrayList<>();

        declaredVariables = new ArrayList<>();
        declaredVariables.addAll(block.declaredVariables);

        assignedVariables = new HashSet<>();
        assignedVariables.addAll(block.assignedVariables);

        dominatorTree = block.dominatorTree.clone();
        phiIndex = block.phiIndex;
        whileBlock = block.whileBlock;
        isCond = block.isCond;
        nested = block.nested;
        dominatorBlock = block.dominatorBlock;
        IDNum = instrNum;
        instrNum++;
        vis = block.vis;
        visbranch = block.visbranch;
        copyMap.put(block.IDNum, this);
    }

    public void modifyInstructions(Map<Integer, Instruction>copyInstructionMap){
        for(int i=0; i<instructions.size(); i++){
            instructions.get(i).modifyInstruction(copyInstructionMap);
        }
    }

    Instruction getLastInstruction() {
        int numInstructions = instructions.size();
        if (numInstructions == 0) {
            return null;
        }
        int last = instructions.size() - 1;
        return instructions.get(last);
    }

    void setLastInstruction(Instruction instruction) {
        int last = instructions.size() - 1;
        instructions.set(last, instruction);
    }
}
