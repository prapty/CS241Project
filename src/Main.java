import java.io.IOException;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
//        String fileName = args[0];
        try {
//            Parser parser = new Parser(fileName);
            Parser parser = new Parser("simpleArray.txt");
            IntermediateTree intermediateTree = parser.getIntermediateRepresentation();
            String outputFileName = "outputCodeDot.dot";
            Dot dot = new Dot(outputFileName);
            dot.makeDotGraph(intermediateTree);

            InterferenceGraph interferenceGraph = new InterferenceGraph(intermediateTree);
            HashMap<Instruction, GraphNode>graph = interferenceGraph.getGraph();
            interferenceGraph.colorGraph(graph);

            String coloredFileName = "coloredCodeDot.dot";
            Dot coloredDot = new Dot(coloredFileName);
            coloredDot.idInstructionMap = interferenceGraph.idInstructionMap;
            for(Function func : interferenceGraph.functionsInterferenceGraph.keySet()){
                InterferenceGraph ig = interferenceGraph.functionsInterferenceGraph.get(func);
                HashMap<Instruction, GraphNode> funcGraph = ig.getFunctionGraph(func);
                ig.colorGraph(funcGraph);
                coloredDot.idInstructionMap.putAll(ig.idInstructionMap);
            }

            coloredDot.makeDotGraph(intermediateTree);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SyntaxException e) {
            e.printStackTrace();
        }
    }
}
