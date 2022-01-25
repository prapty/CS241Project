import java.io.IOException;

public class Parser {
    Lexer lexer;
    Token token;

    public Parser(String fileName) throws IOException, SyntaxException {
        this.lexer=new Lexer(fileName);
        token=lexer.nextToken();
    }

    IntermediateTree getIntermediateRepresentation(){
        IntermediateTree intermediateTree=new IntermediateTree();

        return intermediateTree;
    }
    private void Expression(IntermediateTree irTree) throws IOException, SyntaxException {
        Term(irTree);
        while(token.kind==TokenKind.reservedSymbol && (token.id==ReservedWords.plusDefaultId.ordinal()||token.id==ReservedWords.minusDefaultId.ordinal())){
            Term(irTree);
        }
    }

    private void Term(IntermediateTree irTree) {
        Factor(irTree);
        while(token.kind==TokenKind.reservedSymbol && (token.id==ReservedWords.mulDefaultId.ordinal()||token.id==ReservedWords.divDefaultId.ordinal())){
            Factor(irTree);
        }
    }

    private void Factor(IntermediateTree irTree) {

    }
}
