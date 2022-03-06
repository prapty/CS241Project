import java.util.ArrayList;

public class ArrayIdent {

    Token identifier;
    private int size;
    ArrayList<Integer> dimensions;
    ArrayList<Operand> opDims;
    private int startingAddress;
    static int currentAddress;


    public ArrayIdent(Token ident) {
        identifier = ident;
        dimensions = new ArrayList<>();
        opDims = new ArrayList<>();
    }

    public int getSize() {
        if (size == 0) {
            size = dimensions.get(0);
            for (int i = 1; i < dimensions.size(); i++) {
                size *= dimensions.get(i);
            }
        }
        return size;
    }

    public int getStartingAddress() {
        size = getSize();
        if (startingAddress == 0) {
            startingAddress = (currentAddress - size) * 4;
            currentAddress = startingAddress;
        }
        return startingAddress;
    }
}
