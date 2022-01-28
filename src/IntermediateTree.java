import java.util.ArrayList;
import java.util.List;

public class IntermediateTree {
    BasicBlock start;
    List<BasicBlock> parentBlocks;
    List<BasicBlock> childBlocks;
    BasicBlock end;

    public IntermediateTree() {
        parentBlocks=new ArrayList<>();
        childBlocks=new ArrayList<>();
    }
}
