import java.io.IOException;
import java.util.*;

public class Parser {
    Lexer lexer;
    Token token;
    Instruction assignZeroInstruction;
    Operand zeroOperand;
    Set<Integer> visitedBlocks;
    Map<Integer, Function> functionInfo;

    public Parser(String fileName) throws IOException, SyntaxException {
        this.lexer = new Lexer(fileName);
        token = lexer.nextToken();
        zeroOperand = new Operand(true, 0, null, -1);
        assignZeroInstruction = new Instruction(Operators.constant, zeroOperand, zeroOperand);
        visitedBlocks = new HashSet<>();
        functionInfo = new HashMap<>();
    }

    IntermediateTree getIntermediateRepresentation() throws SyntaxException, IOException {
        IntermediateTree intermediateTree = new IntermediateTree();
        InstructionLinkedList node = new InstructionLinkedList();
        node.previous = null;
        node.value = assignZeroInstruction;
        intermediateTree.constants.dominatorTree[Operators.constant.ordinal()] = node;
        intermediateTree.constants.instructions.add(assignZeroInstruction);
        intermediateTree.constants.instructionIDs.add(assignZeroInstruction.IDNum);
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
        if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.voidDefaultId.ordinal())) {
            token = lexer.nextToken();
            if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.functionDefaultId.ordinal())) {
                boolean isVoid = true;
                token = lexer.nextToken();
                funcDecl(isVoid);
            } else {
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "void");
            }
        }

        if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.functionDefaultId.ordinal())) {
            token = lexer.nextToken();
            boolean isVoid = false;
            funcDecl(isVoid);
        }

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

    private void funcDecl(boolean isVoid) throws SyntaxException, IOException {
        if (token.kind != TokenKind.identity) {
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
        }
        int identity = token.id;
        Function function = new Function(isVoid);
        IntermediateTree irTree = function.irTree;
        functionInfo.put(identity, function);
        token = lexer.nextToken();
        if (token.kind != TokenKind.identity) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
        }
        function.formalParameters.add(token.id);
        irTree.current.declaredVariables.add(token.id);
        token = lexer.nextToken();
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.commaDefaultId.ordinal())) {
            token = lexer.nextToken();
            if (token.kind != TokenKind.identity) {
                //error
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
            }
            function.formalParameters.add(token.id);
            irTree.current.declaredVariables.add(token.id);
            token = lexer.nextToken();
            //store ident?
        }
        //need to check if var or array
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.semicolonDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ";");
        }
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingCurlyBracketDefaultId.ordinal()) {
            token = lexer.nextToken();
            funcBody(irTree); //parse statement sequence
        } else {
            //if no {, error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "{");
        }
    }

    private void funcBody(IntermediateTree irTree) throws SyntaxException, IOException {
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
    }

    private void statSequence(IntermediateTree irTree) throws SyntaxException, IOException {
        statement(irTree);
        while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.semicolonDefaultId.ordinal()) {
            token = lexer.nextToken();
            if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.endingCurlyBracketDefaultId.ordinal()) {
                break; //if ; was the last optional terminating semicolon before ; stop parsing
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
        if (token.id == ReservedWords.fiDefaultId.ordinal() || token.id == ReservedWords.odDefaultId.ordinal() || token.id == ReservedWords.elseDefaultId.ordinal()) {
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
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingThirdBracketDefaultId.ordinal()) {
            arrayDesignator(irTree, left);

        }
        if (token.kind != TokenKind.relOp && token.id != ReservedWords.assignmentSymbolDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "->");
        }
        token = lexer.nextToken();
        Operand op = Expression(irTree);
        irTree.current.valueInstructionMap.put(left.id, op.returnVal);
        irTree.current.assignedVariables.add(left.id);
        if (irTree.current.makeDuplicate) {
            BasicBlock parent = irTree.current.parentBlocks.get(0);
            if (parent.declaredVariables.contains(left.id)) {
                if (!irTree.current.instructionIDs.contains(op.valGenerator)) {
                    Instruction newInstruction = new Instruction(op.returnVal);
                    op.valGenerator = newInstruction.IDNum;
                    irTree.current.instructions.add(newInstruction);
                    irTree.current.instructionIDs.add(newInstruction.IDNum);
                    irTree.current.valueInstructionMap.put(left.id, newInstruction);
                }
            }
        }

        //create phi instruction in parent and use that value for assignment
        if (irTree.current.whileBlock && !irTree.current.isCond) {
            int i = irTree.current.nested;
            BasicBlock condd = irTree.current.condBlock;
            Instruction phi = makeWhilePhiDirectCond(irTree, irTree.current, condd, op, left.id); // creat or update phi in the direct while condblock
            i--;
            while (i > 0) {
                //put the phi instr that is in the nested cond in all the upper cond blocks
                makeWhilePhiNested(irTree, condd, condd.condBlock, phi, left.id);
                i--;
                condd = condd.condBlock;
            }
        }
    }

    private void makeWhilePhiNested(IntermediateTree irTree, BasicBlock condBlock, BasicBlock putInThisCondd, Instruction phi, int id) {
        Instruction newPhi;
        Instruction valueGenerator = putInThisCondd.valueInstructionMap.get(id);
        Operand op = new Operand(false, -1, phi.IDNum, id);
        if (valueGenerator == null) {
            warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
            valueGenerator = assignZeroInstruction;
        }
        Operand firstop = new Operand(false, 0, valueGenerator.IDNum, id);
        if (valueGenerator.operator == Operators.phi) {
            newPhi = valueGenerator;
            newPhi.secondOp = op;
        } else {
            newPhi = new Instruction(Operators.phi, firstop, op);
            putInThisCondd.instructions.add(0, newPhi);
            putInThisCondd.instructionIDs.add(newPhi.IDNum);
            putInThisCondd.valueInstructionMap.put(id, newPhi);
        }
        updateInstrWhile(irTree, putInThisCondd, newPhi);

    }

    private Instruction makeWhilePhiDirectCond(IntermediateTree irTree, BasicBlock current, BasicBlock condd, Operand op, int id) {
        Instruction phi;
        Instruction valueGenerator = condd.valueInstructionMap.get(id);
        if (valueGenerator == null) {
            warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
            valueGenerator = assignZeroInstruction;
        }
        Operand firstop = new Operand(false, 0, valueGenerator.IDNum, id);
        if (valueGenerator.operator == Operators.phi) { //if the phi already exists, update
            phi = valueGenerator;
            phi.secondOp = op;
        } else { // if it does not exist, create
            phi = new Instruction(Operators.phi, firstop, op);
            condd.instructions.add(0, phi);
            condd.instructionIDs.add(phi.IDNum);
            condd.valueInstructionMap.put(id, phi);
        }
        updateInstrWhile(irTree, condd, phi);
        return phi;
    }

    //update the instructions to use phi value, in all the nested if there is
    private void updateInstrWhile(IntermediateTree irTree, BasicBlock condd, Instruction phi) {
        ArrayList<BasicBlock> visited = new ArrayList<>();
        LinkedList<BasicBlock> toVisit = new LinkedList<>();
        visited.add(condd);
        //do condd here
        for (Instruction i : condd.instructions) { // change the cmp in original cond block
            if (i.operator != Operators.phi) {
                if (i.firstOp != null && i.firstOp.valGenerator == phi.firstOp.valGenerator && !i.firstOp.constant) {
                    i.firstOp.valGenerator = phi.IDNum;
                }
                if (i.secondOp != null && i.secondOp.valGenerator == phi.firstOp.valGenerator && !i.secondOp.constant) {
                    i.secondOp.valGenerator = phi.IDNum;
                }
            }
        }
        toVisit.add(condd.childBlocks.get(0));
        while (!toVisit.isEmpty()) {
            BasicBlock current = toVisit.poll();
            visited.add(current);
            for (Instruction i : current.instructions) { // updates in all the nested blocks
                if (i.firstOp != null && i.firstOp.valGenerator == phi.firstOp.valGenerator && !i.firstOp.constant) {
                    i.firstOp.valGenerator = phi.IDNum;
                }
                if (i.secondOp != null && i.secondOp.valGenerator == phi.firstOp.valGenerator && !i.secondOp.constant) {
                    i.secondOp.valGenerator = phi.IDNum;
                }
            }
            for (BasicBlock child : current.childBlocks) {
                if (!visited.contains(child)) {
                    toVisit.add(child);
                }
            }
        }
    }

    private Operand funcCall(IntermediateTree irTree) throws SyntaxException, IOException { //do later

        if (token.kind == TokenKind.reservedWord) {
            if (token.id == ReservedWords.InputNumDefaultId.ordinal()) {
                Operators ops = Operators.read;
                Instruction readInstr = new Instruction(ops);
                irTree.current.instructions.add(readInstr);
                irTree.current.instructionIDs.add(readInstr.IDNum);
                token = lexer.nextToken();
                if (token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                    token = lexer.nextToken();
                }

                Operand ret = new Operand(true, -1, readInstr.IDNum, -1);
                ret.returnVal = readInstr;
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
                    Instruction duplicate = getDuplicateInstruction(irTree.constants.dominatorTree[Operators.constant.ordinal()], constantInstruction);
                    if (duplicate != null) {
                        writtenNum.valGenerator = duplicate.IDNum;
                        Instruction.instrNum--;
                    } else {
                        irTree.constants.instructions.add(constantInstruction);
                        irTree.constants.instructionIDs.add(constantInstruction.IDNum);
                        writtenNum = new Operand(true, token.val, constantInstruction.IDNum, -1);
                        InstructionLinkedList node = new InstructionLinkedList();
                        node.value = constantInstruction;
                        node.previous = irTree.constants.dominatorTree[Operators.constant.ordinal()];
                        irTree.constants.dominatorTree[Operators.constant.ordinal()] = node;
                    }
                    Instruction write = new Instruction(ops, writtenNum);
                    irTree.current.instructions.add(write);
                    irTree.current.instructionIDs.add(write.IDNum);
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
                    varWrite = new Operand(false, 0, valueGenerator.IDNum, token.id);
                    Instruction write = new Instruction(ops, varWrite);
                    irTree.current.instructions.add(write);
                    irTree.current.instructionIDs.add(write.IDNum);
                }
                token = lexer.nextToken();
                token = lexer.nextToken();
            } else if (token.id == ReservedWords.OutputNewLineDefaultId.ordinal()) {
                Operators ops = Operators.writeNL;
                Instruction writeNLInstr = new Instruction(ops);
                irTree.current.instructions.add(writeNLInstr);
                irTree.current.instructionIDs.add(writeNLInstr.IDNum);
                token = lexer.nextToken();
                if (token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
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
        BasicBlock thenBlock = new BasicBlock();
        BasicBlock joinBlock = new BasicBlock();
        if (parentBlock.whileBlock) {
            thenBlock.makeDuplicate = true;
        }
        joinBlock.dominatorTree = parentBlock.dominatorTree.clone();
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.thenDefaultId.ordinal()) {
            token = lexer.nextToken();
            thenBlock.valueInstructionMap.putAll(parentBlock.valueInstructionMap);
            thenBlock.dominatorTree = parentBlock.dominatorTree.clone();
            thenBlock.declaredVariables.addAll(parentBlock.declaredVariables);
            thenBlock.parentBlocks.add(parentBlock);
            parentBlock.childBlocks.add(thenBlock);
            thenBlock.dominatorBlock = parentBlock;
            irTree.current = thenBlock;
            statSequence(irTree);

            joinBlock.parentBlocks.add(irTree.current);
            irTree.current.childBlocks.add(joinBlock);
            joinBlock.assignedVariables.addAll(irTree.current.assignedVariables);
            joinBlock.declaredVariables.addAll(irTree.current.declaredVariables);
            joinBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);

            if (thenBlock.instructions.isEmpty()) {
                Instruction emptyInstr = new Instruction(Operators.empty);
                thenBlock.instructions.add(emptyInstr);
                thenBlock.instructionIDs.add(emptyInstr.IDNum);
            }
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "then");
        }
        BasicBlock elseBlock = new BasicBlock();
        if (parentBlock.whileBlock) {
            elseBlock.makeDuplicate = true;
        }
        elseBlock.dominatorBlock = parentBlock;
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.elseDefaultId.ordinal()) {
            token = lexer.nextToken();
            elseBlock.valueInstructionMap.putAll(parentBlock.valueInstructionMap);
            elseBlock.dominatorTree = parentBlock.dominatorTree.clone();
            elseBlock.declaredVariables.addAll(parentBlock.declaredVariables);
            elseBlock.parentBlocks.add(parentBlock);
            parentBlock.childBlocks.add(elseBlock);
            irTree.current = elseBlock;
            statSequence(irTree);
        } else { //empty else block
            elseBlock.valueInstructionMap.putAll(parentBlock.valueInstructionMap);
            elseBlock.dominatorTree = parentBlock.dominatorTree.clone();
            elseBlock.declaredVariables.addAll(parentBlock.declaredVariables);
            irTree.current = irTree.current.parentBlocks.get(0);
            elseBlock.parentBlocks.add(parentBlock);
            parentBlock.childBlocks.add(elseBlock);
            irTree.current = elseBlock;
        }

        joinBlock.parentBlocks.add(irTree.current);
        irTree.current.childBlocks.add(joinBlock);
        joinBlock.assignedVariables.addAll(irTree.current.assignedVariables);
        joinBlock.declaredVariables.addAll(irTree.current.declaredVariables);
        joinBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);

        if (elseBlock.instructions.isEmpty()) {
            Instruction emptyInstr = new Instruction(Operators.empty);
            elseBlock.instructions.add(emptyInstr);
            elseBlock.instructionIDs.add(emptyInstr.IDNum);
        }
        Instruction firstInstr = elseBlock.instructions.get(0);
        Operand op = new Operand(false, 0, firstInstr.IDNum, -1);
        Instruction branch = parentBlock.getLastInstruction();
        branch.secondOp = op;
        parentBlock.setLastInstruction(branch);
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.fiDefaultId.ordinal()) {
            token = lexer.nextToken();
            joinBlock.dominatorBlock = parentBlock;
            irTree.current = parentBlock;
            for (int i = 0; i < irTree.current.childBlocks.size(); i++) {
                BasicBlock block = irTree.current.childBlocks.get(i);
                //get all variables which were assigned in if block and else block
                joinBlock.assignedVariables.addAll(block.assignedVariables);
                joinBlock.declaredVariables.addAll(block.declaredVariables);
                joinBlock.valueInstructionMap.putAll(block.valueInstructionMap);
            }
            //create phi instructions for changed variables
            createPhiInstructions(joinBlock);
            irTree.current = joinBlock;
            if (parentBlock.whileBlock) {
                for (int identity : joinBlock.assignedVariables) {
                    Operand phiOp = new Operand(false, 0, joinBlock.valueInstructionMap.get(identity).IDNum, identity);
                    Instruction updatedInstruction = createPhiInstructionSingleVar(parentBlock, identity, phiOp);
                    updateBlockInstructions(parentBlock, identity, updatedInstruction);
                }
            }
            if (joinBlock.instructions.isEmpty()) {
                Instruction emptyInstr = new Instruction(Operators.empty);
                joinBlock.instructions.add(emptyInstr);
                joinBlock.instructionIDs.add(emptyInstr.IDNum);
            }

            // create operand to branch bra instruction
            Instruction first = joinBlock.instructions.get(0);
            Operand opp = new Operand(false, 0, first.IDNum, -1);
            Instruction bra = new Instruction(Operators.bra, opp);
            joinBlock.parentBlocks.get(0).instructions.add(bra);
            joinBlock.parentBlocks.get(0).instructionIDs.add(bra.IDNum);
        } else {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "fi");
        }
    }

    private void whileStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        BasicBlock condBlock = new BasicBlock();
        if (irTree.current.instructions.isEmpty()) {
            irTree.current.instructions.add(new Instruction(Operators.empty));
        }
        condBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
        condBlock.dominatorTree = irTree.current.dominatorTree.clone();
        condBlock.declaredVariables.addAll(irTree.current.declaredVariables);
        condBlock.parentBlocks.add(irTree.current);
        irTree.current.childBlocks.add(condBlock);
        irTree.current = condBlock;

        condBlock.whileBlock = condBlock.parentBlocks.get(0).whileBlock;
        condBlock.nested = condBlock.parentBlocks.get(0).nested + 1;
        condBlock.isCond = true;
        condBlock.condBlock = condBlock.parentBlocks.get(0).condBlock;

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
        whileBlock.condBlock = condBlock;
        whileBlock.nested = condBlock.nested;

        irTree.current.childBlocks.add(whileBlock);

        //need to add phi for current (condBlock)
        irTree.current = whileBlock;
        statSequence(irTree);

        irTree.current.childBlocks.add(condBlock);
        condBlock.parentBlocks.add(irTree.current);

        if (token.kind != TokenKind.reservedWord || token.id != ReservedWords.odDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "od");
        }

        BasicBlock newBlock = new BasicBlock();
        if (condBlock.parentBlocks.get(0).whileBlock) {
            newBlock.whileBlock = true;
            newBlock.nestedBlock = true;
        }
        newBlock.assignedVariables = condBlock.assignedVariables;
        newBlock.valueInstructionMap.putAll(condBlock.valueInstructionMap);
        newBlock.dominatorTree = condBlock.dominatorTree.clone();
        newBlock.declaredVariables.addAll(condBlock.declaredVariables);
        newBlock.parentBlocks.add(condBlock);
        condBlock.childBlocks.add(newBlock);
        irTree.current = newBlock;
        token = lexer.nextToken();

        if (newBlock.instructions.isEmpty()) {
            Instruction emptyInstr = new Instruction(Operators.empty);
            newBlock.instructions.add(emptyInstr);
            newBlock.instructionIDs.add(emptyInstr.IDNum);
        }

        newBlock.nested = condBlock.nested - 1;
        newBlock.condBlock = condBlock.condBlock;
        newBlock.whileBlock = condBlock.whileBlock;

        Instruction firstInstr = newBlock.instructions.get(0);
        Operand ops = new Operand(false, 0, firstInstr.IDNum, -1);
        Instruction branchCond = condBlock.getLastInstruction();
        branchCond.secondOp = ops;
        condBlock.setLastInstruction(branchCond);

        firstInstr = condBlock.instructions.get(0);
        Operand op = new Operand(false, 0, firstInstr.IDNum, -1);
        Instruction branch = new Instruction(Operators.bra, op);
        condBlock.parentBlocks.get(1).instructions.add(branch);
        condBlock.parentBlocks.get(1).instructionIDs.add(branch.IDNum);
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
            irTree.current.instructionIDs.add(cmp.IDNum);

            Operand opcmp = new Operand(false, 0, cmp.IDNum, -1);
            if (relOp.id == ReservedWords.equalToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.bne, opcmp, null);
                irTree.current.instructions.add(branch);
                irTree.current.instructionIDs.add(branch.IDNum);
            } else if (relOp.id == ReservedWords.notEqualToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.beq, opcmp, null);
                irTree.current.instructions.add(branch);
                irTree.current.instructionIDs.add(branch.IDNum);
            } else if (relOp.id == ReservedWords.lessThanDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.bge, opcmp, null);
                irTree.current.instructions.add(branch);
                irTree.current.instructionIDs.add(branch.IDNum);
            } else if (relOp.id == ReservedWords.lessThanOrEqualToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.bgt, opcmp, null);
                irTree.current.instructions.add(branch);
                irTree.current.instructionIDs.add(branch.IDNum);
            } else if (relOp.id == ReservedWords.greaterThanDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.ble, opcmp, null);
                irTree.current.instructions.add(branch);
                irTree.current.instructionIDs.add(branch.IDNum);
            } else if (relOp.id == ReservedWords.greaterThanOrEqualToDefaultId.ordinal()) {
                Instruction branch = new Instruction(Operators.blt, opcmp, null);
                irTree.current.instructions.add(branch);
                irTree.current.instructionIDs.add(branch.IDNum);
            }
        }
    }

    private void varDecl(IntermediateTree irTree) throws SyntaxException, IOException {
        boolean array = false;
        ArrayList<Integer> dimensionArray = new ArrayList<>();
        if (token.id == ReservedWords.varDefaultId.ordinal()) {
            token = lexer.nextToken();
            if (token.kind != TokenKind.identity) {
                //error
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
            }
            irTree.current.declaredVariables.add(token.id);

        } else if (token.id == ReservedWords.arrayDefaultId.ordinal()) {
            array = true;

            token = lexer.nextToken();
            while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingThirdBracketDefaultId.ordinal()) {
                token = lexer.nextToken();
                dimensionArray.add(token.val);
                token = lexer.nextToken();
                token = lexer.nextToken();
            }
            irTree.current.declaredVariables.add(token.id);
            ArrayIdent arrayIdent = new ArrayIdent(token);
            arrayIdent.dimensions = dimensionArray;
//            irTree.current.ArrayIdentifiers.add(arrayIdent);
            irTree.current.arrayMap.put(token, arrayIdent);
        }
        token = lexer.nextToken();
        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.commaDefaultId.ordinal())) {
            token = lexer.nextToken();
            if (token.kind != TokenKind.identity) {
                //error
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "ident");
            }
            irTree.current.declaredVariables.add(token.id);
            if (array) {
                ArrayIdent arrayIdent = new ArrayIdent(token);
                arrayIdent.dimensions = dimensionArray;
//                irTree.current.ArrayIdentifiers.add(arrayIdent);
                irTree.current.arrayMap.put(token, arrayIdent);
            }
            token = lexer.nextToken();
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
        Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[operator.ordinal()], instruction);

        boolean allowdupl = irTree.current.whileBlock && (!instruction.firstOp.constant || !instruction.secondOp.constant);
        if (duplicate != null && !allowdupl) {
            instruction = duplicate;
        } else {
            irTree.current.instructions.add(instruction);
            irTree.current.instructionIDs.add(instruction.IDNum);
            InstructionLinkedList node = new InstructionLinkedList();
            node.value = instruction;
            node.previous = irTree.current.dominatorTree[operator.ordinal()];
            irTree.current.dominatorTree[operator.ordinal()] = node;
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
        Operand op = new Operand(false, 0, instruction.IDNum, id);
        op.returnVal = instruction;
        return op;
    }

    private Operand Factor(IntermediateTree irTree) throws SyntaxException, IOException {
        Operand result = new Operand();

        //negation
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.minusDefaultId.ordinal()) {
            token = lexer.nextToken();
            if (token.kind == TokenKind.number) {
                result = new Operand(true, token.val, null, -1);
                Instruction constantInstruction = new Instruction(Operators.constant, result, result);
                Instruction duplicate = getDuplicateInstruction(irTree.constants.dominatorTree[Operators.constant.ordinal()], constantInstruction);
                if (duplicate != null) {
                    result.valGenerator = duplicate.IDNum;
                    result.returnVal = duplicate;
                    Instruction.instrNum--;
                } else {
                    irTree.constants.instructions.add(constantInstruction);
                    irTree.constants.instructionIDs.add(constantInstruction.IDNum);
                    result = new Operand(true, token.val, constantInstruction.IDNum, -1);
                    result.returnVal = constantInstruction;
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = constantInstruction;
                    node.previous = irTree.constants.dominatorTree[Operators.constant.ordinal()];
                    irTree.constants.dominatorTree[Operators.constant.ordinal()] = node;
                }

                Instruction negInstr = new Instruction(Operators.neg, result);
                result = new Operand(true, -token.val, negInstr.IDNum, -1);
                result.returnVal = negInstr;
                duplicate = getDuplicateInstructionSingleOp(irTree.current.dominatorTree[Operators.neg.ordinal()], negInstr);
                if (duplicate != null) {
                    result.valGenerator = duplicate.IDNum;
                    result.returnVal = duplicate;
                    Instruction.instrNum--;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = negInstr;
                    node.previous = irTree.current.dominatorTree[Operators.neg.ordinal()];
                    irTree.current.dominatorTree[Operators.neg.ordinal()] = node;
                    irTree.current.instructions.add(negInstr);
                    irTree.current.instructionIDs.add(negInstr.IDNum);
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
                result = new Operand(false, 0, valueGenerator.IDNum, token.id);
                result.returnVal = valueGenerator;
                Instruction negInstr = new Instruction(Operators.neg, result);
                result = new Operand(false, 0, negInstr.IDNum, token.id);
                Instruction duplicate = getDuplicateInstructionSingleOp(irTree.current.dominatorTree[Operators.neg.ordinal()], negInstr);
                boolean allowdupl = irTree.current.whileBlock && (!negInstr.firstOp.constant);
                if (duplicate != null && allowdupl) {
                    result.valGenerator = duplicate.IDNum;
                    result.returnVal = duplicate;
                    Instruction.instrNum--;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = negInstr;
                    node.previous = irTree.current.dominatorTree[Operators.neg.ordinal()];
                    irTree.current.dominatorTree[Operators.neg.ordinal()] = node;

                    irTree.current.instructions.add(negInstr);
                    irTree.current.instructionIDs.add(negInstr.IDNum);
                }
            } else if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                token = lexer.nextToken();
                result = Expression(irTree);
                Instruction negInstr = new Instruction(Operators.neg, result);
                result = new Operand(false, 0, negInstr.IDNum, -1);
                result.returnVal = negInstr;
                Instruction duplicate = getDuplicateInstructionSingleOp(irTree.current.dominatorTree[Operators.neg.ordinal()], negInstr);
                boolean allowdupl = irTree.current.whileBlock && (!negInstr.firstOp.constant);
                if (duplicate != null && allowdupl) {
                    result.valGenerator = duplicate.IDNum;
                    result.returnVal = duplicate;
                    Instruction.instrNum--;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = negInstr;
                    node.previous = irTree.current.dominatorTree[Operators.neg.ordinal()];
                    irTree.current.dominatorTree[Operators.neg.ordinal()] = node;
                    irTree.current.instructions.add(negInstr);
                    irTree.current.instructionIDs.add(negInstr.IDNum);
                }
            }
        }

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
            Token ident = token;
            if (token.kind == TokenKind.identity) {
                //identity
                if (!irTree.current.declaredVariables.contains(token.id)) {
                    error(ErrorInfo.UNDECLARED_VARIABLE_PARSER_ERROR, "");
                }

                token = lexer.nextToken();
                if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingThirdBracketDefaultId.ordinal()) {
                    arrayDesignator(irTree, ident);
                }

                Instruction valueGenerator = irTree.current.valueInstructionMap.get(token.id);
                if (valueGenerator == null) {
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                    valueGenerator = assignZeroInstruction;
                }
                result = new Operand(false, 0, valueGenerator.IDNum, token.id);
                result.returnVal = valueGenerator;
            }
            if (token.kind == TokenKind.number) {
                //num
                result = new Operand(true, token.val, null, -1);
                Instruction constantInstruction = new Instruction(Operators.constant, result, result);
                Instruction duplicate = getDuplicateInstruction(irTree.constants.dominatorTree[Operators.constant.ordinal()], constantInstruction);
                if (duplicate != null) {
                    result.valGenerator = duplicate.IDNum;
                    result.returnVal = duplicate;
                    Instruction.instrNum--;
                } else {
                    irTree.constants.instructions.add(constantInstruction);
                    irTree.constants.instructionIDs.add(constantInstruction.IDNum);

                    result = new Operand(true, token.val, constantInstruction.IDNum, -1);
                    result.returnVal = constantInstruction;
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = constantInstruction;
                    node.previous = irTree.constants.dominatorTree[Operators.constant.ordinal()];
                    irTree.constants.dominatorTree[Operators.constant.ordinal()] = node;
                }
                token = lexer.nextToken();
            }

        } else if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.callDefaultId.ordinal()) {
            token = lexer.nextToken();
            result = funcCall(irTree);
            if (token.id != ReservedWords.semicolonDefaultId.ordinal()) {
                token = lexer.nextToken();
            }
        }
        return result;
    }

    private Operand arrayDesignator(IntermediateTree irTree, Token ident) throws SyntaxException, IOException {
//        while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingThirdBracketDefaultId.ordinal()) {
//            token = lexer.nextToken();
//            Operand opp = Expression(irTree);
//
//            token = lexer.nextToken();
//        }
        ArrayIdent arr = irTree.current.arrayMap.get(ident);
        Operand curOp;
        ArrayList<Operand> indexes = new ArrayList<>();
        for (int i = 0; i < arr.dimensions.size(); i++) {
            token = lexer.nextToken();
            curOp = Expression(irTree);
            indexes.add(curOp);
        }
        Operand ret;
        Operand ofs;
        if (indexes.size() == 1) {
            //simple array
            ofs = null;
        } else {
            boolean addIn = false;
            Operand op = indexes.get(indexes.size() - 1);
            for (int i = indexes.size() - 2; i >= 0; i--) {
                if (addIn) {
                    Instruction add = new Instruction(Operators.add, op, indexes.get(i));
                    irTree.current.instructions.add(add); //need to check duplicates
                    op = new Operand(false, -1, add.IDNum, -1);
                    addIn = false;
                } else {
                    Instruction mul = new Instruction(Operators.mul, op, indexes.get(i));
                    irTree.current.instructions.add(mul); // need to check duplicates
                    op = new Operand(false, -1, mul.IDNum, -1);
                    addIn = true;
                }
            }
            Operand four = new Operand(true, 4, null, -1);
            Instruction constantFour = new Instruction(Operators.constant, four, four); //check duplicate, add to bb
            four = new Operand(true, 4, constantFour.IDNum, -1);
            Instruction offset = new Instruction(Operators.mul, op, four); //check duplicate, add to bb
            ofs = new Operand(false, -1, offset.IDNum, -1);
        }
        Operand FP = new Operand("FP");
        Operand arrayBase = new Operand(arr.getStartingAddress() + "(FP)");
        Instruction base = new Instruction(Operators.add, FP, arrayBase); //check duplicate, add to bb
        Operand bas = new Operand(false, -1, base.IDNum, -1);
        Instruction adda = new Instruction(Operators.adda, ofs, bas);

        ret = new Operand(false, -1, adda.IDNum, -1);

        // create instructions: mul/muli, add FP arra base, adda
        //then will be either store or load
        // need to add Hashmap when creating new block, or static.
        return ret;
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

    private void createPhiInstructions(BasicBlock joinBlock) {
        for (Integer identity : joinBlock.assignedVariables) {
            BasicBlock ifParent = joinBlock.parentBlocks.get(0);
            Instruction ifValueGenerator = ifParent.valueInstructionMap.get(identity);
            if (ifValueGenerator == null) {
                warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                ifValueGenerator = assignZeroInstruction;
            }
            Operand firstOp = new Operand(false, 0, ifValueGenerator.IDNum, identity);

            BasicBlock elseParent = joinBlock.parentBlocks.get(1);
            Instruction elseValueGenerator = elseParent.valueInstructionMap.get(identity);
            if (elseValueGenerator == null) {
                warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                elseValueGenerator = assignZeroInstruction;
            }
            Operand secondOp = new Operand(false, 0, elseValueGenerator.IDNum, identity);
            Instruction phiInstruction = new Instruction(Operators.phi, firstOp, secondOp);
            joinBlock.instructions.add(phiInstruction);
            joinBlock.instructionIDs.add(phiInstruction.IDNum);
            joinBlock.valueInstructionMap.put(identity, phiInstruction);
        }
    }

    private Instruction createPhiInstructionSingleVar(BasicBlock whileBlock, int identity, Operand whileOp) {
        BasicBlock condBlock = whileBlock.parentBlocks.get(0);
        if (whileBlock.nestedBlock) {
            condBlock = whileBlock.parentBlocks.get(0).parentBlocks.get(0);
        }
        Instruction valueGenerator = condBlock.valueInstructionMap.get(identity);
        if (valueGenerator == null) {
            warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
            valueGenerator = assignZeroInstruction;
        }
        Operand condOp = new Operand(false, 0, valueGenerator.IDNum, identity);
        Instruction phiInstruction = new Instruction(Operators.phi, condOp, whileOp);
        condBlock.instructions.add(condBlock.phiIndex, phiInstruction);
        condBlock.instructionIDs.add(phiInstruction.IDNum);
        condBlock.phiIndex++;
        condBlock.valueInstructionMap.put(identity, phiInstruction);
        condBlock.assignedVariables.add(identity);
        whileOp = new Operand(whileOp.constant, whileOp.constVal, phiInstruction.IDNum, whileOp.id);
        visitedBlocks.clear();
        updateBlockInstructions(condBlock, identity, phiInstruction);

        whileBlock.assignedVariables.add(identity);

        if (condBlock.parentBlocks.size() > 0) {
            BasicBlock outerParent = condBlock.parentBlocks.get(0);
            if (outerParent.whileBlock) {
                createPhiInstructionSingleVar(condBlock, identity, whileOp);
            }
        }

        return phiInstruction;
    }

    private void updateBlockInstructions(BasicBlock block, int identity, Instruction newValueGenerator) {
        visitedBlocks.add(block.IDNum);
        for (int i = 0; i < block.instructions.size(); i++) {
            Instruction oldInstruction = block.instructions.get(i);
            if (oldInstruction.IDNum == newValueGenerator.IDNum) {
                continue;
            }
            Instruction updatedInstruction = updateInstruction(oldInstruction, newValueGenerator);
            block.instructions.set(i, updatedInstruction);
        }
        for (int i = 0; i < block.childBlocks.size(); i++) {
            if (!visitedBlocks.contains(block.childBlocks.get(i).IDNum)) {
                updateBlockInstructions(block.childBlocks.get(i), identity, newValueGenerator);
            }
        }
    }

    private Instruction updateInstruction(Instruction oldInstruction, Instruction newValueGenerator) {
        if (oldInstruction.operator == Operators.constant) {
            return oldInstruction;
        }
        Operand firstOp, secondOp;

        if (oldInstruction.firstOp != null && !oldInstruction.firstOp.constant) {
            firstOp = updateOperand(oldInstruction.firstOp, newValueGenerator);
        } else {
            firstOp = oldInstruction.firstOp;
        }
        if (oldInstruction.secondOp != null && !oldInstruction.secondOp.constant) {
            secondOp = updateOperand(oldInstruction.secondOp, newValueGenerator);
        } else {
            secondOp = oldInstruction.secondOp;
        }

        oldInstruction.firstOp = firstOp;
        oldInstruction.secondOp = secondOp;
        Instruction updatedInstruction = oldInstruction;

        return updatedInstruction;
    }

    private Operand updateOperand(Operand operand, Instruction newValueGenerator) {
        if (operand.valGenerator == newValueGenerator.firstOp.valGenerator) {
            operand.valGenerator = newValueGenerator.IDNum;
        } else if (operand.valGenerator == newValueGenerator.secondOp.valGenerator) {
            operand.valGenerator = newValueGenerator.IDNum;
        }
        return operand;
    }
}
