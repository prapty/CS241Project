import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        if (token.kind == TokenKind.reservedWord && token.id == ReservedWords.callDefaultId.ordinal()) {
            token = lexer.nextToken();
            funcCall(irTree);
        }
        Operand op=Expression(irTree);
        irTree.current.valueInstructionMap.put(left.id, op.valGenerator);
        irTree.current.assignedVariables.add(left.id);
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

            // create second operand to branch instruction
            Instruction firstInstr = thenBlock.instructions.get(0);
            Operand op = new Operand(false, 0, firstInstr, token.id);
            Instruction branch = thenBlock.parentBlocks.get(0).getLastInstruction();
            branch.secondOp = op;
            thenBlock.parentBlocks.get(0).setLastInstruction(branch);
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
            BasicBlock joinBlock = new BasicBlock();
            irTree.current = irTree.current.parentBlocks.get(0);
            for (int i = 0; i < irTree.current.childBlocks.size(); i++) {
                BasicBlock block = irTree.current.childBlocks.get(i);
                joinBlock.parentBlocks.add(block);
                block.childBlocks.add(joinBlock);
                //get all variables which were assigned in if block and else block
                joinBlock.assignedVariables.addAll(block.assignedVariables);
            }
            //create phi instructions for changed variables
            int lastIndex = Math.max((joinBlock.instructions.size() - 1), 0);
            createPhiInstructions(joinBlock, lastIndex);
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

        // create second operand to branch instruction
        Instruction firstInstr = whileBlock.instructions.get(0);
        Operand op = new Operand(false, 0, firstInstr, token.id);
        Instruction branch = whileBlock.parentBlocks.get(0).getLastInstruction();
        branch.secondOp = op;
        whileBlock.parentBlocks.get(0).setLastInstruction(branch);
        if (token.kind != TokenKind.reservedWord || token.id != ReservedWords.odDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "od");
        }

        //create phi instructions at the beginning of the cond block
        int index = 0;
        condBlock.assignedVariables.addAll(whileBlock.assignedVariables);
        createPhiInstructions(condBlock, index);

        //go through instructions in while block and update variable values using phi
        for (int i = 0; i < whileBlock.instructions.size(); i++) {
            Instruction instruction = whileBlock.getAnyInstruction(i);
            if (!instruction.firstOp.constant) {
                int id = instruction.firstOp.id;
                if (whileBlock.assignedVariables.contains(id)) {
                    instruction.firstOp.valGenerator = condBlock.valueInstructionMap.get(id);
                }
            }
            if (!instruction.secondOp.constant) {
                int id = instruction.secondOp.id;
                if (whileBlock.assignedVariables.contains(id)) {
                    instruction.secondOp.valGenerator = condBlock.valueInstructionMap.get(instruction.secondOp.id);
                }
            }
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
            Instruction instruction = irTree.current.getLastInstruction();
            Operators ops = Operators.cmp;
            if (instruction.operator == null) {
                instruction.operator = ops;
                irTree.current.setLastInstruction(instruction);
            } else {
//                Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[ops.ordinal()], instruction);
//                if (duplicate != null) {
//                    instruction = duplicate;
//                    irTree.current.instructions.remove(irTree.current.instructions.size() - 1);
//                }
                Operand firsrOp = new Operand(false, 0, instruction, token.id);
                Instruction newInstruction = new Instruction(ops, firsrOp, null);
                irTree.current.instructions.add(newInstruction);
            }
            token = lexer.nextToken();
            Expression(irTree);

            Instruction last = irTree.current.getLastInstruction();
            Operand opcmp = new Operand(false, 0, last, token.id);
            if (relOp.id == ReservedWords.equalToDefaultId.ordinal()){
                Instruction branch = new Instruction(Operators.beq, opcmp, null);
                irTree.current.instructions.add(branch);
            }
            if (relOp.id == ReservedWords.notEqualToDefaultId.ordinal()){
                Instruction branch = new Instruction(Operators.bne, opcmp, null);
                irTree.current.instructions.add(branch);
            }
            if (relOp.id == ReservedWords.lessThanDefaultId.ordinal()){
                Instruction branch = new Instruction(Operators.blt, opcmp, null);
                irTree.current.instructions.add(branch);
            }
            if (relOp.id == ReservedWords.lessThanOrEqualToDefaultId.ordinal()){
                Instruction branch = new Instruction(Operators.ble, opcmp, null);
                irTree.current.instructions.add(branch);
            }
            if (relOp.id == ReservedWords.greaterThanDefaultId.ordinal()){
                Instruction branch = new Instruction(Operators.bgt, opcmp, null);
                irTree.current.instructions.add(branch);
            }
            if (relOp.id == ReservedWords.greaterThanOrEqualToDefaultId.ordinal()){
                Instruction branch = new Instruction(Operators.bge, opcmp, null);
                irTree.current.instructions.add(branch);
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

    private Operand Expression(IntermediateTree irTree) throws IOException, SyntaxException {
        Operand left, right;
        left=Term(irTree);
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
        left=Factor(irTree);

        while (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.mulDefaultId.ordinal() || token.id == ReservedWords.divDefaultId.ordinal())) {
            Operators operator;
            if (token.id == ReservedWords.mulDefaultId.ordinal()) {
                operator = Operators.mul;
            } else {
                operator = Operators.div;
            }
            token = lexer.nextToken();
            right=Factor(irTree);
            left=Compute(irTree, operator, left, right);
        }

        return left;
    }

    private Operand Compute(IntermediateTree irTree, Operators operator, Operand left, Operand right) {
        Instruction instruction = new Instruction(operator, left, right);
        Instruction duplicate=getDuplicateInstruction(irTree.current.dominatorTree[operator.ordinal()],instruction);
        if(duplicate!=null){
            instruction=duplicate;
        }
        else{
            irTree.current.instructions.add(instruction);
            InstructionLinkedList node = new InstructionLinkedList();
            node.value = instruction;
            node.previous = irTree.current.dominatorTree[operator.ordinal()];
            irTree.current.dominatorTree[operator.ordinal()] = node;
        }
        int id=-1;
        if(!left.constant && !right.constant){
            id=-3;
        }
        if((left.constant && !right.constant)||(!left.constant && right.constant)){
            id=-2;
        }
        Operand op = new Operand(false, 0, instruction, id);
        return op;
        //op.valGenerator=constantInstruction;
    }

    private Operand Factor(IntermediateTree irTree) throws SyntaxException, IOException {
        Operand result=new Operand();
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
            token = lexer.nextToken();
            result=Expression(irTree);
            if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.endingFirstBracketDefaultId.ordinal()) {
                //end expression
                token = lexer.nextToken();
            } else {
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ")");
            }

        } else if (token.kind == TokenKind.identity || token.kind == TokenKind.number) {
            if (token.kind == TokenKind.identity) {
                //identity
                Instruction valueGenerator = irTree.current.valueInstructionMap.get(token.id);
                if (valueGenerator == null) {
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                    valueGenerator=assignZeroInstruction;
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
            //do later
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

    private void createPhiInstructions(BasicBlock joinBlock, int index) {
        for (Integer identity : joinBlock.assignedVariables) {
            joinBlock.assignedVariables.add(identity);
            for (int i = 0; i < joinBlock.parentBlocks.size(); i++) {
                BasicBlock parentBlock = joinBlock.parentBlocks.get(i);
                Operand op = new Operand(false, 0, parentBlock.valueInstructionMap.get(identity), identity);
                if (i == 0) {
                    Instruction phiInstruction = new Instruction(Operators.phi, op, null);
                    joinBlock.instructions.add(index, phiInstruction);
                    joinBlock.valueInstructionMap.put(identity, phiInstruction);
                } else {
                    Instruction lastInstruction = joinBlock.getAnyInstruction(index);
                    if (lastInstruction.secondOp == null) {
                        lastInstruction.secondOp = op;
                        joinBlock.setLastInstruction(lastInstruction);
                    } else {
                        Operand firstOp = new Operand(false, 0, lastInstruction, identity);
                        Instruction newInstruction = new Instruction(Operators.phi, firstOp, op);
                        joinBlock.instructions.add(newInstruction);
                        joinBlock.valueInstructionMap.put(identity, newInstruction);
                    }
                    index++;
                }
            }
        }
    }
}
