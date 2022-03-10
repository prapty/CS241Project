import java.util.*;

public class Function {
    boolean isVoid;
    ArrayList<Integer>parameters;
    IntermediateTree irTree;
    Map<Integer, BasicBlock> copyBlockMap;

    public Function(boolean isVoid, BasicBlock constants) {
        parameters = new ArrayList<>();
        irTree = new IntermediateTree(constants);
        irTree.start.functionHead = true;
        copyBlockMap = new HashMap<>();
        this.isVoid = isVoid;
    }
    public void replaceParametersWithArguments(Map<Integer, Integer>positionArgumentMap, IntermediateTree copyIrTree){
        ArrayList<BasicBlock> visited = new ArrayList<>();
        LinkedList<BasicBlock> toVisit = new LinkedList<>();
        toVisit.add(copyIrTree.start);
        while (!toVisit.isEmpty()) {
            BasicBlock current = toVisit.poll();
            visited.add(current);
            modifyInstructionsWithArguments(current, positionArgumentMap);
            for (BasicBlock child : current.childBlocks) {
                if (!visited.contains(child)) {
                    toVisit.add(child);
                }
            }
        }
    }
    private void modifyInstructionsWithArguments(BasicBlock block, Map<Integer, Integer>positionArgumentMap){
        for(int i=0; i<block.instructions.size(); i++){
            Instruction instruction = block.instructions.get(i);
            if(instruction.firstOp!=null){
                Integer firstOpVal = instruction.firstOp.valGenerator;
                if(firstOpVal!=null){
                    Integer firstOpReplacement = positionArgumentMap.get(firstOpVal);
                    if(firstOpReplacement != null){
                        instruction.firstOp.valGenerator = firstOpReplacement;
                    }
                }
            }
            if(instruction.secondOp!=null){
                Integer secondOpVal = instruction.secondOp.valGenerator;
                if(secondOpVal!=null){
                    Integer secondOpReplacement = positionArgumentMap.get(secondOpVal);
                    if(secondOpReplacement != null){
                        instruction.secondOp.valGenerator = secondOpReplacement;
                    }
                }
            }
        }
    }
}
