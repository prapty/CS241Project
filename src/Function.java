import java.util.*;

public class Function {
    boolean isVoid;
    List<Integer>parameters;
    IntermediateTree irTree;

    public Function(boolean isVoid, BasicBlock constants, Integer identity) {
        parameters = new ArrayList<>();
        irTree = new IntermediateTree(constants);
        irTree.start.functionHead = true;
        this.isVoid = isVoid;
        irTree.isVoid = isVoid;
        irTree.start.functionIdentity = identity;
    }
}
