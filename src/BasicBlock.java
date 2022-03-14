import java.util.*;

public class BasicBlock {
    int IDNum;
    static int instrNum = 1;
    Integer functionIdentity;
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
    //index: 0-add, 1-sub, 2-mul, 3-div, 4-const, 5-neg, 6-adda, 7-load, 8
    InstructionLinkedList[] dominatorTree;
    //indicates whether current block is a while block
    boolean isWhileBlock; //if it is part of a while loop
    boolean isCond; // if it is a condblock
    BasicBlock condBlock; // points to the direct upper cond block. if nested, the condblock points to outer condblock
    int nested; // 1 if simple loop, increase by 1 for each nested while

    boolean isIfBlock;
    HashMap<Integer, ArrayIdent> arrayMap;
    boolean nestedBlock;
    boolean functionHead;
    boolean retAdded;
    BasicBlock dominatorBlock;

    public BasicBlock() {
        IDNum = instrNum;
        instrNum++;
        instructions = new ArrayList<>();
        instructionIDs = new ArrayList<>();
        valueInstructionMap = new HashMap<>();
        parentBlocks = new ArrayList<>();
        childBlocks = new ArrayList<>();
        declaredVariables = new ArrayList<>();
        assignedVariables = new HashSet<>();
        dominatorTree = new InstructionLinkedList[9];
        isWhileBlock = false;
        condBlock = null;
        isCond = false;
        nested = 0;
        dominatorBlock = null;
        arrayMap = new HashMap<>();
        isIfBlock = false;
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
