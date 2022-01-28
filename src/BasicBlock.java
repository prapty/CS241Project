import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicBlock {
    List<Instruction> instructions;
    //during assignment, look into map of each parent block for latest value.
    // Join block has multiple parents. Use phi for them. All other blocks have single parents
    Map<Integer, Instruction> valueInstructionMap;
    public BasicBlock() {
        instructions = new ArrayList<>();
        valueInstructionMap = new HashMap<>();
    }
}
