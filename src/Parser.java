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
        //order:
        // main, varDecl, funcDecl,"{", statsequence, "}", "."
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.mainDefaultId.ordinal()) {
            //if not main, return error
        } else {
            token = lexer.nextToken();
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.varDefaultId.ordinal()) {
            //parse all variables
            varDecl(irTree);
            while (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.varDefaultId.ordinal() || token.id == ReservedWords.arrayDefaultId.ordinal())) {
                varDecl(irTree);
            }
        }
        //funcdecl
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingCurlyBracketDefaultId.ordinal()) {
            token = lexer.nextToken();
            statSequence(irTree); //parse statement sequence
        } else {
            //if no {, error
        }
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.endingCurlyBracketDefaultId.ordinal()) {
            //if no }, error
        }
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.dotDefaultId.ordinal()) {
            //if no ., error
        }
    }

    private void statSequence(IntermediateTree irTree) throws SyntaxException, IOException {
        statement(irTree);
        while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.semicolonDefaultId.ordinal()) {
            token = lexer.nextToken();
            if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.endingCurlyBracketDefaultId.ordinal()) {
                break; //if ; was the last optional terminating semicolon, stop parsing statements
            }
            statement(irTree);
        }
    }

    private void statement(IntermediateTree irTree) throws SyntaxException, IOException {
        //possibilities: let, call, if, while, return
        if (token.kind != TokenKind.reservedWord) {
            //throw error
        }
        if (token.id == ReservedWords.letDefaultId.ordinal()) {
            token = lexer.nextToken();
            assignment(irTree);
        }
        if (token.id == ReservedWords.callDefaultId.ordinal()) {
            token = lexer.nextToken();
            funcCall(irTree);
        }
        if (token.id == ReservedWords.ifDefaultId.ordinal()) {
            token = lexer.nextToken();
            IfStatement(irTree);
        }
        if (token.id == ReservedWords.whileDefaultId.ordinal()) {
            token = lexer.nextToken();
            whileStatement(irTree);
        }
        if (token.id == ReservedWords.returnDefaultId.ordinal()) {
            token = lexer.nextToken();
            returnStatement(irTree);
        } else {
            //trhow error
        }
    }

    private void assignment(IntermediateTree irTree) {

    }

    private void funcCall(IntermediateTree irTree) { //do later
    }

    private void IfStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        relation(irTree);
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.thenDefaultId.ordinal()) {
            token = lexer.nextToken();
            statSequence(irTree);
        } else {
            //error
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.elseDefaultId.ordinal()) {
            token = lexer.nextToken();
            statSequence(irTree);
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.fiDefaultId.ordinal()) {
            token = lexer.nextToken();
        } else {
            //error
        }
    }

    private void whileStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        relation(irTree);
    }


    private void returnStatement(IntermediateTree irTree) {

    }

    private void relation(IntermediateTree irTree) throws SyntaxException, IOException {
        Expression(irTree);
        if (token.kind != TokenKind.relOp || token.id == ReservedWords.assignmentSymbolDefaultId.ordinal()) {
            //errror
        } else {
            //handle relOp
            token = lexer.nextToken();
            Expression(irTree);
        }
    }

    private void varDecl(IntermediateTree irTree) {

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
                //end expression
                token = lexer.nextToken();
            } else {
                String errorMessage = String.format(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR);
                throw new SyntaxException(errorMessage);
            }
        } else if (token.kind == TokenKind.identity) {
            //num
            token = lexer.nextToken();
        } else if (token.kind == TokenKind.number) {
            //num
            token = lexer.nextToken();
        } else if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.callDefaultId.ordinal()) {
            //do later
        }
    }


}
