import java.io.IOException;
import java.util.*;

public class Parser {
    Lexer lexer;
    Token token;
    Instruction assignZeroInstruction;
    Operand zeroOperand;

    public Parser(String fileName) throws IOException, SyntaxException {
        this.lexer = new Lexer(fileName);
        token = lexer.nextToken();
        zeroOperand = new Operand(true, 0, null, -1);
        assignZeroInstruction = new Instruction(Operators.constant, zeroOperand, zeroOperand);
    }

    IntermediateTree getIntermediateRepresentation() throws SyntaxException, IOException {
        IntermediateTree intermediateTree = new IntermediateTree();
        InstructionLinkedList node = new InstructionLinkedList();
        node.previous = null;
        node.value = assignZeroInstruction;
        intermediateTree.current.dominatorTree[Operators.constant.ordinal()] = node;
        Computation(intermediateTree);
        return intermediateTree;
    }

    public void Computation(IntermediateTree irTree) throws SyntaxException, IOException {
        //order:
        // main, varDecl, funcDecl,"{", statsequence, "}", "."

        if (token.kind != TokenKind.reservedSymbol && token.id != ReservedWords.mainDefaultId.ordinal()) {
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
//            if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.elseDefaultId.ordinal() || token.id == ReservedWords.fiDefaultId.ordinal() || token.id == ReservedWords.odDefaultId.ordinal())) {
//                break; //if ; was the last optional terminating semicolon before else, or fi, or od stop parsing
//            }
            statement(irTree);
        }
    }

