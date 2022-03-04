import java.util.ArrayList;

public class ArrayIdent {

    Token identifier;
    private int size;
    ArrayList<Integer> dimensions;
    private int startingAddress;
    static int currentAddress;


    public ArrayIdent(Token ident) {
        identifier = ident;
        dimensions = new ArrayList<>();
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
        if (startingAddress == 0) {
            startingAddress = currentAddress - size;
            currentAddress = startingAddress;
        }
        return startingAddress;
    }
}
