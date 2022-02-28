import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntermediateTree {
    BasicBlock start;
    BasicBlock current;
    BasicBlock constants;

    public IntermediateTree() {
        constants = new BasicBlock();
        current=new BasicBlock();
        start=current;
        constants.childBlocks.add(start);
        start.parentBlocks.add(constants);
        start.dominatorBlock = constants;
    }

    IntermediateTree getCopyIrTree(){
        IntermediateTree copyIrTree = new IntermediateTree();
        Map<Integer, BasicBlock> copyBlockMap = new HashMap<>();
        copyIrTree.start = new BasicBlock(start, copyBlockMap);
        return copyIrTree;
    }
}
