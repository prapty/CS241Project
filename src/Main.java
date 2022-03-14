import java.io.IOException;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        String fileName = args[0];
        try {
            Parser parser = new Parser(fileName);
            IntermediateTree intermediateTree = parser.getIntermediateRepresentation();
            String outputFileName = "outputCodeDot.dot";
            Dot dot = new Dot(outputFileName);
            dot.makeDotGraph(intermediateTree);

            InterferenceGraph interferenceGraph = new InterferenceGraph(intermediateTree);
            HashMap<Instruction, GraphNode>graph = interferenceGraph.getGraph();
            interferenceGraph.colorGraph();
            String coloredFileName = "coloredCodeDot.dot";
            Dot coloredDot = new Dot(coloredFileName);
            coloredDot.idInstructionMap = interferenceGraph.idInstructionMap;
            coloredDot.makeDotGraph(intermediateTree);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SyntaxException e) {
            e.printStackTrace();
        }
    }
}
