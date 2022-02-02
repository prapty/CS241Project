import java.io.IOException;

public class Parser {
    Lexer lexer;
    Token token;
    Instruction assignZeroInstruction;
    Operand zeroOperand;

    public Parser(String fileName) throws IOException, SyntaxException {
        this.lexer = new Lexer(fileName);
        token = lexer.nextToken();
        zeroOperand = new Operand(true, 0, null);
        assignZeroInstruction = new Instruction(Operators.add, zeroOperand, zeroOperand);
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
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "main");
        } else {
            token = lexer.nextToken();
        }
        if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.varDefaultId.ordinal() || token.id == ReservedWords.arrayDefaultId.ordinal())) {
            //parse all variables
            varDecl(irTree);

            while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.semicolonDefaultId.ordinal()) {
                //after ";" , check if next is var decl
                token = lexer.nextToken();
                if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.varDefaultId.ordinal() || token.id == ReservedWords.arrayDefaultId.ordinal())) {
                    varDecl(irTree);
                }
            }
        }

        //funcdecl

        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingCurlyBracketDefaultId.ordinal()) {
            token = lexer.nextToken();
            statSequence(irTree); //parse statement sequence
        } else {
            //if no {, error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "{");
        }
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.endingCurlyBracketDefaultId.ordinal()) {
            //if no }, error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "}");
        }
        token = lexer.nextToken();
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.dotDefaultId.ordinal()) {
            //if no ., error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ".");
        }
    }

    private void statSequence(IntermediateTree irTree) throws SyntaxException, IOException {
        statement(irTree);
        while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.semicolonDefaultId.ordinal()) {
            token = lexer.nextToken();
            if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.endingCurlyBracketDefaultId.ordinal()) {
                break; //if ; was the last optional terminating semicolon before ; stop parsing
            }
            if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.elseDefaultId.ordinal() || token.id == ReservedWords.fiDefaultId.ordinal() || token.id == ReservedWords.odDefaultId.ordinal())) {
                break; //if ; was the last optional terminating semicolon before else, or fi, or od stop parsing
            }
            statement(irTree);
        }
    }

    private void statement(IntermediateTree irTree) throws SyntaxException, IOException {
        //possibilities: let, call, if, while, return
        if (token.kind != TokenKind.reservedWord) {
            //throw error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "reserved word");
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
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "let OR call OR if OR while OR return");
        }
    }

    private void assignment(IntermediateTree irTree) throws IOException, SyntaxException {
        if (token.kind != TokenKind.identity) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
        }
        Token left = token; //change when considering arrays
        token = lexer.nextToken();
        if (token.kind != TokenKind.relOp && token.id != ReservedWords.assignmentSymbolDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "->");
        }
        token = lexer.nextToken();
        Expression(irTree);
        Instruction instruction = irTree.current.getLastInstruction();
        if (instruction.operator == null) {
            instruction.operator = Operators.add;
            instruction.firstOp = zeroOperand;
            irTree.current.setLastInstruction(instruction);
        }
        irTree.current.valueInstructionMap.put(left.id, instruction);
    }

    private void funcCall(IntermediateTree irTree) { //do later
    }

    private void IfStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        relation(irTree);
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.thenDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock thenBlock = new BasicBlock();
            thenBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
            thenBlock.dominatorTree = irTree.current.dominatorTree.clone();
            thenBlock.parentBlocks.add(irTree.current);
            irTree.current.childBlocks.add(thenBlock);
            irTree.current = thenBlock;
            statSequence(irTree);
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "then");
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.elseDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock elseBlock = new BasicBlock();
            elseBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
            elseBlock.dominatorTree = irTree.current.dominatorTree.clone();
            irTree.current = irTree.current.parentBlocks.get(0);
            elseBlock.parentBlocks.add(irTree.current);
            irTree.current.childBlocks.add(elseBlock);
            irTree.current = elseBlock;
            statSequence(irTree);
        } else { //empty else block
            BasicBlock elseBlock = new BasicBlock();
            elseBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
            elseBlock.dominatorTree = irTree.current.dominatorTree.clone();
            irTree.current = irTree.current.parentBlocks.get(0);
            elseBlock.parentBlocks.add(irTree.current);
            irTree.current.childBlocks.add(elseBlock);
            irTree.current = elseBlock;
        }
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.fiDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock join = new BasicBlock();
            irTree.current = irTree.current.parentBlocks.get(0);
            for (int i = 0; i < irTree.current.childBlocks.size(); i++) {
                BasicBlock block = irTree.current.childBlocks.get(i);
                join.parentBlocks.add(block);
                block.childBlocks.add(join);
                //also need to insert phi instructions here
            }
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "fi");
        }
    }

    private void whileStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        BasicBlock condBlock = new BasicBlock();
        condBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
        condBlock.dominatorTree = irTree.current.dominatorTree.clone();
        condBlock.parentBlocks.add(irTree.current);
        irTree.current.childBlocks.add(condBlock);
        irTree.current = condBlock;
        relation(irTree);
        if (token.kind != TokenKind.reservedWord || token.id != ReservedWords.doDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "do");
        }
        token = lexer.nextToken();
        BasicBlock whileBlock = new BasicBlock();
        whileBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
        whileBlock.dominatorTree = irTree.current.dominatorTree.clone();
        whileBlock.parentBlocks.add(irTree.current);
        irTree.current.childBlocks.add(whileBlock);
        irTree.current.parentBlocks.add(whileBlock);
        //need to add phi for current (condBlock)
        irTree.current = whileBlock;
        statSequence(irTree);
        if (token.kind != TokenKind.reservedWord || token.id != ReservedWords.odDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "od");
        }
        BasicBlock newBlock = new BasicBlock();
        newBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
        newBlock.dominatorTree = irTree.current.dominatorTree.clone();
        newBlock.parentBlocks.add(condBlock);
        condBlock.childBlocks.add(newBlock);
        irTree.current = newBlock;
    }


    private void returnStatement(IntermediateTree irTree) {

    }

    private void relation(IntermediateTree irTree) throws SyntaxException, IOException {
        Expression(irTree);
        if (token.kind != TokenKind.relOp || token.id == ReservedWords.assignmentSymbolDefaultId.ordinal()) {
            //errror
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "relOp");
        } else {
            //handle relOp
            Token relOp = token;
            token = lexer.nextToken();
            Expression(irTree);
        }
    }

    private void varDecl(IntermediateTree irTree) throws SyntaxException, IOException {
        if (token.id == ReservedWords.varDefaultId.ordinal()) {
        }

        token = lexer.nextToken();
        if (token.kind != TokenKind.identity) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
        }
        token = lexer.nextToken();
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.commaDefaultId.ordinal())) {
            token = lexer.nextToken();
            if (token.kind != TokenKind.identity) {
                //error
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "ident");
            }
            token = lexer.nextToken();
            //store ident?
        }
        //need to check if var or array
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.semicolonDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ";");
        }
        //; is handled elsewhere
    }

    private void Expression(IntermediateTree irTree) throws IOException, SyntaxException {
        int lastIndex = -1;
        Term(irTree);
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.plusDefaultId.ordinal() || token.id == ReservedWords.minusDefaultId.ordinal())) {
            token = lexer.nextToken();
            Instruction instruction = irTree.current.getLastInstruction();
            Operators op;
            InstructionLinkedList node = new InstructionLinkedList();
            if (token.id == ReservedWords.plusDefaultId.ordinal()) {
                op = Operators.add;
            } else {
                op = Operators.sub;
            }
            if (instruction.operator == null) {
                instruction.operator = op;
                node.value = instruction;
                irTree.current.setLastInstruction(instruction);
            } else {
                Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[op.ordinal()], instruction);
                if (duplicate != null) {
                    instruction = duplicate;
                    irTree.current.instructions.remove(irTree.current.instructions.size() - 1);
                }
                Operand firsrOp = new Operand(false, 0, instruction);
                Instruction newInstruction = new Instruction(op, firsrOp, null);
                node.value = newInstruction;
                irTree.current.instructions.add(newInstruction);
                if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                    lastIndex = irTree.current.instructions.size() - 1;
                    Instruction initInstruction = new Instruction(null, null, null);
                    irTree.current.instructions.add(initInstruction);
                }
            }
            node.previous = irTree.current.dominatorTree[op.ordinal()];
            irTree.current.dominatorTree[op.ordinal()] = node;
            Term(irTree);
        }
        if (lastIndex != -1) {
            Instruction instruction = irTree.current.getAnyInstruction(lastIndex);
            Instruction lastInstruction = irTree.current.getLastInstruction();
            Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[lastInstruction.operator.ordinal()], lastInstruction);
            if (duplicate != null) {
                lastInstruction = duplicate;
                irTree.current.instructions.remove(irTree.current.instructions.size() - 1);
            }
            Operand op = new Operand(false, 0, lastInstruction);
            instruction.secondOp = op;
            irTree.current.setAnyInstruction(lastIndex, instruction);
        }
    }

    private void Term(IntermediateTree irTree) throws SyntaxException, IOException {
        Factor(irTree);
        int lastIndex = -1;
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.mulDefaultId.ordinal() || token.id == ReservedWords.divDefaultId.ordinal())) {
            token = lexer.nextToken();
            Instruction instruction = irTree.current.getLastInstruction();
            InstructionLinkedList node = new InstructionLinkedList();
            Operators op;
            if (token.id == ReservedWords.mulDefaultId.ordinal()) {
                op = Operators.mul;
            } else {
                op = Operators.div;
            }
            if (instruction.operator == null) {
                instruction.operator = op;
                node.value = instruction;
                irTree.current.setLastInstruction(instruction);
            } else {
                Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[op.ordinal()], instruction);
                if (duplicate != null) {
                    instruction = duplicate;
                    irTree.current.instructions.remove(irTree.current.instructions.size() - 1);
                }
                Operand firsrOp = new Operand(false, 0, instruction);
                Instruction newInstruction = new Instruction(op, firsrOp, null);
                node.value = newInstruction;
                irTree.current.instructions.add(newInstruction);
                if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                    lastIndex = irTree.current.instructions.size() - 1;
                    Instruction initInstruction = new Instruction(null, null, null);
                    irTree.current.instructions.add(initInstruction);
                }
            }
            node.previous = irTree.current.dominatorTree[op.ordinal()];
            irTree.current.dominatorTree[op.ordinal()] = node;
            Factor(irTree);
        }
        if (lastIndex != -1) {
            Instruction instruction = irTree.current.getAnyInstruction(lastIndex);
            Instruction lastInstruction = irTree.current.getLastInstruction();
            Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[lastInstruction.operator.ordinal()], lastInstruction);
            if (duplicate != null) {
                lastInstruction = duplicate;
                irTree.current.instructions.remove(irTree.current.instructions.size() - 1);
            }
            Operand op = new Operand(false, 0, lastInstruction);
            instruction.secondOp = op;
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
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ")");
            }

        } else if (token.kind == TokenKind.identity || token.kind == TokenKind.number) {
            Operand op = new Operand();
            if (token.kind == TokenKind.identity) {
                //identity
                Instruction valueGenerator = irTree.current.valueInstructionMap.get(token.id);
                if (valueGenerator == null) {
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                }
                op = new Operand(false, 0, valueGenerator);
            }
            if (token.kind == TokenKind.number) {
                //num
                op = new Operand(true, token.val, null);
            }
            Instruction instruction = irTree.current.getLastInstruction();
            if (instruction == null) {
                instruction = new Instruction(null, op, null);
                irTree.current.instructions.add(instruction);
            } else if (instruction.operator == null) {
                instruction.firstOp = op;
                irTree.current.setLastInstruction(instruction);
            } else if (instruction.operator != null && instruction.secondOp == null) {
                instruction.secondOp = op;
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

    private void warning(String message) {
        System.out.println(message);
    }

    private Instruction getDuplicateInstruction(InstructionLinkedList list, Instruction instruction) {
        InstructionLinkedList tail = list;
        while (tail != null) {
            if (sameInstruction(tail.value, instruction)) {
                break;
            }
            tail = tail.previous;
        }
        if (tail == null) {
            return null;
        }
        return tail.value;
    }

    private boolean sameInstruction(Instruction first, Instruction second) {
        if (sameOperand(first.firstOp, second.firstOp) && sameOperand(first.secondOp, second.secondOp)) {
            return true;
        } else if (sameOperand(first.secondOp, second.firstOp) && sameOperand(first.firstOp, second.secondOp)) {
            return true;
        }
        return false;
    }

    private boolean sameOperand(Operand first, Operand second) {
        if (first.constant == second.constant && first.constVal == second.constVal && first.valGenerator == second.valGenerator) {
            return true;
        }
        return false;
    }
}
