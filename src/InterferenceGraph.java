
import java.util.*;

public class InterferenceGraph {

    IntermediateTree irTree;


    public InterferenceGraph(IntermediateTree irTree) {
        this.irTree = irTree;
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
        HashMap<Integer, HashSet<Instruction>> blockLiveValues = new HashMap<>(); //live values at end of block i, for if functions
        blockLiveValues.put(current.IDNum, new HashSet<>());

        Operators noLive[] = {Operators.write, Operators.writeNL, Operators.empty, Operators.constant, Operators.store, Operators.end, Operators.bra, Operators.bne, Operators.beq, Operators.ble, Operators.blt, Operators.bge, Operators.bgt, Operators.kill, Operators.cmp};


        while (current != irTree.constants) {
            HashSet<Instruction> liveValues = new HashSet<>();
            if (current.childBlocks.size() > 0) {
                if (current.isIfBlock) {
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                    if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                    }
                } else if (current.isCond && visite.get(current) == null) {
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
                liveValues.remove(currInstr);

                boolean live = true;
                for (Operators op : noLive) {
                    if (currInstr.operator == op) {
                        live = false;
                    }
                }
                if (live && (!currInstr.firstOp.constant || !currInstr.secondOp.constant)) {
                    for (Instruction j : liveValues) {
                        createEdge(graph, j, currInstr);
                    }
                }
                if (currInstr.firstOp != null && !currInstr.firstOp.constant && currInstr.firstOp.id != -1 && currInstr.firstOp.valGenerator != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    liveValues.add(currInstr.firstOp.returnVal);
                }
                if (currInstr.secondOp != null && !currInstr.secondOp.constant && currInstr.secondOp.id != -1 && currInstr.secondOp.valGenerator != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    liveValues.add(currInstr.secondOp.returnVal);
                }
            }
            HashSet<Instruction> blockLives = new HashSet<>();
            blockLives.addAll(liveValues);
            blockLiveValues.put(current.IDNum, blockLives);
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
        }
        if (nodeI == null) {
            nodeI = new GraphNode(i.IDNum, i);
            graph.put(i, nodeI);
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


}
