import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GraphNode {

    Integer instrID;
    Instruction instruction;
    HashSet<GraphNode> neighbors;
    List<GraphNode>members;
    boolean clusterAdded;

    public GraphNode(int id, Instruction instr){
        instrID = id;
        instruction = instr;
        neighbors = new HashSet<>();
        members = new ArrayList<>();
    }

    public String toString(){
        String ret = instrID +": ";
        for(GraphNode g:neighbors){
            ret += g.instrID + ", ";
        }
        return ret;
    }
}
