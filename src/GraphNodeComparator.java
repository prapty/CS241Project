import java.util.Comparator;

public class GraphNodeComparator implements Comparator<GraphNode> {
    @Override
    public int compare(GraphNode o1, GraphNode o2) {
        if(o1.instruction.cost > o2.instruction.cost){
            return -1;
        } else if (o1.instruction.cost < o2.instruction.cost){
            return 1;
        }
        return 0;
    }
}
