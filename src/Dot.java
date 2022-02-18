import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;

public class Dot {

    String outputFile;

    public Dot(String filename) {
        outputFile = filename;
    }

    public void makeDotGraph(IntermediateTree irTree) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> branchLines = new ArrayList<>();
        lines.add("digraph G {");
        updateInstructionID(irTree);
        LinkedList<BasicBlock> blocks = new LinkedList<>();
        blocks.add(irTree.constants);
        blocks.get(0).visbranch = true;
        while (!blocks.isEmpty()) {
            makeBlockCode(blocks.get(0), lines, branchLines);
//            for (BasicBlock b : blocks.get(0).dominatorBlocks) {
            if (blocks.get(0).dominatorBlock != null) {
                BasicBlock bl = blocks.get(0).dominatorBlock;
                while (bl != null) {
                    String domLine = "bb" + bl.IDNum + ":b -> bb" + blocks.get(0).IDNum + ":b [color=blue, style=dotted, label=\"dom\"]";
                    branchLines.add(domLine);
                    bl = bl.dominatorBlock;
                }
            }
            for (BasicBlock b : blocks.get(0).childBlocks) {
                if (!b.visbranch) {
                    blocks.add(b);
                    b.visbranch = true;
                }
                ;
            }
            blocks.poll();
        }
        lines.addAll(branchLines);
        lines.add("}");

        Path file = Paths.get(outputFile);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private void updateInstructionID(IntermediateTree irTree) {
        LinkedList<BasicBlock> blocks = new LinkedList<>();
        blocks.add(irTree.constants);
        blocks.get(0).vis = true;
        while (!blocks.isEmpty()) {
            for (Instruction i : blocks.get(0).instructions) {
                i.IDNum = Instruction.instrNum;
                Instruction.instrNum++;
            }
            for (BasicBlock b : blocks.get(0).childBlocks) {
                if (!b.vis) {
                    blocks.add(b);
                    b.vis = true;
                }
            }
            blocks.poll();
        }
    }

    private void fallThrough(BasicBlock basicBlock, BasicBlock fallBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        String fallThroughLine = "bb" + basicBlock.IDNum + ":s -> bb" + fallBlock.IDNum + ":n [label=\"fall-through\"];";
        branchLines.add(fallThroughLine);
    }

    private void branchBRA(Instruction i, BasicBlock basicBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        Instruction branchTo = i.firstOp.valGenerator;
        BasicBlock branchBlock = null;
        for (BasicBlock cb : basicBlock.childBlocks) {
            if (cb.instructions.contains(branchTo)) {
                branchBlock = cb;
                break;
            }
        }
        String branchLine = "bb" + basicBlock.IDNum + ":s -> bb" + branchBlock.IDNum + ":n [label=\"branch\"];";
        branchLines.add(branchLine);
    }

    private void branchSO(Instruction i, BasicBlock basicBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        Instruction branchTo = i.secondOp.valGenerator;
        BasicBlock branchBlock = null;
        for (BasicBlock cb : basicBlock.childBlocks) {
            if (cb.instructions.contains(branchTo)) {
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
//            i.IDNum = Instruction.instrNum;
            if (i.operator.toString().charAt(0) == 'b' && !i.operator.toString().equals("bra")) {
                branchSO(i, basicBlock, lines, branchLines);
            } else if (i.operator.toString().equals("bra")) {
                branchBRA(i, basicBlock, lines, branchLines);
            } else if (i == basicBlock.getLastInstruction() && i.operator.toString().charAt(0) != 'b') {
                if (!basicBlock.childBlocks.isEmpty()) {
                    fallThrough(basicBlock, basicBlock.childBlocks.get(0), lines, branchLines);
                }
            }
            instrline += i.IDNum + ": " + i.toString() + "|";
//            Instruction.instrNum++;
        }

        instrline = instrline.substring(0, instrline.length() - 1);
        instrline += "}\"];";
        lines.add(instrline);
    }
}
