import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String fileName = args[0];
        try {
            Parser parser = new Parser(fileName);
            //Parser parser = new Parser("simpleArray.txt");
            Map<Integer, IntermediateTree> intermediateTreeMap = parser.getIntermediateRepresentation();
            IntermediateTree intermediateTree = intermediateTreeMap.get(ReservedWords.mainDefaultId.ordinal());
            String outputFileName = "outputCodeDot.dot";
            Dot dot = new Dot(outputFileName);
            //main irTree put against main
            dot.makeDotGraph(intermediateTree);
            String coloredFileName = "coloredCodeDot.dot";
            Dot coloredDot = new Dot(coloredFileName);
            for(int id : intermediateTreeMap.keySet()){
                IntermediateTree irTree = intermediateTreeMap.get(id);
                InterferenceGraph interferenceGraph = new InterferenceGraph(irTree);
                HashMap<Instruction, GraphNode>graph = interferenceGraph.getGraph();
                interferenceGraph.colorGraph(graph);
                coloredDot.idInstructionMap.putAll(interferenceGraph.idInstructionMap);
            }
            coloredDot.makeDotGraph(intermediateTree);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SyntaxException e) {
            e.printStackTrace();
        }
    }
}
