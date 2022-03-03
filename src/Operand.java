public class Operand {
    boolean constant;
    int constVal;
    int id;
    Integer valGenerator;
    Instruction returnVal;
    String arraybase;

    public Operand() {
    }
    public Operand(Operand operand) {
        constant = operand.constant;
        constVal = operand.constVal;
        id = operand.id;
        valGenerator = operand.valGenerator;
        returnVal = operand.returnVal;
    }

    public Operand(boolean constant, int constVal, Integer valGenerator, int id) {
        this.constant = constant;
        this.constVal = constVal;
        this.valGenerator = valGenerator;
        this.id=id;
    }

    public Operand(String arrayBase){
        arraybase = arrayBase;
    }
}
