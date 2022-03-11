import java.util.*;

public class Function {
    boolean isVoid;
    List<Integer>parameters;
    IntermediateTree irTree;
    Map<Integer, BasicBlock> copyBlockMap;

    public Function(boolean isVoid, BasicBlock constants) {
        parameters = new ArrayList<>();
        irTree = new IntermediateTree(constants);
        irTree.start.functionHead = true;
        copyBlockMap = new HashMap<>();
        this.isVoid = isVoid;
        irTree.isVoid = isVoid;
    }
}
