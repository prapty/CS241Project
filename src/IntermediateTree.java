import java.util.*;

public class IntermediateTree {
    BasicBlock start;
    BasicBlock current;
    BasicBlock constants;

    public IntermediateTree() {
        constants = new BasicBlock();
        current=new BasicBlock();
        start=current;
        constants.childBlocks.add(start);
        start.parentBlocks.add(constants);
        start.dominatorBlock = constants;
    }

    public IntermediateTree(boolean copy) {

    }

    IntermediateTree getCopyIrTree(){
        IntermediateTree copyIrTree = new IntermediateTree(true);
        Map<Integer, BasicBlock> copyBlockMap = new HashMap<>();
        Map<Integer, Instruction> copyInstructionMap = new HashMap<>();
        ArrayList<BasicBlock> visited = new ArrayList<>();
        LinkedList<BasicBlock> toVisit = new LinkedList<>();
        copyIrTree.constants = new BasicBlock(constants, copyBlockMap, copyInstructionMap);
        toVisit.add(start);
        while (!toVisit.isEmpty()) {
            BasicBlock current = toVisit.poll();
            visited.add(current);
            BasicBlock copy = new BasicBlock(current, copyBlockMap, copyInstructionMap);
            for (BasicBlock child : current.childBlocks) {
                if (!visited.contains(child)) {
                    toVisit.add(child);
                }
            }
        }
        visited.clear();
        toVisit.clear();

        for(int i=0; i<constants.parentBlocks.size(); i++){
            copyIrTree.constants.parentBlocks.add(copyBlockMap.get(constants.parentBlocks.get(i).IDNum));
        }
        for(int i=0; i<constants.childBlocks.size(); i++){
            copyIrTree.constants.childBlocks.add(copyBlockMap.get(constants.childBlocks.get(i).IDNum));
        }
        if(constants.condBlock!=null){
            copyIrTree.constants.condBlock=copyBlockMap.get(constants.condBlock.IDNum);
        }
        if(constants.dominatorBlock!=null){
            copyIrTree.constants.dominatorBlock=copyBlockMap.get(constants.dominatorBlock.IDNum);
        }

        toVisit.add(start);
        while (!toVisit.isEmpty()) {
            BasicBlock current = toVisit.poll();
            visited.add(current);
            copyIrTree.current=copyBlockMap.get(current.IDNum);
            copyIrTree.current.modifyInstructions(copyInstructionMap);
            if(copyIrTree.start == null){
                copyIrTree.start = copyIrTree.current;
            }
            for(int i=0; i<current.parentBlocks.size(); i++){
                copyIrTree.current.parentBlocks.add(copyBlockMap.get(current.parentBlocks.get(i).IDNum));
            }
            for(int i=0; i<current.childBlocks.size(); i++){
                copyIrTree.current.childBlocks.add(copyBlockMap.get(current.childBlocks.get(i).IDNum));
            }
            if(current.condBlock!=null){
                copyIrTree.current.condBlock=copyBlockMap.get(current.condBlock.IDNum);
            }
            if(current.dominatorBlock!=null){
                copyIrTree.current.dominatorBlock=copyBlockMap.get(current.dominatorBlock.IDNum);
            }
            for (BasicBlock child : current.childBlocks) {
                if (!visited.contains(child)) {
                    toVisit.add(child);
                }
            }
        }

        return copyIrTree;
    }
}
