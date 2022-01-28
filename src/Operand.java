public class Operand {
    boolean constant;
    int constVal;
    Instruction valGenerator;

    public Operand() {
    }

    public Operand(boolean constant, int constVal, Instruction valGenerator) {
        this.constant = constant;
        this.constVal = constVal;
        this.valGenerator = valGenerator;
    }
}
