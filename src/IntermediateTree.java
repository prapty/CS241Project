import java.util.*;

public class IntermediateTree {
    BasicBlock start;
    BasicBlock current;
    BasicBlock constants;
    HashMap<Integer, Function> headToFunc;
    boolean isVoid;
    int numParam;

    public IntermediateTree() {
        constants = new BasicBlock();
        current=new BasicBlock();
        start=current;
        headToFunc = new HashMap<>();

        constants.childBlocks.add(start);
        start.parentBlocks.add(constants);
        start.dominatorBlock = constants;
    }

    public IntermediateTree(BasicBlock constants) {
        this.constants = constants;
        current=new BasicBlock();
        start=current;
        headToFunc = new HashMap<>();

//        this.constants.childBlocks.add(start);
//        start.parentBlocks.add(this.constants);
        start.dominatorBlock = this.constants;
    }
}
