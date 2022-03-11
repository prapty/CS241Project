import java.util.*;

public class IntermediateTree {
    BasicBlock start;
    BasicBlock current;
    BasicBlock constants;
    boolean isVoid;

    public IntermediateTree() {
        constants = new BasicBlock();
        current=new BasicBlock();
        start=current;

        constants.childBlocks.add(start);
        start.parentBlocks.add(constants);
        start.dominatorBlock = constants;
    }

    public IntermediateTree(BasicBlock constants) {
        this.constants = constants;
        current=new BasicBlock();
        start=current;

//        this.constants.childBlocks.add(start);
//        start.parentBlocks.add(this.constants);
        start.dominatorBlock = this.constants;
    }
}
