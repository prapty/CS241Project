import java.util.ArrayList;
import java.util.List;

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
}
