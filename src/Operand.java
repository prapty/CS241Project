public class Operand {
    boolean constant;
    int constVal;
    int id;
    Instruction valGenerator;

    public Operand() {
    }

    public Operand(boolean constant, int constVal, Instruction valGenerator,int id) {
        this.constant = constant;
        this.constVal = constVal;
        this.valGenerator = valGenerator;
        this.id=id;
    }
}
