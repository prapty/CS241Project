import java.util.*;

public class Function {
    boolean isVoid;
    List<Integer>parameters;
    IntermediateTree irTree;

    public Function(boolean isVoid, Integer identity) {
        parameters = new ArrayList<>();
        irTree = new IntermediateTree();
        irTree.constants.functionHead = true;
        this.isVoid = isVoid;
        irTree.isVoid = isVoid;
        irTree.constants.functionIdentity = identity;
    }
}
