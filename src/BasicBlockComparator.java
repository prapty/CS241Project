import java.util.Comparator;

public class BasicBlockComparator implements Comparator<BasicBlock> {
    @Override
    public int compare(BasicBlock o1, BasicBlock o2) {
        if(o1.IDNum > o2.IDNum){
            return -1;
        } else if (o1.IDNum < o2.IDNum){
            return 1;
        }
        return 0;
    }
}
