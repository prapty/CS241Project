import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

public class Dot {

    String outputFile;
    Map<Integer, Instruction> idInstructionMap;

    public Dot(String filename) {
        outputFile = filename;
    }

    public void makeDotGraph(IntermediateTree irTree) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        ArrayList<String> branchLines = new ArrayList<>();
        lines.add("digraph G {");
        LinkedList<BasicBlock> blocks = new LinkedList<>();
        ArrayList<BasicBlock> visited = new ArrayList<>();
        blocks.add(irTree.constants);
        visited.add(irTree.constants);
        while (!blocks.isEmpty()) {

            makeBlockCode(blocks.get(0), lines, branchLines);
            if (blocks.get(0).dominatorBlock != null) {
                BasicBlock bl = blocks.get(0).dominatorBlock;
                while (bl != null) {
                    String domLine = "bb" + bl.IDNum + ":b -> bb" + blocks.get(0).IDNum + ":b [color=blue, style=dotted, label=\"dom\"]";
                    branchLines.add(domLine);
                    bl = bl.dominatorBlock;
                }
            }
            for (BasicBlock b : blocks.get(0).childBlocks) {
                if (!visited.contains(b)) {
                    blocks.add(b);
                    visited.add(b);
                }
            }
            blocks.poll();
        }
        lines.addAll(branchLines);
        lines.add("}");

        Path file = Paths.get(outputFile);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private void fallThrough(BasicBlock basicBlock, BasicBlock fallBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        String fallThroughLine = "bb" + basicBlock.IDNum + ":s -> bb" + fallBlock.IDNum + ":n [label=\"fall-through\"];";
        branchLines.add(fallThroughLine);
    }

    private void functionCall(BasicBlock basicBlock, BasicBlock fallBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        String fallThroughLine = "bb" + basicBlock.IDNum + ":s -> bb" + fallBlock.IDNum + ":n [color=red, label=\"function-call\"];";
        branchLines.add(fallThroughLine);
    }

    private void branchBRA(Instruction i, BasicBlock basicBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        Integer branchTo = i.firstOp.valGenerator;
        BasicBlock branchBlock = null;
        for (BasicBlock cb : basicBlock.childBlocks) {
            if (cb.instructionIDs.contains(branchTo)) {
                branchBlock = cb;
                break;
            }
        }

        String branchLine = "bb" + basicBlock.IDNum + ":s -> bb" + branchBlock.IDNum + ":n [label=\"branch\"];";
        branchLines.add(branchLine);
    }

    private void branchSO(Instruction i, BasicBlock basicBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        Integer branchTo = i.secondOp.valGenerator;
        BasicBlock branchBlock = null;
        for (BasicBlock cb : basicBlock.childBlocks) {
            if (cb.instructionIDs.contains(branchTo)) {
                branchBlock = cb;
                break;
            } else {
                fallThrough(basicBlock, cb, lines, branchLines);
            }
        }
        String branchLine = "bb" + basicBlock.IDNum + ":s -> bb" + branchBlock.IDNum + ":n [label=\"branch\"];";
        branchLines.add(branchLine);
    }

    private void makeBlockCode(BasicBlock basicBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        lines.add("bb" + basicBlock.IDNum + " [shape=record, label=\"<b>BB" + basicBlock.IDNum + "|");
        String instrline = "{";
        for (Instruction i : basicBlock.instructions) {
            if (i.operator.toString().charAt(0) == 'b' && !i.operator.toString().equals("bra")) {
                branchSO(i, basicBlock, lines, branchLines);
            } else if (i.operator.toString().equals("bra")) {
                branchBRA(i, basicBlock, lines, branchLines);
            } else if (i == basicBlock.getLastInstruction() && i.operator.toString().charAt(0) != 'b') {
                if (!basicBlock.childBlocks.isEmpty()) {
                    for(int j=0; j<basicBlock.childBlocks.size(); j++){
                        BasicBlock child = basicBlock.childBlocks.get(j);
                        if(child.functionHead){
                            functionCall(basicBlock, child, lines, branchLines);
                        }
                        else{
                            fallThrough(basicBlock, child, lines, branchLines);
                        }
                    }

                }
            }
            if(idInstructionMap!=null){
                instrline += i.IDNum + ": " + i.toString(idInstructionMap) + "|";
            }
            else{
                instrline += i.IDNum + ": " + i.toString() + "|";
            }
        }

        instrline = instrline.substring(0, instrline.length() - 1);
        instrline += "}\"];";
        lines.add(instrline);
    }
}
