
import java.io.IOException;
import java.util.*;

public class InterferenceGraph {

    IntermediateTree irTree;
    HashMap<Function, InterferenceGraph> functionsInterferenceGraph;
    Map<Integer, Instruction> idInstructionMap;
    Map<Integer, BasicBlock> idBlockMap;
    List<Operators> noLive;

    public InterferenceGraph(IntermediateTree irTree) {
        this.irTree = irTree;
        idInstructionMap = new HashMap<>();
        idBlockMap = new HashMap<>();
        idBlockMap.put(irTree.constants.IDNum, irTree.constants);
        for (Instruction instruction : irTree.constants.instructions) {
            idInstructionMap.put(instruction.IDNum, instruction);
        }
        functionsInterferenceGraph = new HashMap<>();
        noLive = new ArrayList<>(Arrays.asList(Operators.write, Operators.writeNL, Operators.nop, Operators.store, Operators.end, Operators.bra, Operators.bne, Operators.beq, Operators.ble, Operators.blt, Operators.bge, Operators.bgt, Operators.kill, Operators.cmp, Operators.push, Operators.pushUsedRegisters, Operators.popUsedRegisters, Operators.call, Operators.ret));
    }

    public HashMap<Instruction, GraphNode> getGraph() {
        HashMap<Instruction, GraphNode> graph = new HashMap<>();
        BasicBlock current = irTree.current;

        Comparator<BasicBlock> comparator = new BasicBlockComparator();
        PriorityQueue<BasicBlock> toVisit = new PriorityQueue<>(comparator);
        HashMap<BasicBlock, Integer> visite = new HashMap<>();
        visite.put(current, 1);
        idBlockMap.put(current.IDNum, current);
        HashMap<Integer, HashSet<Instruction>> blockLiveValues = new HashMap<>(); //live values at end of block i, for if functions
        HashMap<Integer, HashSet<Instruction>> thenLiveValues = new HashMap<>();
        HashMap<Integer, HashSet<Instruction>> elseLiveValues = new HashMap<>();
        blockLiveValues.put(current.IDNum, new HashSet<>());

        // while (current != irTree.constants) {
        while (current != null) {
            idBlockMap.put(current.IDNum, current);
            HashSet<Instruction> liveValues = new HashSet<>();
            HashSet<Instruction> thenValues = new HashSet<>();
            HashSet<Instruction> elseValues = new HashSet<>();
            if (current.childBlocks != null && current.childBlocks.size() > 0)
            {
                if (current.childBlocks.get(0).functionHead) {
                    if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                    }
//                    Function f = irTree.headToFunc.get(current.childBlocks.get(0).IDNum);
//                    InterferenceGraph functionIG = new InterferenceGraph(f.irTree);
//                    functionsInterferenceGraph.putIfAbsent(f, functionIG);
                }
                else {
                    if (current.ifDiamond == IfDiamond.ifBlock) {
                        if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                            liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                        }
                        if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                            liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                        }
                    } else if (current.ifDiamond == IfDiamond.thenBlock) {
                        if (thenLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                            liveValues.addAll(thenLiveValues.get(current.childBlocks.get(0).IDNum));
                        }
                        if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                            liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                        }
                    } else if (current.ifDiamond == IfDiamond.elseBlock) {
                        if (elseLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                            liveValues.addAll(elseLiveValues.get(current.childBlocks.get(0).IDNum));
                        }
                        if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                            liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                        }
                    } else if (current.isCond && visite.get(current) == 2) {
                        if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                            liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                        }
                    } else {
                        BasicBlock child = current.childBlocks.get(0);
                        if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                            liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                        }
                        if (child.parentBlocks.size() > 1) {
                            //child is a join block
                            int parentIndex = child.parentBlocks.indexOf(current);
                            if (parentIndex == 0) {
                                //current is then block
                                if (thenLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                                    liveValues.addAll(thenLiveValues.get(current.childBlocks.get(0).IDNum));
                                }
                            } else {
                                //current is else block
                                if (elseLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                                    liveValues.addAll(elseLiveValues.get(current.childBlocks.get(0).IDNum));
                                }
                            }
                        }
                    }
                }
            }
            for (int i = current.instructions.size() - 1; i >= 0; i--) {
                Instruction currInstr = current.instructions.get(i);
                idInstructionMap.put(currInstr.IDNum, currInstr);
                liveValues.remove(currInstr);
                thenValues.remove(currInstr);
                elseValues.remove(currInstr);

                boolean live = true;
                if (noLive.contains(currInstr.operator)) {
                    live = false;
                }
                if (live) {
                    for (Instruction j : liveValues) {
                        if (j != null) {
                            createEdge(graph, j, currInstr);
                        }
                    }
                    for (Instruction j : thenValues) {
                        if (j != null) {
                            createEdge(graph, j, currInstr);
                        }
                    }
                    for (Instruction j : elseValues) {
                        if (j != null) {
                            createEdge(graph, j, currInstr);
                        }
                    }
                }
                if (currInstr.firstOp != null && currInstr.firstOp.returnVal != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    if (currInstr.operator == Operators.phi) {
                        thenValues.add(currInstr.firstOp.returnVal);
                    } else {
                        liveValues.add(currInstr.firstOp.returnVal);
                    }
                }
                if (currInstr.secondOp != null && currInstr.secondOp.returnVal != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.secondOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.secondOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    if (currInstr.operator == Operators.phi) {
                        elseValues.add(currInstr.secondOp.returnVal);
                    } else {
                        liveValues.add(currInstr.secondOp.returnVal);
                    }
                }
                if (currInstr.thirdOp != null && currInstr.thirdOp.returnVal != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.thirdOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.thirdOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    liveValues.add(currInstr.thirdOp.returnVal);
                }
            }
            HashSet<Instruction> blockLives = new HashSet<>();
            blockLives.addAll(liveValues);
            blockLiveValues.put(current.IDNum, blockLives);
            thenLiveValues.put(current.IDNum, thenValues);
            elseLiveValues.put(current.IDNum, elseValues);
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

    public HashMap<Instruction, GraphNode> getFunctionGraph(Function f) {
        HashMap<Instruction, GraphNode> graph = new HashMap<>();
        BasicBlock current = f.irTree.current;

        Comparator<BasicBlock> comparator = new BasicBlockComparator();
        PriorityQueue<BasicBlock> toVisit = new PriorityQueue<>(comparator);
        HashMap<BasicBlock, Integer> visite = new HashMap<>();
        visite.put(current, 1);
        idBlockMap.put(current.IDNum, current);
        HashMap<Integer, HashSet<Instruction>> blockLiveValues = new HashMap<>(); //live values at end of block i, for if functions
        HashMap<Integer, HashSet<Instruction>> thenLiveValues = new HashMap<>();
        HashMap<Integer, HashSet<Instruction>> elseLiveValues = new HashMap<>();
        blockLiveValues.put(current.IDNum, new HashSet<>());

        int maxIDblockNum = f.irTree.current.IDNum;

        while (current.IDNum >= f.irTree.constants.IDNum && current.IDNum <= maxIDblockNum) {
            HashSet<Instruction> liveValues = new HashSet<>();
            HashSet<Instruction> thenValues = new HashSet<>();
            HashSet<Instruction> elseValues = new HashSet<>();
            if (current.childBlocks.size() > 0) {
                if (current.ifDiamond == IfDiamond.ifBlock) {
                    if (visite.get(current.childBlocks.get(0)) == null) {
                        toVisit.add(current.childBlocks.get(0));
                        visite.put(current.childBlocks.get(0), 1);
                        toVisit.add(current);
                        current = toVisit.poll();
                        continue;
                    }
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                    if (visite.get(current.childBlocks.get(1)) == null) {
                        toVisit.add(current.childBlocks.get(1));
                        visite.put(current.childBlocks.get(1), 1);
                        toVisit.add(current);
                        current = toVisit.poll();
                        continue;
                    }
                    if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                    }
                } else if (current.ifDiamond == IfDiamond.thenBlock) {
                    if (visite.get(current.childBlocks.get(0)) == null) {
                        toVisit.add(current.childBlocks.get(0));
                        visite.put(current.childBlocks.get(0), 1);
                        toVisit.add(current);
                        current = toVisit.poll();
                        continue;
                    }
                    if (thenLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(thenLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                } else if (current.ifDiamond == IfDiamond.elseBlock) {
                    if (visite.get(current.childBlocks.get(0)) == null) {
                        toVisit.add(current.childBlocks.get(0));
                        visite.put(current.childBlocks.get(0), 1);
                        toVisit.add(current);
                        current = toVisit.poll();
                        continue;
                    }
                    if (elseLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(elseLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                } else if (current.isCond && visite.get(current) == 2) {
                    if (visite.get(current.childBlocks.get(1)) == null) {
                        toVisit.add(current.childBlocks.get(1));
                        visite.put(current.childBlocks.get(1), 1);
                        toVisit.add(current);
                        current = toVisit.poll();
                        continue;
                    }
                    if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                    }
                } else if (current.childBlocks.get(0).functionHead) {
                    if (visite.get(current.childBlocks.get(1)) == null) {
                        toVisit.add(current.childBlocks.get(1));
                        visite.put(current.childBlocks.get(1), 1);
                        toVisit.add(current);
                        current = toVisit.poll();
                        continue;
                    }
                    if (blockLiveValues.get(current.childBlocks.get(1).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(1).IDNum));
                    }
                    Function fc = irTree.headToFunc.get(current.childBlocks.get(0).IDNum);
                    InterferenceGraph functionIG = new InterferenceGraph(fc.irTree);
                    functionsInterferenceGraph.putIfAbsent(fc, functionIG);
                } else {
                    if (visite.get(current.childBlocks.get(0)) == null) {
                        toVisit.add(current.childBlocks.get(0));
                        visite.put(current.childBlocks.get(0), 1);
                        toVisit.add(current);
                        current = toVisit.poll();
                        continue;
                    }
                    if (blockLiveValues.get(current.childBlocks.get(0).IDNum) != null) {
                        liveValues.addAll(blockLiveValues.get(current.childBlocks.get(0).IDNum));
                    }
                }
            }
            for (int i = current.instructions.size() - 1; i >= 0; i--) {
                Instruction currInstr = current.instructions.get(i);
                idInstructionMap.put(currInstr.IDNum, currInstr);
                liveValues.remove(currInstr);

                boolean live = true;
                if (noLive.contains(currInstr.operator)) {
                    live = false;
                }
                if (live) {
                    for (Instruction j : liveValues) {
                        if (j != null) {
                            createEdge(graph, j, currInstr);
                        }
                    }
                }
                if (currInstr.firstOp != null && currInstr.firstOp.returnVal != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.firstOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    if (currInstr.operator == Operators.phi) {
                        thenValues.add(currInstr.firstOp.returnVal);
                    } else {
                        liveValues.add(currInstr.firstOp.returnVal);
                    }
                }
                if (currInstr.secondOp != null && currInstr.secondOp.returnVal != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.secondOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.secondOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    if (currInstr.operator == Operators.phi) {
                        elseValues.add(currInstr.secondOp.returnVal);
                    } else {
                        liveValues.add(currInstr.secondOp.returnVal);
                    }
                }
                if (currInstr.thirdOp != null && currInstr.thirdOp.returnVal != null && currInstr.operator.toString().charAt(0) != 'b') {
                    if (current.isCond) {
                        currInstr.thirdOp.returnVal.cost += Math.pow(10, current.nested - 1);
                    } else {
                        currInstr.thirdOp.returnVal.cost += Math.pow(10, current.nested);
                    }
                    liveValues.add(currInstr.thirdOp.returnVal);
                }
            }
            HashSet<Instruction> blockLives = new HashSet<>();
            blockLives.addAll(liveValues);
            blockLiveValues.put(current.IDNum, blockLives);
            thenLiveValues.put(current.IDNum, thenValues);
            elseLiveValues.put(current.IDNum, elseValues);
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

    private void updateCost(HashMap<Instruction, GraphNode> graph) {
        for (GraphNode gn : graph.values()) {
            gn.instruction.cost = gn.instruction.cost / gn.neighbors.size();
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

    public void colorGraph(HashMap<Instruction, GraphNode> graph) {
        int numColor = 1;
        Set<Integer> excludeColorSet = new HashSet<>(Arrays.asList(0, 27, 28, 29, 30, 31));
        Map<String, String> registerNameNumMap = new HashMap<>();
        registerNameNumMap.put(Registers.R0.name(), "R0");
        registerNameNumMap.put(Registers.SP.name(), "R29");
        registerNameNumMap.put(Registers.FP.name(), "R28");
        registerNameNumMap.put(Registers.RV.name(), "R27");
        registerNameNumMap.put(Registers.R31.name(), "R31");
        String register = "R";
        PriorityQueue<GraphNode> sortedNodes = buildPriorityQue(graph);
        while (!sortedNodes.isEmpty()) {
            GraphNode node = sortedNodes.poll();
            boolean colored = false;
            String allocatedRegister = null;
            if (node.instruction.storeRegister == null) {
                for (int i = 1; i <= numColor; i++) {
                    if (isColorAvailable(node, i)) {
                        colored = true;
                        allocatedRegister = register + i;
                        break;
                    }
                }
                if (!colored) {
                    numColor++;
                    while (excludeColorSet.contains(numColor)) {
                        numColor++;
                    }
                    allocatedRegister = register + numColor;
                }
                node.instruction.storeRegister = allocatedRegister;
                if (node.members != null && node.members.size() > 0) {
                    for (GraphNode member : node.members) {
                        member.instruction.storeRegister = allocatedRegister;
                    }
                }
            } else {
                node.instruction.storeRegister = registerNameNumMap.get(node.instruction.storeRegister);
            }
        }
        String coloredFileName = "unresolvedPhiDot.dot";
        Dot coloredDot = new Dot(coloredFileName);
        coloredDot.idInstructionMap = idInstructionMap;
        try {
            coloredDot.makeDotGraph(irTree);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String defaultRegister = "R1";
        for (Integer id : idInstructionMap.keySet()) {
            Instruction instruction = idInstructionMap.get(id);

            if (instruction.firstOp != null && instruction.firstOp.valGenerator == null && instruction.firstOp.arraybase != null) {
                if (registerNameNumMap.get(instruction.firstOp.arraybase) != null) {
                    instruction.firstOp.arraybase = registerNameNumMap.get(instruction.firstOp.arraybase);
                }
            }
            if (instruction.secondOp != null && instruction.secondOp.valGenerator == null && instruction.secondOp.arraybase != null) {
                if (registerNameNumMap.get(instruction.secondOp.arraybase) != null) {
                    instruction.secondOp.arraybase = registerNameNumMap.get(instruction.secondOp.arraybase);
                }
            }
            if (instruction.thirdOp != null && instruction.thirdOp.valGenerator == null && instruction.thirdOp.arraybase != null) {
                if (registerNameNumMap.get(instruction.thirdOp.arraybase) != null) {
                    instruction.thirdOp.arraybase = registerNameNumMap.get(instruction.thirdOp.arraybase);
                }
            }
            if (!noLive.contains(instruction.operator) && instruction.storeRegister == null) {
                instruction.storeRegister = defaultRegister;
            }
        }
        for (Integer id : idInstructionMap.keySet()) {
            Instruction instruction = idInstructionMap.get(id);
            if (instruction.operator == Operators.phi) {
                resolvePhi(instruction);
            }
        }
    }

    private void resolvePhi(Instruction instruction) {
        Instruction leftInstr = idInstructionMap.get(instruction.firstOp.valGenerator);
        Instruction rightInstr = idInstructionMap.get(instruction.secondOp.valGenerator);
        int phiBlockId = 0;
        for (int id : idBlockMap.keySet()) {
            BasicBlock block = idBlockMap.get(id);
            if (block.instructionIDs.contains(instruction.IDNum)) {
                phiBlockId = block.IDNum;
                break;
            }
        }

        BasicBlock phiBlock = idBlockMap.get(phiBlockId);

        if (!instruction.storeRegister.equals(leftInstr.storeRegister)) {
            //need to add move for left
            Operand leftOp = new Operand(leftInstr.storeRegister);
            Instruction moveInstr = new Instruction(Operators.move, leftOp);
            moveInstr.storeRegister = instruction.storeRegister;

            BasicBlock leftBlock = phiBlock.parentBlocks.get(0);
            int index = leftBlock.instructions.indexOf(leftInstr);
            if (index != -1) {
                leftBlock.instructions.add(index + 1, moveInstr);
                leftBlock.instructionIDs.add(index + 1, moveInstr.IDNum);
            } else {
                leftBlock.instructions.add(moveInstr);
                leftBlock.instructionIDs.add(moveInstr.IDNum);
                if (leftBlock.instructions.get(leftBlock.instructions.size() - 2).toString().charAt(0) == 'b') {
                    Collections.swap(leftBlock.instructions, leftBlock.instructions.size() - 1, leftBlock.instructions.size() - 2);
                }
            }
        }

        if (!instruction.storeRegister.equals(rightInstr.storeRegister)) {
            //need to add move for right
            Operand rightOp = new Operand(rightInstr.storeRegister);
            Instruction moveInstr = new Instruction(Operators.move, rightOp);
            moveInstr.storeRegister = instruction.storeRegister;
            BasicBlock rightBlock = phiBlock.parentBlocks.get(1);
            int index = rightBlock.instructions.indexOf(rightInstr);
            if (index != -1) {
                rightBlock.instructions.add(index + 1, moveInstr);
                rightBlock.instructionIDs.add(index + 1, moveInstr.IDNum);
            } else {
                rightBlock.instructions.add(moveInstr);
                rightBlock.instructionIDs.add(moveInstr.IDNum);
                if (rightBlock.instructions.get(rightBlock.instructions.size() - 2).toString().charAt(0) == 'b') {
                    Collections.swap(rightBlock.instructions, rightBlock.instructions.size() - 1, rightBlock.instructions.size() - 2);
                }
            }
        }
        //remove phi
        phiBlock.instructions.remove(instruction);
        int idIndex = phiBlock.instructionIDs.indexOf(instruction.IDNum);
        phiBlock.instructionIDs.remove(idIndex);
    }

    private PriorityQueue<GraphNode> buildPriorityQue(HashMap<Instruction, GraphNode> graph) {
        Comparator<GraphNode> graphNodComparator = new GraphNodeComparator();
        PriorityQueue<GraphNode> sortedNodes = new PriorityQueue<>(graphNodComparator);
        for (Instruction instruction : graph.keySet()) {
            GraphNode node = graph.get(instruction);
            if (node.clusterAdded) {
                continue;
            }
            if (instruction.operator == Operators.phi) {
                GraphNode cluster = new GraphNode(instruction.IDNum, instruction);
                cluster = buildCluster(graph, instruction, cluster, sortedNodes);
                sortedNodes.add(cluster);
            } else if (!sortedNodes.contains(node)) {
                sortedNodes.add(node);
            }
        }
        return sortedNodes;
    }

    private GraphNode buildCluster(HashMap<Instruction, GraphNode> graph, Instruction instruction, GraphNode cluster, PriorityQueue<GraphNode> sortedNodes) {
        GraphNode phiNode = graph.get(instruction);
        GraphNode leftNode, rightNode;
        cluster.neighbors.addAll(phiNode.neighbors);
        cluster.members.add(phiNode);
        sortedNodes.remove(phiNode);
        phiNode.clusterAdded = true;
        Instruction leftInstr = idInstructionMap.get(instruction.firstOp.valGenerator);
        if (leftInstr != null) {
            leftNode = graph.get(leftInstr);
            if (leftNode != null) {
                if (leftInstr.operator != Operators.phi) {
                    if (!leftNode.neighbors.contains(phiNode)) {
                        cluster.members.add(leftNode);
                        leftNode.clusterAdded = true;
                        cluster.neighbors.addAll(leftNode.neighbors);
                        sortedNodes.remove(leftNode);
                    }
                } else {
                    if (leftNode.members.size() == 0) {
                        //cluster not processed
                        leftNode = buildCluster(graph, leftInstr, leftNode, sortedNodes);
                    }
                    boolean interfere = false;
                    for (GraphNode node : leftNode.members) {
                        for (GraphNode node1 : cluster.members) {
                            if (node.neighbors.contains(node1)) {
                                interfere = true;
                            }
                        }
                    }
                    if (!interfere) {
                        for (GraphNode node : leftNode.members) {
                            cluster.members.add(node);
                            node.clusterAdded = true;
                            cluster.neighbors.addAll(node.neighbors);
                        }
                        sortedNodes.remove(leftNode);
                    } else if (!sortedNodes.contains(leftNode)) {
                        sortedNodes.add(leftNode);
                    }
                }
            }
        }
        Instruction rightInstr = idInstructionMap.get(instruction.secondOp.valGenerator);
        if (rightInstr != null) {
            rightNode = graph.get(rightInstr);
            if (rightNode != null) {
                if (rightInstr.operator != Operators.phi) {
                    boolean interfere = false;
                    for (GraphNode node : cluster.members) {
                        if (rightNode.neighbors.contains(node)) {
                            interfere = true;
                        }
                    }
                    if (!interfere) {
                        cluster.members.add(rightNode);
                        rightNode.clusterAdded = true;
                        cluster.neighbors.addAll(rightNode.neighbors);
                        sortedNodes.remove(rightNode);
                    }
                } else {
                    if (rightNode.members.size() == 0) {
                        //cluster not processed
                        rightNode = buildCluster(graph, rightInstr, rightNode, sortedNodes);
                    }
                    boolean interfere = false;
                    for (GraphNode node : rightNode.members) {
                        for (GraphNode node1 : cluster.members) {
                            if (node.neighbors.contains(node1)) {
                                interfere = true;
                            }
                        }
                    }
                    if (!interfere) {
                        for (GraphNode node : rightNode.members) {
                            cluster.members.add(node);
                            node.clusterAdded = true;
                            cluster.neighbors.addAll(node.neighbors);
                        }
                        sortedNodes.remove(rightNode);
                    } else if (!sortedNodes.contains(rightNode)) {
                        sortedNodes.add(rightNode);
                    }
                }
            }
        }
        return cluster;
    }

    private boolean isColorAvailable(GraphNode node, int color) {
        String register = "R" + color;
        for (GraphNode neighbour : node.neighbors) {
            if (neighbour.instruction.storeRegister != null && neighbour.instruction.storeRegister.equals(register)) {
                return false;
            }
        }
        return true;
    }
}