    private void statement(IntermediateTree irTree) throws SyntaxException, IOException {
        //possibilities: let, call, if, while, return
        if (token.kind != TokenKind.reservedWord) {
            //throw error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "reserved word");
        }
        if (token.id == ReservedWords.fiDefaultId.ordinal() || token.id == ReservedWords.odDefaultId.ordinal() || token.id == ReservedWords.elseDefaultId.ordinal()) {
            //token = lexer.nextToken();
            return;
        }
        if (token.id == ReservedWords.letDefaultId.ordinal()) {
            token = lexer.nextToken();
            assignment(irTree);
        } else if (token.id == ReservedWords.callDefaultId.ordinal()) {
            token = lexer.nextToken();
            funcCall(irTree);
        } else if (token.id == ReservedWords.ifDefaultId.ordinal()) {
            token = lexer.nextToken();
            IfStatement(irTree);
        } else if (token.id == ReservedWords.whileDefaultId.ordinal()) {
            token = lexer.nextToken();
            whileStatement(irTree);
        } else if (token.id == ReservedWords.returnDefaultId.ordinal()) {
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
//        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.callDefaultId.ordinal()) {
//            token = lexer.nextToken();
//            Operand op = funcCall(irTree);
//        }
        Operand op = Expression(irTree);
        //create phi instruction in parent and use that value for assignment
        if (irTree.current.whileBlock) {
            BasicBlock parent = irTree.current.parentBlocks.get(0);
            if (parent.declaredVariables.contains(left.id)) {
                if (!irTree.current.instructions.contains(op.valGenerator)) {
                    Instruction newInstruction = new Instruction(op.valGenerator);
                    op.valGenerator = newInstruction;
                    irTree.current.instructions.add(op.valGenerator);
                }
                Instruction valueGenerator = createPhiInstructionSingleVar(irTree, left.id, op);
                op = new Operand(op.constant, op.constVal, valueGenerator, op.id);
                updateBlockInstructions(parent, left.id, valueGenerator);
                updateBlockInstructions(irTree.current, left.id, valueGenerator);
            }
        }
        irTree.current.instructionValueMap.put(op.valGenerator, left.id);
        irTree.current.valueInstructionMap.put(left.id, op.valGenerator);
        irTree.current.assignedVariables.add(left.id);
    }

    private Operand funcCall(IntermediateTree irTree) throws SyntaxException, IOException { //do later

        if (token.kind == TokenKind.reservedWord) {
            if (token.id == ReservedWords.InputNumDefaultId.ordinal()) {
                Operators ops = Operators.read;
                Instruction readInstr = new Instruction(ops);

                irTree.current.instructions.add(readInstr);
//                InstructionLinkedList node = new InstructionLinkedList();
//                node.value = readInstr;
//                node.previous = irTree.current.dominatorTree[ops.ordinal()];
//                irTree.current.dominatorTree[ops.ordinal()] = node;
                token = lexer.nextToken();
                if(token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()){
                    token = lexer.nextToken();
                }

                Operand ret = new Operand(true, -1, readInstr, -1);
                return ret;
                //return operand
            } else if (token.id == ReservedWords.OutputNumDefaultId.ordinal()) {
                Operators ops = Operators.write;
                token = lexer.nextToken();
                token = lexer.nextToken();
                Operand writtenNum;
                if (token.kind == TokenKind.number) {
                    writtenNum = new Operand(true, token.val, null, -1);
                    Instruction constantInstruction = new Instruction(Operators.constant, writtenNum, writtenNum);
                    Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.constant.ordinal()], constantInstruction);
                    if (duplicate != null) {
                        writtenNum.valGenerator = duplicate;
                    } else {

                        irTree.constants.instructions.add(constantInstruction);
//                        Instruction.instrNum++;

                        writtenNum = new Operand(true, token.val, constantInstruction, -1);
                        InstructionLinkedList node = new InstructionLinkedList();
                        node.value = constantInstruction;
                        node.previous = irTree.current.dominatorTree[Operators.constant.ordinal()];
                        irTree.current.dominatorTree[Operators.constant.ordinal()] = node;
                    }
                    Instruction write = new Instruction(ops, writtenNum);

                    irTree.current.instructions.add(write);
//                    InstructionLinkedList node = new InstructionLinkedList();
//                    node.value = write;
//                    node.previous = irTree.current.dominatorTree[ops.ordinal()];
//                    irTree.current.dominatorTree[ops.ordinal()] = node;
                    //put in block
                } else if (token.kind == TokenKind.identity) {
                    Operand varWrite;
                    if (!irTree.current.declaredVariables.contains(token.id)) {
                        error(ErrorInfo.UNDECLARED_VARIABLE_PARSER_ERROR, "");
                    }
                    Instruction valueGenerator = irTree.current.valueInstructionMap.get(token.id);
                    if (valueGenerator == null) {
                        warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                        valueGenerator = assignZeroInstruction;
                    }
                    varWrite = new Operand(false, 0, valueGenerator, token.id);
                    Instruction write = new Instruction(ops, varWrite);

                    irTree.current.instructions.add(write);

//                    InstructionLinkedList node = new InstructionLinkedList();
//                    node.value = write;
//                    node.previous = irTree.current.dominatorTree[ops.ordinal()];
//                    irTree.current.dominatorTree[ops.ordinal()] = node;
                    //put in block
                }
                token = lexer.nextToken();
                token = lexer.nextToken();
            } else if (token.id == ReservedWords.OutputNewLineDefaultId.ordinal()) {
                Operators ops = Operators.writeNL;
                Instruction writeNLInstr = new Instruction(ops);

                irTree.current.instructions.add(writeNLInstr);
//                InstructionLinkedList node = new InstructionLinkedList();
//                node.value = writeNLInstr;
//                node.previous = irTree.current.dominatorTree[ops.ordinal()];
//                irTree.current.dominatorTree[ops.ordinal()] = node;
                token = lexer.nextToken();
                if(token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()){
                    token = lexer.nextToken();
                }
            } else {
                //error
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "predefined function");
            }
        }
        return null;
    }

    private void IfStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        relation(irTree);
        BasicBlock parentBlock = irTree.current;
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.thenDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock thenBlock = new BasicBlock();
            thenBlock.valueInstructionMap.putAll(parentBlock.valueInstructionMap);
            thenBlock.dominatorTree = parentBlock.dominatorTree.clone();
            thenBlock.declaredVariables.addAll(parentBlock.declaredVariables);
            thenBlock.parentBlocks.add(parentBlock);
            parentBlock.childBlocks.add(thenBlock);
            irTree.current = thenBlock;

