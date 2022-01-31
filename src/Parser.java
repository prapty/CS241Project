import java.io.IOException;

public class Parser {
    Lexer lexer;
    Token token;
    Instruction assignZeroInstruction;
    Operand zeroOperand;

    public Parser(String fileName) throws IOException, SyntaxException {
        this.lexer = new Lexer(fileName);
        token = lexer.nextToken();
        zeroOperand= new Operand(true, 0, null);
        assignZeroInstruction=new Instruction(Operators.add, zeroOperand, zeroOperand);
    }

    IntermediateTree getIntermediateRepresentation() throws SyntaxException, IOException {
        IntermediateTree intermediateTree = new IntermediateTree();
        Computation(intermediateTree);
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
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR,"reserved word");
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

    private void assignment(IntermediateTree irTree) throws IOException, SyntaxException {
        Token left=token;
        token=lexer.nextToken();
        if(token.kind!=TokenKind.relOp && token.id!=ReservedWords.assignmentSymbolDefaultId.ordinal()){
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR,"->");
        }
        token=lexer.nextToken();
        Expression(irTree);
        Instruction instruction=irTree.current.getLastInstruction();
        if(instruction.operator==null){
            instruction.operator=Operators.add;
            instruction.firstOp = zeroOperand;
            irTree.current.setLastInstruction(instruction);
        }
        irTree.current.valueInstructionMap.put(left.id,instruction);
    }

    private void funcCall(IntermediateTree irTree) { //do later
    }

    private void IfStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        relation(irTree);
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.thenDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock ifBlock = new BasicBlock();
            ifBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
            ifBlock.dominatorTree=irTree.current.dominatorTree.clone();
            ifBlock.parentBlocks.add(irTree.current);
            irTree.current.childBlocks.add(ifBlock);
            irTree.current=ifBlock;
            statSequence(irTree);
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR,"then");
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.elseDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock thenBlock = new BasicBlock();
            thenBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
            thenBlock.dominatorTree=irTree.current.dominatorTree.clone();
            irTree.current=irTree.current.parentBlocks.get(0);
            thenBlock.parentBlocks.add(irTree.current);
            irTree.current.childBlocks.add(thenBlock);
            irTree.current=thenBlock;
            statSequence(irTree);
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.fiDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock join=new BasicBlock();
            irTree.current=irTree.current.parentBlocks.get(0);
            for(int i=0;i<irTree.current.childBlocks.size();i++){
                BasicBlock block=irTree.current.childBlocks.get(i);
                join.parentBlocks.add(block);
                block.childBlocks.add(join);
                //also need to insert phi instructions here
            }
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR,"fi");
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
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR,"relOp");
        } else {
            //handle relOp
            token = lexer.nextToken();
            Expression(irTree);
        }
    }

    private void varDecl(IntermediateTree irTree) {

    }

    private void Expression(IntermediateTree irTree) throws IOException, SyntaxException {
        int lastIndex=-1;
        Term(irTree);
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.plusDefaultId.ordinal() || token.id == ReservedWords.minusDefaultId.ordinal())) {
            token = lexer.nextToken();
            Instruction instruction=irTree.current.getLastInstruction();
            Operators op;
            InstructionLinkedList node = new InstructionLinkedList();
            if(token.id == ReservedWords.plusDefaultId.ordinal()){
                op=Operators.add;
            }
            else{
                op=Operators.sub;
            }
            if(instruction.operator==null){
                instruction.operator=op;
                node.value=instruction;
                irTree.current.setLastInstruction(instruction);
            }
            else{
                Operand firsrOp=new Operand(false, 0, instruction);
                Instruction newInstruction = new Instruction(op, firsrOp, null);
                node.value=newInstruction;
                irTree.current.instructions.add(newInstruction);
                if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                    lastIndex=irTree.current.instructions.size()-1;
                    Instruction initInstruction = new Instruction(null, null, null);
                    irTree.current.instructions.add(initInstruction);
                }
            }
            node.previous=irTree.current.dominatorTree[op.ordinal()];
            irTree.current.dominatorTree[op.ordinal()]=node;
            Term(irTree);
        }
        if(lastIndex !=-1){
            Instruction instruction = irTree.current.getAnyInstruction(lastIndex);
            Instruction lastInstruction=irTree.current.getLastInstruction();
            Operand op = new Operand(false, 0, lastInstruction);
            instruction.secondOp=op;
            irTree.current.setAnyInstruction(lastIndex, instruction);
        }

    }

    private void Term(IntermediateTree irTree) throws SyntaxException, IOException {
        Factor(irTree);
        int lastIndex=-1;
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.mulDefaultId.ordinal() || token.id == ReservedWords.divDefaultId.ordinal())) {
            token = lexer.nextToken();
            Instruction instruction=irTree.current.getLastInstruction();
            InstructionLinkedList node = new InstructionLinkedList();
            Operators op;
            if(token.id == ReservedWords.mulDefaultId.ordinal()){
                op=Operators.mul;
            }
            else{
                op=Operators.div;
            }
            if(instruction.operator==null){
                instruction.operator=op;
                node.value=instruction;
                irTree.current.setLastInstruction(instruction);
            }
            else{
                Operand firsrOp=new Operand(false, 0, instruction);
                Instruction newInstruction = new Instruction(op, firsrOp, null);
                node.value=newInstruction;
                irTree.current.instructions.add(newInstruction);
                if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                    lastIndex=irTree.current.instructions.size()-1;
                    Instruction initInstruction = new Instruction(null, null, null);
                    irTree.current.instructions.add(initInstruction);
                }
            }
            node.previous=irTree.current.dominatorTree[op.ordinal()];
            irTree.current.dominatorTree[op.ordinal()]=node;
            Factor(irTree);
        }
        if(lastIndex !=-1){
            Instruction instruction = irTree.current.getAnyInstruction(lastIndex);
            Instruction lastInstruction=irTree.current.getLastInstruction();
            Operand op = new Operand(false, 0, lastInstruction);
            instruction.secondOp=op;
            irTree.current.setAnyInstruction(lastIndex, instruction);
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
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR,")");
            }

        } else if (token.kind == TokenKind.identity || token.kind == TokenKind.number) {
            Operand op=new Operand();
            if (token.kind == TokenKind.identity){
                //identity
                Instruction valueGenerator=irTree.current.valueInstructionMap.get(token.id);
                if(valueGenerator==null){
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                }
                op= new Operand(false, 0,  valueGenerator);
            }
            if (token.kind == TokenKind.number){
                //num
                op= new Operand(true, token.val,  null);
            }
            Instruction instruction = irTree.current.getLastInstruction();
            if(instruction==null){
                instruction = new Instruction(null, op, null);
                irTree.current.instructions.add(instruction);
            }
            else if(instruction.operator==null){
                instruction.firstOp=op;
                irTree.current.setLastInstruction(instruction);
            }
            else if(instruction.operator!=null && instruction.secondOp==null){
                instruction.secondOp=op;
                irTree.current.setLastInstruction(instruction);
            }
            token = lexer.nextToken();
        } else if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.callDefaultId.ordinal()) {
            //do later
        }
    }

    private void error(String message, String expected) throws SyntaxException {
        String errorMessage = String.format(message, expected);
        throw new SyntaxException(errorMessage);
    }

    private void warning(String message){
        System.out.println(message);
    }
}
