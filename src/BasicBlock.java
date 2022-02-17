import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

public class BasicBlock {
    int IDNum;
    static int instrNum = 1;

    List<Instruction> instructions;
    List<BasicBlock> parentBlocks;
    List<BasicBlock> childBlocks;
    List<Integer> declaredVariables;
    //To keep track of assigned variables for phi functions
    Set<Integer> assignedVariables;
    //during assignment, look into map of each parent block for latest value.
    // Join block has multiple parents. Use phi for them. All other blocks have single parents
    Map<Integer, Instruction> valueInstructionMap;
    Multimap<Instruction, Integer> instructionValueMap;
    Map<Integer, Instruction> nestedValueInstructionMap;

    //Array to maintain linked list of basic operation instructions for common subexpression elimination
    //index: 0-add, 1-sub, 2-mul, 3-div, 4-const, 5-neg
    InstructionLinkedList[] dominatorTree;
    //indicates whether current block is a while block
    boolean whileBlock;
    //indicates the index where phi instruction should be added
    int phiIndex;

    HashSet<BasicBlock> dominatorBlocks;

    public BasicBlock() {
        instructions = new ArrayList<>();
        valueInstructionMap = new HashMap<>();
        nestedValueInstructionMap = new HashMap<>();
        parentBlocks = new ArrayList<>();
        childBlocks = new ArrayList<>();
        declaredVariables = new ArrayList<>();
        assignedVariables = new HashSet<>();
        dominatorTree = new InstructionLinkedList[9];
        instructionValueMap = ArrayListMultimap.create();
        phiIndex = 0;
        whileBlock = false;
        dominatorBlocks = new HashSet<>();
        IDNum = instrNum;
        instrNum++;
    }

    Instruction getLastInstruction() {
        int numInstructions = instructions.size();
        if (numInstructions == 0) {
            return null;
        }
        int last = instructions.size() - 1;
        return instructions.get(last);
    }

    Instruction removeLastInstruction() {
        int last = instructions.size() - 1;
        return instructions.remove(last);
    }

    void setLastInstruction(Instruction instruction) {
        int last = instructions.size() - 1;
        instructions.set(last, instruction);
    }

    Instruction getAnyInstruction(int index) {
        return instructions.get(index);
    }

    void setAnyInstruction(int index, Instruction instruction) {
        instructions.set(index, instruction);
    }
}