//            thenBlock.dominatorBlocks = parentBlock.dominatorBlocks;
//            thenBlock.dominatorBlocks.add(parentBlock);

            statSequence(irTree);

            if(thenBlock.instructions.isEmpty()){
                Instruction emptyInstr = new Instruction(Operators.empty);
                thenBlock.instructions.add(emptyInstr);
            }
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "then");
        }
        BasicBlock elseBlock = new BasicBlock();
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.elseDefaultId.ordinal()) {
            token = lexer.nextToken();
            elseBlock.valueInstructionMap.putAll(parentBlock.valueInstructionMap);
            elseBlock.dominatorTree = parentBlock.dominatorTree.clone();
            elseBlock.declaredVariables.addAll(parentBlock.declaredVariables);
            elseBlock.parentBlocks.add(parentBlock);
            parentBlock.childBlocks.add(elseBlock);
            irTree.current = elseBlock;

//            elseBlock.dominatorBlocks = parentBlock.dominatorBlocks;
//            elseBlock.dominatorBlocks.add(parentBlock);

            statSequence(irTree);
        } else { //empty else block
            elseBlock.valueInstructionMap.putAll(parentBlock.valueInstructionMap);
            elseBlock.dominatorTree = parentBlock.dominatorTree.clone();
            elseBlock.declaredVariables.addAll(parentBlock.declaredVariables);
            irTree.current = irTree.current.parentBlocks.get(0);
            elseBlock.parentBlocks.add(parentBlock);
            parentBlock.childBlocks.add(elseBlock);
            irTree.current = elseBlock;

//            elseBlock.dominatorBlocks = parentBlock.dominatorBlocks;
//            elseBlock.dominatorBlocks.add(parentBlock);
        }

        if(elseBlock.instructions.isEmpty()){
            Instruction emptyInstr = new Instruction(Operators.empty);
            elseBlock.instructions.add(emptyInstr);
        }
        Instruction firstInstr = elseBlock.instructions.get(0);
        Operand op = new Operand(false, 0, firstInstr, -1);
        Instruction branch = parentBlock.getLastInstruction();
        branch.secondOp = op;
        parentBlock.setLastInstruction(branch);

        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.fiDefaultId.ordinal()) {
            token = lexer.nextToken();
            BasicBlock joinBlock = new BasicBlock();
            irTree.current = parentBlock;
            //irTree.current = irTree.current.parentBlocks.get(0);
            for (int i = 0; i < irTree.current.childBlocks.size(); i++) {
                BasicBlock block = irTree.current.childBlocks.get(i);
                joinBlock.parentBlocks.add(block);
                block.childBlocks.add(joinBlock);
                //get all variables which were assigned in if block and else block
                joinBlock.assignedVariables.addAll(block.assignedVariables);
                joinBlock.declaredVariables.addAll(block.declaredVariables);
                joinBlock.valueInstructionMap.putAll(block.valueInstructionMap);
            }
            //create phi instructions for changed variables
            int lastIndex = Math.max((joinBlock.instructions.size() - 1), 0);
            createPhiInstructions(joinBlock, lastIndex);
            irTree.current = joinBlock;

//            joinBlock.dominatorBlocks = parentBlock.dominatorBlocks;
//            joinBlock.dominatorBlocks.add(parentBlock);


            if(joinBlock.instructions.isEmpty()){
                Instruction emptyInstr = new Instruction(Operators.empty);
                joinBlock.instructions.add(emptyInstr);
            }
            // create second operand to branch instruction
//            Instruction firstInstr = joinBlock.instructions.get(0);
//            Operand op = new Operand(false, 0, firstInstr, -1);
//            Instruction branch = parentBlock.getLastInstruction();
//            branch.secondOp = op;
//            parentBlock.setLastInstruction(branch);
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "fi");
        }
        //token = lexer.nextToken();
    }

    private void whileStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        BasicBlock condBlock = new BasicBlock();
        condBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
        condBlock.instructionValueMap.putAll(irTree.current.instructionValueMap);
        condBlock.dominatorTree = irTree.current.dominatorTree.clone();
        condBlock.declaredVariables.addAll(irTree.current.declaredVariables);
        condBlock.parentBlocks.add(irTree.current);
        condBlock.whileBlock=true;
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
        whileBlock.declaredVariables.addAll(irTree.current.declaredVariables);
        whileBlock.parentBlocks.add(irTree.current);
        whileBlock.whileBlock = true;

        irTree.current.childBlocks.add(whileBlock);
        irTree.current.parentBlocks.add(whileBlock);
        //need to add phi for current (condBlock)
        irTree.current = whileBlock;
        statSequence(irTree);

        // create second operand to branch instruction
        Instruction firstInstr = whileBlock.instructions.get(0);
        Operand op = new Operand(false, 0, firstInstr, -1);
        Instruction branch = whileBlock.parentBlocks.get(0).getLastInstruction();
        branch.secondOp = op;
        whileBlock.parentBlocks.get(0).setLastInstruction(branch);

        if (token.kind != TokenKind.reservedWord || token.id != ReservedWords.odDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "od");
        }

        BasicBlock newBlock = new BasicBlock();
        newBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
        newBlock.dominatorTree = irTree.current.dominatorTree.clone();
        newBlock.declaredVariables.addAll(irTree.current.declaredVariables);
        newBlock.parentBlocks.add(condBlock);
        condBlock.childBlocks.add(newBlock);
        irTree.current = newBlock;
        token = lexer.nextToken();
    }

    private Operand returnStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        if (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.endingCurlyBracketDefaultId.ordinal() || token.id == ReservedWords.semicolonDefaultId.ordinal())) {
            return null;
        } else if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.elseDefaultId.ordinal() || token.id == ReservedWords.fiDefaultId.ordinal() || token.id == ReservedWords.odDefaultId.ordinal())) {
            return null;
        } else {
            return Expression(irTree);
        }
    }

    private void relation(IntermediateTree irTree) throws SyntaxException, IOException {
        Operand left, right;
        left = Expression(irTree);
        if (token.kind != TokenKind.relOp || token.id == ReservedWords.assignmentSymbolDefaultId.ordinal()) {
            //errror
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "relOp");
        } else {
            //handle relOp
            Token relOp = token;
            Operators ops = Operators.cmp;

            token = lexer.nextToken();
            right = Expression(irTree);
            Instruction cmp = new Instruction(ops, left, right);
            irTree.current.instructions.add(cmp);
//            Instruction.instrNum++;
//            Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[ops.ordinal()], cmp);
//
//            if (duplicate != null) {
//                cmp = duplicate;
//                irTree.current.instructions.remove(irTree.current.instructions.size() - 1);
//            } else {
//                irTree.current.instructions.add(cmp);
//                InstructionLinkedList node = new InstructionLinkedList();
//                node.value = cmp;
//                node.previous = irTree.current.dominatorTree[ops.ordinal()];
//                irTree.current.dominatorTree[ops.ordinal()] = node;
//            }
            Operand opcmp = new Operand(false, 0, cmp, -1);
            if (relOp.id == ReservedWords.equalToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.beq, opcmp, null);
                irTree.current.instructions.add(branch);
//                Instruction.instrNum++;
            } else if (relOp.id == ReservedWords.notEqualToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.bne, opcmp, null);
                irTree.current.instructions.add(branch);
