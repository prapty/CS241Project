import java.io.IOException;

public class Parser {
    Lexer lexer;
    Token token;

    public Parser(String fileName) throws IOException, SyntaxException {
        this.lexer = new Lexer(fileName);
        token = lexer.nextToken();
    }

    IntermediateTree getIntermediateRepresentation() {
        IntermediateTree intermediateTree = new IntermediateTree();

        return intermediateTree;
    }

    public void Computation(IntermediateTree irTree) throws SyntaxException, IOException {
        /*order:
        main, varDecl, funcDecl,"{", statsequence, "}", "."
         */
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.mainDefaultId.ordinal()) {
            //return error
        } else {
            token = lexer.nextToken();
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.varDefaultId.ordinal()) {

        }

    }

    private void Expression(IntermediateTree irTree) throws IOException, SyntaxException {
        Term(irTree);
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.plusDefaultId.ordinal() || token.id == ReservedWords.minusDefaultId.ordinal())) {
            token = lexer.nextToken();
            Term(irTree);
        }
    }

    private void Term(IntermediateTree irTree) throws SyntaxException, IOException {
        Factor(irTree);
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.mulDefaultId.ordinal() || token.id == ReservedWords.divDefaultId.ordinal())) {
            token = lexer.nextToken();
            Factor(irTree);
        }
    }

    private void Factor(IntermediateTree irTree) throws SyntaxException, IOException {
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
            token = lexer.nextToken();
            Expression(irTree);
            if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.endingFirstBracketDefaultId.ordinal()) {
                //something
                token = lexer.nextToken();
            } else {
                String errorMessage = String.format(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR);
                throw new SyntaxException(errorMessage);
            }
        } else if (token.kind == TokenKind.identity) {
            //something
            token = lexer.nextToken();
        } else if (token.kind == TokenKind.number) {
            //something
            token = lexer.nextToken();
        } else if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.callDefaultId.ordinal()) {
            //do later
        }
    }


    private void IfStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        Expression(irTree);
        if (token.kind == TokenKind.relOp) {

        }
    }
}
