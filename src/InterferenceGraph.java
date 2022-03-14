
import java.util.*;

public class InterferenceGraph {

    IntermediateTree irTree;
    PriorityQueue<GraphNode>sortedNodes;
    Map<Integer, Instruction>idInstructionMap;
    Map<Integer, BasicBlock>idBlockMap;
    List<Operators>noLive;

    public InterferenceGraph(IntermediateTree irTree) {
        this.irTree = irTree;
        Comparator<GraphNode>graphNodComparator = new GraphNodeComparator();
        sortedNodes = new PriorityQueue<>(graphNodComparator);
        idInstructionMap = new HashMap<>();
        idBlockMap = new HashMap<>();
        idBlockMap.put(irTree.constants.IDNum, irTree.constants);
        for(Instruction instruction: irTree.constants.instructions){
            idInstructionMap.put(instruction.IDNum, instruction);
        }
        noLive = new ArrayList<>(Arrays.asList(Operators.write, Operators.writeNL, Operators.empty, Operators.constant, Operators.store, Operators.end, Operators.bra, Operators.bne, Operators.beq, Operators.ble, Operators.blt, Operators.bge, Operators.bgt, Operators.kill, Operators.cmp, Operators.push, Operators.pushUsedRegisters, Operators.popUsedRegisters, Operators.jsr));
    }

