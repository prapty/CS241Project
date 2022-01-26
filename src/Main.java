import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String fileName = args[0];
        try {
            Parser parser = new Parser(fileName);
            IntermediateTree intermediateTree = parser.getIntermediateRepresentation();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SyntaxException e) {
            e.printStackTrace();
        }
    }
}
