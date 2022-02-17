import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Dot {

    String outputFile;

    public Dot(String filename) {
        outputFile = filename;
    }

    public void makeDotGraph(IntermediateTree irTree) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> branchLines = new ArrayList<>();
        lines.add("digraph G {");
        ArrayList<BasicBlock> blocks = new ArrayList<>();
        blocks.add(irTree.constants);
        while (!blocks.isEmpty()) {
            makeBlockCode(blocks.get(0), lines, branchLines);
//            if (!blocks.get(0).instructions.isEmpty()) {
//                Instruction i = blocks.get(0).getLastInstruction();
//                if (i.operator.toString().charAt(0) == 'b' && !i.operator.toString().equals("bra")) {
//                    branchSO(i, blocks.get(0), lines);
//                } else if (i.operator.toString().equals("bra")) {
//                    branchBRA(i, blocks.get(0), lines);
//                } else {
//                    fallThrough(blocks.get(0), lines);
//                }
//            }
            for (BasicBlock b : blocks.get(0).dominatorBlocks) {
                String domLine = "bb" + b.IDNum + ":b -> bb" + blocks.get(0).IDNum + ":b [color=blue, style=dotted, label=\"dom\"]";
                lines.add(domLine);
            }
            for (BasicBlock b : blocks.get(0).childBlocks) {
                blocks.add(b);
            }
            blocks.remove(0);
        }
        lines.addAll(branchLines);
        lines.add("}");

        Path file = Paths.get(outputFile);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private void fallThrough(BasicBlock basicBlock, BasicBlock fallBlock, ArrayList<String> lines, ArrayList<String> branchLines) {
        if (!basicBlock.childBlocks.isEmpty()) {
            for (BasicBlock b : basicBlock.childBlocks) {
                String fallThroughLine = "bb" + basicBlock.IDNum + ":s -> bb" + fallBlock.IDNum + ":n [label=\"fall-through\"];";
                branchLines.add(fallThroughLine);
            }
        }
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
            i.IDNum = Instruction.instrNum;
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
            Instruction.instrNum++;
        }

        instrline = instrline.substring(0, instrline.length() - 1);
        instrline += "}\"];";
        lines.add(instrline);
    }
}