    public HashMap<Instruction, GraphNode> getGraph() {
        HashMap<Instruction, GraphNode> graph = new HashMap<>();
        BasicBlock current = irTree.current;

        Comparator<BasicBlock> comparator = new BasicBlockComparator();
        PriorityQueue<BasicBlock> toVisit = new PriorityQueue<>(comparator);
//        LinkedList<BasicBlock> toVisit = new LinkedList<>();
        HashMap<BasicBlock, Integer> visite = new HashMap<>();
//        toVisit.add(current);
        visite.put(current, 1);
        idBlockMap.put(current.IDNum, current);
        HashMap<Integer, HashSet<Instruction>> blockLiveValues = new HashMap<>(); //live values at end of block i, for if functions
        HashMap<Integer, HashSet<Instruction>> thenLiveValues = new HashMap<>();
        HashMap<Integer, HashSet<Instruction>> elseLiveValues = new HashMap<>();
        blockLiveValues.put(current.IDNum, new HashSet<>());

        //Operators noLive[] = {Operators.write, Operators.writeNL, Operators.empty, Operators.constant, Operators.store, Operators.end, Operators.bra, Operators.bne, Operators.beq, Operators.ble, Operators.blt, Operators.bge, Operators.bgt, Operators.kill, Operators.cmp, Operators.push, Operators.pushUsedRegisters, Operators.popUsedRegisters, Operators.jsr};


        while (current != irTree.constants) {
            HashSet<Instruction> liveValues = new HashSet<>();
            HashSet<Instruction> thenValues = new HashSet<>();
            HashSet<Instruction> elseValues = new HashSet<>();
            if (current.childBlocks.size() > 0) {
                if (current.ifDiamond==IfDiamond.ifBlock) {
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                    if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                    }
                }
                else if (current.ifDiamond==IfDiamond.thenBlock) {
                    if (thenLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(thenLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                }
                else if (current.ifDiamond==IfDiamond.elseBlock) {
                    if (elseLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(elseLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                }
                else if (current.isCond && visite.get(current) == null) {
                    if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                    }
                } else {
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                }
            }
            for (int i = current.instructions.size() - 1; i >= 0; i--) {
                Instruction currInstr = current.instructions.get(i);
                boolean joinPi = false;
                if(irTree.current.ifDiamond==IfDiamond.joinBlock && currInstr.operator==Operators.phi){
                    joinPi = true;
                }
                idInstructionMap.put(currInstr.IDNum, currInstr);
                liveValues.remove(currInstr);
                thenValues.remove(currInstr);
                elseValues.remove(currInstr);

                boolean live = true;
                if(noLive.contains(currInstr.operator)){
                    live = false;
                }
                boolean notConstant = true;
                if((currInstr.firstOp!=null && currInstr.firstOp.constant)||(currInstr.secondOp!=null && currInstr.secondOp.constant)){
                    notConstant = false;
                }
                if (live && notConstant) {
                    for (Instruction j : liveValues) {
                        if(j!=null){
                            createEdge(graph, j, currInstr);
                        }
                    }
                    for (Instruction j : thenValues) {
                        if(j!=null){
                            createEdge(graph, j, currInstr);
                        }
                    }
                    for (Instruction j : elseValues) {
                        if(j!=null){
                            createEdge(graph, j, currInstr);
                        }
                    }
                }
                if (currInstr.firstOp != null && !currInstr.firstOp.constant && currInstr.firstOp.id != -1 && currInstr.firstOp.valGenerator != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    if(joinPi){
                        thenValues.add(currInstr.firstOp.returnVal);
                    }
                    else{
                        liveValues.add(currInstr.firstOp.returnVal);
                    }
                }
                if (currInstr.secondOp != null && !currInstr.secondOp.constant && currInstr.secondOp.id != -1 && currInstr.secondOp.valGenerator != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    if(joinPi){
                        elseValues.add(currInstr.secondOp.returnVal);
                    }
                    else{
                        liveValues.add(currInstr.secondOp.returnVal);
                    }
                }
            }
            HashSet<Instruction> blockLives = new HashSet<>();
            blockLives.addAll(liveValues);
            blockLiveValues.put(current.IDNum, blockLives);
            if(current.ifDiamond==IfDiamond.joinBlock){
                thenLiveValues.put(current.IDNum, thenValues);
                elseLiveValues.put(current.IDNum, elseValues);
            }
            for (BasicBlock b : current.parentBlocks) {
                if (!visited(b, visite)) {
                    toVisit.add(b);
                }
            }
            current = toVisit.poll();
        }
        updateCost(graph);
        return graph;
    }

    private void updateCost(HashMap<Instruction,GraphNode> graph) {
        for(GraphNode gn : graph.values()){
            gn.instruction.cost = gn.instruction.cost/gn.neighbors.size();
        }
    }

    private void createEdge(HashMap<Instruction, GraphNode> graph, Instruction j, Instruction i) {
        GraphNode nodeJ = graph.get(j);
        GraphNode nodeI = graph.get(i);
        if (nodeJ == null) {
            nodeJ = new GraphNode(j.IDNum, j);
            graph.put(j, nodeJ);
            sortedNodes.add(nodeJ);
        }
        if (nodeI == null) {
            nodeI = new GraphNode(i.IDNum, i);
            graph.put(i, nodeI);
            sortedNodes.add(nodeI);
        }
        nodeI.neighbors.add(nodeJ);
        nodeJ.neighbors.add(nodeI);
    }

    private boolean visited(BasicBlock block, HashMap<BasicBlock, Integer> list) {
        if (list.get(block) == null) {
            list.put(block, 1);
            return false;
        } else if (list.get(block) == 1 && block.isCond) {
            list.put(block, 2);
            return false;
        }
        return true;
    }

    public void colorGraph(HashMap<Instruction, GraphNode>graph){
        int numColor = 1;
        Set<Integer>excludeColorSet = new HashSet<>(Arrays.asList(0, 27, 28, 29, 30, 31));
        Map<String, String>registerNameNumMap = new HashMap<>();
        registerNameNumMap.put(Registers.SP.name(), "R29");
        registerNameNumMap.put(Registers.FP.name(), "R28");
        registerNameNumMap.put(Registers.RV.name(), "R27");
        registerNameNumMap.put(Registers.R31.name(), "R31");
        String register = "R";
        while(!sortedNodes.isEmpty()){
            GraphNode node = sortedNodes.poll();
            if(node.instruction.operator==Operators.phi){
                GraphNode cluster = new GraphNode(node.instrID, node.instruction);
                cluster.neighbors.addAll(node.neighbors);
                cluster.members.add(node);
                //sortedNodes.remove(node);
                Instruction firstInstr = idInstructionMap.get(node.instruction.firstOp.valGenerator);
                if(firstInstr!=null){
                    GraphNode firstNode = graph.get(firstInstr);
                    if(firstNode!=null){

                    }
                }

                Instruction secondInstr = idInstructionMap.get(node.instruction.secondOp.valGenerator);
                GraphNode secondNode = graph.get(secondInstr);
            }
            boolean colored = false;
            if(node.instruction.storeRegister==null){
                for(int i=1; i<=numColor; i++){
                    if(isColorAvailable(node, i)){
                        colored = true;
                        node.instruction.storeRegister=register+i;
                        break;
                    }
                }
                if(!colored){
                    numColor++;
                    while(excludeColorSet.contains(numColor)){
                        numColor++;
                    }
                    node.instruction.storeRegister=register+numColor;
                }
            }
            else{
                node.instruction.storeRegister = registerNameNumMap.get(node.instruction.storeRegister);
            }
        }
        String defaultRegister = "R1";
        for(Integer id:idInstructionMap.keySet()){
            Instruction instruction = idInstructionMap.get(id);
            if(!noLive.contains(instruction.operator) && instruction.storeRegister==null){
                instruction.storeRegister = defaultRegister;
            }
        }
    }
    private boolean isColorAvailable(GraphNode node, int color){
        String register = "R"+color;
        for(GraphNode neighbour: node.neighbors){
            if(neighbour.instruction.storeRegister!=null && neighbour.instruction.storeRegister.equals(register)){
                return false;
            }
        }
        return true;
    }
}
