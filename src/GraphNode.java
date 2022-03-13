import java.util.HashSet;

public class GraphNode {

    Integer instrID;
    Instruction instruction;
    HashSet<GraphNode> neighbors;


    public GraphNode(int id, Instruction instr){
        instrID = id;
        instruction = instr;
        neighbors = new HashSet<>();
    }

    public String toString(){
        String ret = instrID +": ";
        for(GraphNode g:neighbors){
            ret += g.instrID + ", ";
        }
        return ret;
    }
}
