import java.util.Comparator;

public class BasicBlockComparator implements Comparator<BasicBlock> {
    @Override
    public int compare(BasicBlock o1, BasicBlock o2) {
        if (o1.IDNum > o2.IDNum) {
            return -1;
        } else if (o1.IDNum < o2.IDNum) {
            return 1;
        } else if (o1.isCond && o2.isCond) {
            if (o1.IDNum2 > o2.IDNum2) {
                return -1;
            } else if (o1.IDNum2 < o2.IDNum2) {
                return 1;
            }
        } else if (o1.isCond) {
            return 1;
        } else if (o2.isCond) {
            return -1;
        }
        return 0;
    }

}