//                Instruction.instrNum++;
            } else if (relOp.id == ReservedWords.lessThanDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.blt, opcmp, null);
                irTree.current.instructions.add(branch);
//                Instruction.instrNum++;
            } else if (relOp.id == ReservedWords.lessThanOrEqualToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.ble, opcmp, null);
                irTree.current.instructions.add(branch);
//                Instruction.instrNum++;
            } else if (relOp.id == ReservedWords.greaterThanDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.bgt, opcmp, null);
                irTree.current.instructions.add(branch);
//                Instruction.instrNum++;
            } else if (relOp.id == ReservedWords.greaterThanOrEqualToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.bge, opcmp, null);
                irTree.current.instructions.add(branch);
//                Instruction.instrNum++;
            }
        }


    }

    private void varDecl(IntermediateTree irTree) throws SyntaxException, IOException {
//        if (token.id == ReservedWords.varDefaultId.ordinal()) {
//        } //for when we do array

        token = lexer.nextToken();
        if (token.kind != TokenKind.identity) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
        }
        irTree.current.declaredVariables.add(token.id);
        token = lexer.nextToken();
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.commaDefaultId.ordinal())) {
            token = lexer.nextToken();
            if (token.kind != TokenKind.identity) {
                //error
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "ident");
            }
            irTree.current.declaredVariables.add(token.id);
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

    private Operand Expression(IntermediateTree irTree) throws IOException, SyntaxException {
        Operand left, right;
        left = Term(irTree);
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.plusDefaultId.ordinal() || token.id == ReservedWords.minusDefaultId.ordinal())) {
            Operators operator;
            if (token.id == ReservedWords.plusDefaultId.ordinal()) {
                operator = Operators.add;
            } else {
                operator = Operators.sub;
            }
            token = lexer.nextToken();
            right = Term(irTree);
            left = Compute(irTree, operator, left, right);
        }
        return left;
    }

    private Operand Term(IntermediateTree irTree) throws SyntaxException, IOException {
        Operand left, right;
        left = Factor(irTree);

        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.mulDefaultId.ordinal() || token.id == ReservedWords.divDefaultId.ordinal())) {
            Operators operator;
            if (token.id == ReservedWords.mulDefaultId.ordinal()) {
                operator = Operators.mul;
            } else {
                operator = Operators.div;
            }
            token = lexer.nextToken();
            right = Factor(irTree);
            left = Compute(irTree, operator, left, right);
        }

        return left;
    }

    private Operand Compute(IntermediateTree irTree, Operators operator, Operand left, Operand right) {
        Instruction instruction = new Instruction(operator, left, right);
        if (!irTree.current.whileBlock) {
            Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[operator.ordinal()], instruction);
            if (duplicate != null) {
                instruction = duplicate;
            } else {
                irTree.current.instructions.add(instruction);
//                Instruction.instrNum++;
                InstructionLinkedList node = new InstructionLinkedList();
                node.value = instruction;
                node.previous = irTree.current.dominatorTree[operator.ordinal()];
                irTree.current.dominatorTree[operator.ordinal()] = node;
            }
        }
        //both operands constant
        int id = -1;
        //both operands variables/instructions
        if (!left.constant && !right.constant) {
            id = -3;
        }
        // one of the operands variable/instruction
        if ((left.constant && !right.constant) || (!left.constant && right.constant)) {
            id = -2;
        }
        Operand op = new Operand(false, 0, instruction, id);
        return op;
        //op.valGenerator=constantInstruction;
    }

    private Operand Factor(IntermediateTree irTree) throws SyntaxException, IOException {
        Operand result = new Operand();

        //negation
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.minusDefaultId.ordinal()) {
            token = lexer.nextToken();
            if (token.kind == TokenKind.number) {
//                int index = irTree.current.instructions.size();
                result = new Operand(true, token.val, null, -1);
                Instruction constantInstruction = new Instruction(Operators.constant, result, result);
                Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.constant.ordinal()], constantInstruction);
                if (duplicate != null) {
                    result.valGenerator = duplicate;
                } else {
                    irTree.constants.instructions.add(constantInstruction);
//                    Instruction.instrNum++;

                    result = new Operand(true, token.val, constantInstruction, -1);
                    //op.valGenerator=constantInstruction;
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = constantInstruction;
                    node.previous = irTree.current.dominatorTree[Operators.constant.ordinal()];
                    irTree.current.dominatorTree[Operators.constant.ordinal()] = node;
                }

                Instruction negInstr = new Instruction(Operators.neg, result);
                result = new Operand(true, -token.val, negInstr, -1);
                duplicate = getDuplicateInstructionSingleOp(irTree.current.dominatorTree[Operators.neg.ordinal()], negInstr);
                if (duplicate != null) {
                    result.valGenerator = duplicate;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = negInstr;
                    node.previous = irTree.current.dominatorTree[Operators.neg.ordinal()];
                    irTree.current.dominatorTree[Operators.neg.ordinal()] = node;
                    irTree.current.instructions.add(negInstr);
//                    Instruction.instrNum++;
                }
            } else if (token.kind == TokenKind.identity) {
                if (!irTree.current.declaredVariables.contains(token.id)) {
                    error(ErrorInfo.UNDECLARED_VARIABLE_PARSER_ERROR, "");
                }
                Instruction valueGenerator = irTree.current.valueInstructionMap.get(token.id);
                if (valueGenerator == null) {
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                    valueGenerator = assignZeroInstruction;
                }
//                irTree.current.instructions.add(valueGenerator);
                result = new Operand(false, 0, valueGenerator, token.id);
                Instruction negInstr = new Instruction(Operators.neg, result);
                result = new Operand(false, 0, negInstr, token.id);
                Instruction duplicate = getDuplicateInstructionSingleOp(irTree.current.dominatorTree[Operators.neg.ordinal()], negInstr);
                if (duplicate != null) {
                    result.valGenerator = duplicate;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = negInstr;
                    node.previous = irTree.current.dominatorTree[Operators.neg.ordinal()];
                    irTree.current.dominatorTree[Operators.neg.ordinal()] = node;
                    irTree.current.instructions.add(negInstr);
//                    Instruction.instrNum++;
                }
            } else if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                token = lexer.nextToken();
                result = Expression(irTree);
                Instruction negInstr = new Instruction(Operators.neg, result);
                result = new Operand(false, 0, negInstr, -1);
                Instruction duplicate = getDuplicateInstructionSingleOp(irTree.current.dominatorTree[Operators.neg.ordinal()], negInstr);
                if (duplicate != null) {
                    result.valGenerator = duplicate;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = negInstr;
                    node.previous = irTree.current.dominatorTree[Operators.neg.ordinal()];
                    irTree.current.dominatorTree[Operators.neg.ordinal()] = node;
                    irTree.current.instructions.add(negInstr);
//                    Instruction++;
                }
            }
        }

        //token = lexer.nextToken();


        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
            token = lexer.nextToken();
            result = Expression(irTree);
            if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.endingFirstBracketDefaultId.ordinal()) {
                //end expression
                token = lexer.nextToken();
            } else {
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ")");
            }

        } else if (token.kind == TokenKind.identity || token.kind == TokenKind.number) {
            int index = irTree.current.instructions.size();
            if (token.kind == TokenKind.identity) {
                //identity
                if (!irTree.current.declaredVariables.contains(token.id)) {
                    error(ErrorInfo.UNDECLARED_VARIABLE_PARSER_ERROR, "");
                }
                Instruction valueGenerator = irTree.current.valueInstructionMap.get(token.id);
                if (valueGenerator == null) {
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                    valueGenerator = assignZeroInstruction;
                }
                if (irTree.current.whileBlock) {
                    valueGenerator = new Instruction(valueGenerator);
                }
                result = new Operand(false, 0, valueGenerator, token.id);
            }
            if (token.kind == TokenKind.number) {
                //num
                result = new Operand(true, token.val, null, -1);
                Instruction constantInstruction = new Instruction(Operators.constant, result, result);
                Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.constant.ordinal()], constantInstruction);
                if (duplicate != null) {
                    result.valGenerator = duplicate;
                } else {
                    irTree.constants.instructions.add(constantInstruction);
//                    Instruction.instrNum++;

                    result = new Operand(true, token.val, constantInstruction, -1);
                    //op.valGenerator=constantInstruction;
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = constantInstruction;
                    node.previous = irTree.current.dominatorTree[Operators.constant.ordinal()];
                    irTree.current.dominatorTree[Operators.constant.ordinal()] = node;
                }
            }
            token = lexer.nextToken();
        } else if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.callDefaultId.ordinal()) {
            token = lexer.nextToken();
            result = funcCall(irTree);
            if(token.id != ReservedWords.semicolonDefaultId.ordinal()){
                token = lexer.nextToken();
            }
        }
        return result;
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

    private InstructionLinkedList removeChangedInstruction(InstructionLinkedList list, Instruction instruction) {
        InstructionLinkedList next = list;
        InstructionLinkedList tail = list.previous;
        if (sameInstruction(list.value, instruction)) {
            list = list.previous;
            return list;
        }
        while (tail != null) {
            if (sameInstruction(tail.value, instruction)) {
                break;
            }
            tail = tail.previous;
        }
        if (tail == null) {
            return list;
        }
        next.previous = tail.previous;
        return list;
    }

    private boolean sameInstruction(Instruction first, Instruction second) {
        if (sameOperand(first.firstOp, second.firstOp) && sameOperand(first.secondOp, second.secondOp)) {
            return true;
        } else if (sameOperand(first.secondOp, second.firstOp) && sameOperand(first.firstOp, second.secondOp) && (first.operator != Operators.sub)) {
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

    private Instruction getDuplicateInstructionSingleOp(InstructionLinkedList list, Instruction instruction) {
        InstructionLinkedList tail = list;
        while (tail != null) {
            if (sameInstructionSingleOp(tail.value, instruction)) {
                break;
            }
            tail = tail.previous;
        }
        if (tail == null) {
            return null;
        }
        return tail.value;
    }

    private boolean sameInstructionSingleOp(Instruction first, Instruction second) {
        if (sameOperand(first.firstOp, second.firstOp)) {
            return true;
        }
        return false;
    }

    private void createPhiInstructions(BasicBlock joinBlock, int index) {
        Map<Integer, Instruction> updatedMap = new HashMap<>();
        for (int i = 0; i < joinBlock.parentBlocks.size(); i++) {
            BasicBlock parentBlock = joinBlock.parentBlocks.get(i);
            if (parentBlock.nestedValueInstructionMap.size() > 0) {
                for (int identity : parentBlock.nestedValueInstructionMap.keySet()) {
                    Operand firstOp = new Operand(false, 0, parentBlock.nestedValueInstructionMap.get(identity), identity);
                    Operand secondOp = new Operand(false, 0, parentBlock.valueInstructionMap.get(identity), identity);
                    Instruction phiInstruction = new Instruction(Operators.phi, firstOp, secondOp);
                    joinBlock.instructions.add(phiInstruction);
//                    Instruction.instrNum++;
                    parentBlock.nestedValueInstructionMap.put(identity, phiInstruction);
                    Instruction anotherPhi = updatedMap.get(identity);
                    if(anotherPhi != null){
                        Operand oldPhi = new Operand(false, 0, anotherPhi, identity);
                        Operand newPhi = new Operand(false, 0, phiInstruction, identity);
                        phiInstruction = new Instruction(Operators.phi, oldPhi, newPhi);
                        joinBlock.instructions.add(phiInstruction);
//                        Instruction.instrNum++;
                    }
                    joinBlock.valueInstructionMap.put(identity, phiInstruction);
                    updatedMap.put(identity, phiInstruction);
                }
            }
        }

        for (Integer identity : joinBlock.assignedVariables) {
            BasicBlock ifParent = joinBlock.parentBlocks.get(0);
            Instruction ifValueGenerator = ifParent.nestedValueInstructionMap.get(identity);
            if (ifValueGenerator == null) {
                ifValueGenerator = ifParent.valueInstructionMap.get(identity);
            }
            if (ifValueGenerator == null) {
                warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                ifValueGenerator = assignZeroInstruction;
            }
            Operand firstOp = new Operand(false, 0, ifValueGenerator, identity);

            BasicBlock elseParent = joinBlock.parentBlocks.get(1);
            Instruction elseValueGenerator = elseParent.nestedValueInstructionMap.get(identity);
            if (elseValueGenerator == null) {
                elseValueGenerator = elseParent.valueInstructionMap.get(identity);
            }
            if (elseValueGenerator == null) {
                warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                elseValueGenerator = assignZeroInstruction;
            }
            Operand secondOp = new Operand(false, 0, elseValueGenerator, identity);
            Instruction phiInstruction = new Instruction(Operators.phi, firstOp, secondOp);
            joinBlock.instructions.add(phiInstruction);
//            Instruction.instrNum++;
            joinBlock.valueInstructionMap.put(identity, phiInstruction);
            updatedMap.put(identity, phiInstruction);
        }
        BasicBlock outerParent = joinBlock.parentBlocks.get(0).parentBlocks.get(0);
        outerParent.nestedValueInstructionMap = updatedMap;
    }

    private Instruction createPhiInstructionSingleVar(IntermediateTree irTree, int identity, Operand whileOp) {
        BasicBlock whileBlock = irTree.current;
        BasicBlock condBlock = whileBlock.parentBlocks.get(0);
        Instruction valueGenerator = condBlock.valueInstructionMap.get(identity);
        if (valueGenerator == null) {
            warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
            valueGenerator = assignZeroInstruction;
        }
        Operand condOp = new Operand(false, 0, valueGenerator, identity);
        Instruction phiInstruction = new Instruction(Operators.phi, condOp, whileOp);
        condBlock.instructions.add(condBlock.phiIndex, phiInstruction);
//        Instruction.instrNum++;
        condBlock.phiIndex++;
        condBlock.valueInstructionMap.put(identity, phiInstruction);
        return phiInstruction;
    }

    private void updateBlockInstructions(BasicBlock block, int identity, Instruction newValueGenerator) {
        for (int i = block.phiIndex; i < block.instructions.size(); i++) {
            Instruction oldInstruction = block.instructions.get(i);
            Instruction updatedInstruction = updateInstruction(block, oldInstruction, identity, newValueGenerator);
            block.instructions.set(i, updatedInstruction);
//            if(oldInstruction.operator.ordinal()<=6){
//                block.dominatorTree[oldInstruction.operator.ordinal()] = removeChangedInstruction(block.dominatorTree[oldInstruction.operator.ordinal()], oldInstruction);
//            }
        }
    }

    private Instruction updateInstruction(BasicBlock block, Instruction oldInstruction, int identity, Instruction newValueGenerator) {
        if (oldInstruction.operator == Operators.constant) {
            return oldInstruction;
        }
        Operand firstOp, secondOp;
        if (!oldInstruction.firstOp.constant) {
            firstOp = updateOperand(block, oldInstruction.firstOp, identity, newValueGenerator);
        } else {
            firstOp = oldInstruction.firstOp;
        }
        if (oldInstruction.secondOp != null && !oldInstruction.secondOp.constant) {
            secondOp = updateOperand(block, oldInstruction.secondOp, identity, newValueGenerator);
        } else {
            secondOp = oldInstruction.secondOp;
        }
        Instruction updatedInstruction = new Instruction(oldInstruction.operator, firstOp, secondOp);
//        if(oldInstruction.operator.ordinal()<=6){
//            Instruction duplicate = getDuplicateInstruction(block.dominatorTree[oldInstruction.operator.ordinal()], updatedInstruction);
//            if (duplicate != null) {
//                updatedInstruction = duplicate;
//            } else {
//                InstructionLinkedList node = new InstructionLinkedList();
//                node.value = updatedInstruction;
//                node.previous = block.dominatorTree[oldInstruction.operator.ordinal()];
//                block.dominatorTree[oldInstruction.operator.ordinal()] = node;
//            }
//        }
        Collection<Integer> usageVariables = block.instructionValueMap.get(oldInstruction);
        for (int id : usageVariables) {
            block.valueInstructionMap.put(id, updatedInstruction);
        }
        return updatedInstruction;
    }

    private Operand updateOperand(BasicBlock block, Operand operand, int identity, Instruction newValueGenerator) {
        Instruction valueGenerator;
        if (operand.id == identity) {
            valueGenerator = newValueGenerator;
        } else if (operand.id < 0) {
            valueGenerator = updateInstruction(block, operand.valGenerator, identity, newValueGenerator);
        } else {
            valueGenerator = operand.valGenerator;
        }
        operand.valGenerator = valueGenerator;
        return operand;
//        Operand updatedOperand = new Operand(false, 0, valueGenerator, identity);
//        return updatedOperand;
    }
}
