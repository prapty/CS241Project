import java.util.ArrayList;
import java.util.List;

public class IntermediateTree {
    BasicBlock start;
    BasicBlock current;

    public IntermediateTree() {
        current=new BasicBlock();
        start=current;
    }
}
