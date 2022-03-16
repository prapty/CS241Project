import java.util.*;

public class IntermediateTree {
    BasicBlock start;
    BasicBlock current;
    BasicBlock constants;
    HashMap<Integer, Function> headToFunc;
    boolean isVoid;
    int numParam;
    Instruction assignZeroInstruction;

    public IntermediateTree() {
        constants = new BasicBlock();
        current=new BasicBlock();
        start=current;
        headToFunc = new HashMap<>();
        constants.childBlocks.add(start);
        start.parentBlocks.add(constants);
        start.dominatorBlock = constants;
        Operand zeroOperand = new Operand(true, 0, null, -1);
        assignZeroInstruction = new Instruction(Operators.constant, zeroOperand, zeroOperand);
        assignZeroInstruction.storeRegister = Registers.R0.name();
        InstructionLinkedList node = new InstructionLinkedList();
        node.previous = null;
        node.value = assignZeroInstruction;
        constants.dominatorTree[Operators.constant.ordinal()] = node;
        constants.instructions.add(assignZeroInstruction);
        constants.instructionIDs.add(assignZeroInstruction.IDNum);
    }
}
