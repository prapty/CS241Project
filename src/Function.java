import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Function {
    boolean isVoid;
    List<Integer> formalParameters;
    IntermediateTree irTree;
    Map<Integer, BasicBlock> copyBlockMap;

    public Function(boolean isVoid) {
        formalParameters = new ArrayList<>();
        irTree = new IntermediateTree();
        copyBlockMap = new HashMap<>();
        this.isVoid = isVoid;
    }
    IntermediateTree getIrTree(){
        IntermediateTree copyIrTree = new IntermediateTree();
        copyIrTree.start = new BasicBlock(irTree.start, copyBlockMap);
        return copyIrTree;
    }
}
