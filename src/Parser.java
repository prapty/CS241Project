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
        if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.voidDefaultId.ordinal() || token.id == ReservedWords.functionDefaultId.ordinal())) {
            funcDecl(irTree);

            while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.semicolonDefaultId.ordinal()) {
                //after ";" , check if next is var decl
                token = lexer.nextToken();
                if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.voidDefaultId.ordinal() || token.id == ReservedWords.functionDefaultId.ordinal())) {
                    funcDecl(irTree);
                }
            }
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
        Instruction endInstr = new Instruction(Operators.end);
        irTree.current.instructions.add(endInstr);
        irTree.current.instructionIDs.add(endInstr.IDNum);
    }

    private void funcDecl(IntermediateTree irTree) throws SyntaxException, IOException {
        boolean isVoid = false;
        if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.voidDefaultId.ordinal())) {
            token = lexer.nextToken();
            if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.functionDefaultId.ordinal())) {
                isVoid = true;
            } else {
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "void");
            }
        }
        token = lexer.nextToken();
        if (token.kind != TokenKind.identity) {
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
        }
        int identity = token.id;
        Function function = new Function(isVoid, irTree.constants);
        if(function.irTree.constants.dominatorTree[Operators.constant.ordinal()]==null){
            InstructionLinkedList node = new InstructionLinkedList();
            node.previous = null;
            node.value = assignZeroInstruction;
            function.irTree.constants.dominatorTree[Operators.constant.ordinal()] = node;
        }
        functionInfo.put(identity, function);
        token = lexer.nextToken();
        if (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.startingFirstBracketDefaultId.ordinal())) {
            formalParam(function);
            while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.commaDefaultId.ordinal()) {
                formalParam(function);
            }
        } else {
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "(");
        }
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.endingFirstBracketDefaultId.ordinal()) {
            //if no }, error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ")");
        }
        token = lexer.nextToken();
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.semicolonDefaultId.ordinal()) {
            //if no }, error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ";");
        }
        token = lexer.nextToken();
        funcBody(function);
        token = lexer.nextToken();
        if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.semicolonDefaultId.ordinal()) {
            //if no ., error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ";");
        }
    }

    private void formalParam(Function function) throws SyntaxException, IOException {
        token = lexer.nextToken();
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.endingFirstBracketDefaultId.ordinal()) {
            return;
        }
        if (token.kind != TokenKind.identity) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "identity");
        }
        function.parameters.add(token.id);
        function.irTree.current.declaredVariables.add(token.id);

        token = lexer.nextToken();
    }

    private void pushOperation(IntermediateTree irTree, Operand pushOp){
        Instruction pushInstruction = new Instruction(Operators.push, pushOp);
        irTree.current.instructions.add(pushInstruction);
        irTree.current.instructionIDs.add(pushInstruction.IDNum);
        //decrease SP by 4, store value in SP
        //create instruction for constant 4
        Operand spOp=new Operand(Registers.SP.name());
        Operand fourOp = new Operand(true, 4, null, -1);
        Instruction fourOpInstr = new Instruction(Operators.constant, fourOp, fourOp);
        fourOp = constantDuplicate(irTree, fourOp, fourOpInstr);
        Instruction subSPInstruction = new Instruction(Operators.sub, spOp, fourOp);
        subSPInstruction.storeRegister = Registers.SP.name();
        irTree.current.instructions.add(subSPInstruction);
        irTree.current.instructionIDs.add(subSPInstruction.IDNum);
    }

    private void pushRegisterOperation(IntermediateTree irTree){
        Instruction pushInstruction = new Instruction(Operators.pushUsedRegisters);
        irTree.current.instructions.add(pushInstruction);
        irTree.current.instructionIDs.add(pushInstruction.IDNum);
    }

    private Instruction popRegisterOperation(IntermediateTree irTree){
        Instruction popInstruction = new Instruction(Operators.popUsedRegisters);
        irTree.current.instructions.add(popInstruction);
        irTree.current.instructionIDs.add(popInstruction.IDNum);
        return popInstruction;
    }

    private Instruction popOperation(IntermediateTree irTree){
        Instruction popInstruction = new Instruction(Operators.pop);
        irTree.current.instructions.add(popInstruction);
        irTree.current.instructionIDs.add(popInstruction.IDNum);


        //create instruction for constant 4
        Operand spOp=new Operand(Registers.SP.name());
        Operand fourOp = new Operand(true, 4, null, -1);
        Instruction fourOpInstr = new Instruction(Operators.constant, fourOp, fourOp);
        fourOp = constantDuplicate(irTree, fourOp, fourOpInstr);
        //increase SP by 4, store value in SP
        Instruction addSPInstruction = new Instruction(Operators.add, spOp, fourOp);
        addSPInstruction.storeRegister = Registers.SP.name();
        irTree.current.instructions.add(addSPInstruction);
        irTree.current.instructionIDs.add(addSPInstruction.IDNum);

        return popInstruction;
    }

    private void funcBody(Function function) throws SyntaxException, IOException {
        IntermediateTree irTree = function.irTree;
        //pop arguments from the stack
        for(int i=function.parameters.size()-1; i>=0; i--){
            int id=function.parameters.get(i);
            Instruction popInstruction = popOperation(irTree);
            irTree.current.valueInstructionMap.put(id, popInstruction);
        }
        //push current return address to the stack
        Operand reg31Op=new Operand(Registers.R31.name());
        pushOperation(irTree, reg31Op);

        //push current frame pointer to the stack
        Operand fpOp=new Operand(Registers.FP.name());
        pushOperation(irTree, fpOp);

        //move frame pointer to stack pointer
        Operand spOp=new Operand(Registers.SP.name());
        Operand zeroOp = new Operand(true, 0, null, -1);
        Instruction zeroOpInstr = new Instruction(Operators.constant, zeroOp, zeroOp);
        zeroOp = constantDuplicate(irTree, zeroOp, zeroOpInstr);
        Instruction assignFPInstruction = new Instruction(Operators.add, spOp, zeroOp);
        assignFPInstruction.storeRegister = Registers.FP.name();
        irTree.current.instructions.add(assignFPInstruction);
        irTree.current.instructionIDs.add(assignFPInstruction.IDNum);

//        if(token.kind != TokenKind.reservedSymbol || (token.id != ReservedWords.startingCurlyBracketDefaultId.ordinal())){
//            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "{");
//        }
        //token = lexer.nextToken();
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
        if(function.isVoid && !irTree.retAdded){
            Instruction retInstr = new Instruction(Operators.ret);
            irTree.current.instructions.add(retInstr);
            irTree.current.instructionIDs.add(retInstr.IDNum);
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
            funcCall(irTree, true);
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
        Operand store = null;
        if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingThirdBracketDefaultId.ordinal()) {
            store = arrayDesignator(irTree, left);
//            token = lexer.nextToken();
//            token = lexer.nextToken();
//            System.out.println(token.id);
        }
        if (token.kind != TokenKind.relOp && token.id != ReservedWords.assignmentSymbolDefaultId.ordinal()) {
            //error
            error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "<-");
        }
        token = lexer.nextToken();
        Operand op = Expression(irTree);
        if (store == null) {
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
        } else {
            Instruction storeInt = new Instruction(Operators.store, op, store); //store op at store location
            storeInt.arrayID = left.id; //check duplicate
            irTree.current.instructions.add(storeInt);
            irTree.current.instructionIDs.add(storeInt.IDNum);
            irTree.current.assignedVariables.add(left.id);

            InstructionLinkedList node = new InstructionLinkedList();
            node.value = storeInt;
            node.previous = irTree.current.dominatorTree[Operators.load.ordinal()];
            irTree.current.dominatorTree[Operators.load.ordinal()] = node;
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
        if (phi.operator == Operators.kill) {
            Operand killOp = new Operand(true, id, null, -1);
            Instruction killInstr = new Instruction(Operators.kill, killOp); //check duplicates
            killInstr.arrayID = id;
            putInThisCondd.instructions.add(killInstr);
            putInThisCondd.instructionIDs.add(killInstr.IDNum);
        } else {
            Instruction newPhi;
            Instruction valueGenerator = putInThisCondd.valueInstructionMap.get(id);
            Operand op = new Operand(false, -1, phi.IDNum, id);
            op.returnVal = phi;
            if (valueGenerator == null) {
                warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                valueGenerator = assignZeroInstruction;
            }
            Operand firstop = new Operand(false, 0, valueGenerator.IDNum, id);
            firstop.returnVal = valueGenerator;
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
    }

    private Instruction makeWhilePhiDirectCond(IntermediateTree irTree, BasicBlock current, BasicBlock condd, Operand op, int id) {
        Instruction phi;
        ArrayIdent arr = condd.arrayMap.get(id);
        if (arr != null) {
            Operand killOp = new Operand(true, id, null, -1);
            Instruction killInstr = new Instruction(Operators.kill, killOp); //check duplicates
            killInstr.arrayID = id;
            condd.instructions.add(killInstr);
            condd.instructionIDs.add(killInstr.IDNum);
            return killInstr;
        } else {
            Instruction valueGenerator = condd.valueInstructionMap.get(id);
            if (valueGenerator == null) {
                warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                valueGenerator = assignZeroInstruction;
            }
            Operand firstop = new Operand(false, 0, valueGenerator.IDNum, id);
            firstop.returnVal = valueGenerator;
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

    private Operand funcCall(IntermediateTree irTree, boolean fromStatement) throws SyntaxException, IOException {
        if (token.kind == TokenKind.reservedWord) {
            return predefinedFuncCall(irTree, fromStatement);
        } else {
            return userDefinedFuncCall(irTree, fromStatement);
        }
    }

    private Operand userDefinedFuncCall(IntermediateTree irTree, boolean fromStatement) throws IOException, SyntaxException {
        Operand returnValue = new Operand(false, 0, null, -1);
        Function function = functionInfo.get(token.id);
        if (function == null) {
            error(ErrorInfo.UNDECLARED_FUNCTION_PARSER_ERROR, "");
        }
        if (fromStatement && !function.isVoid) {
            error(ErrorInfo.UNEXPECTED_FUNCTION_TYPE_PARSER_ERROR, "void");
        }
        if (!fromStatement && function.isVoid) {
            error(ErrorInfo.UNEXPECTED_FUNCTION_TYPE_PARSER_ERROR, "non-void");
        }

        token = lexer.nextToken();
        IntermediateTree functionIrTree = function.irTree;
        //push all used registers to the stack
        pushRegisterOperation(irTree);
        List<Integer> argumentList = new ArrayList<>();
        //functions with formal parameters. Need to push arguments to the stack
        if (function.parameters.size() > 0) {
            if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, "(");
            }
            token = lexer.nextToken();

            Operand argument1= Expression(irTree);
            argumentList.add(argument1.valGenerator);
            //push argument 1 to stack;
            pushOperation(irTree, argument1);

            while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.commaDefaultId.ordinal()) {
                token = lexer.nextToken();
                if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.endingFirstBracketDefaultId.ordinal()) {
                    break; //if ; was the last optional terminating semicolon before ; stop parsing
                }
                Operand argument= Expression(irTree);
                argumentList.add(argument.valGenerator);
                //push argument  to stack;
                pushOperation(irTree, argument);
            }
            if (token.kind != TokenKind.reservedSymbol || token.id != ReservedWords.endingFirstBracketDefaultId.ordinal()) {
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ")");
            }
        }
        //insert special call instruction
        Operand destination = new Operand(false, 0, functionIrTree.start.instructions.get(0).IDNum, -1);
        Instruction callInstruction = new Instruction(Operators.jsr, destination);
        callInstruction.arguments = argumentList;
        irTree.current.instructions.add(callInstruction);
        irTree.current.instructionIDs.add(callInstruction.IDNum);
        //functions with no parameters. Can directly add IrTree of the function to the current block

        functionIrTree.start.parentBlocks.add(irTree.current);
        irTree.current.childBlocks.add(functionIrTree.start);

        if (token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
            token = lexer.nextToken();
            if (token.id != ReservedWords.endingFirstBracketDefaultId.ordinal()) {
                error(ErrorInfo.UNEXPECTED_TOKEN_PARSER_ERROR, ")");
            }
        }

        //new basic block is created after function call
        BasicBlock newBlock = new BasicBlock();
        newBlock.valueInstructionMap.putAll(irTree.current.valueInstructionMap);
        newBlock.dominatorTree = irTree.current.dominatorTree.clone();
        newBlock.declaredVariables.addAll(irTree.current.declaredVariables);
        newBlock.parentBlocks.add(irTree.current);
        irTree.current.childBlocks.add(newBlock);
        newBlock.dominatorBlock = irTree.current;
        irTree.current = newBlock;
        //pop returned value
        if (!function.isVoid) {
            Instruction returnValInstr = popOperation(irTree);
            returnValInstr.storeRegister = Registers.RV.name();
            returnValue.valGenerator = returnValInstr.IDNum;
            returnValue.returnVal = returnValInstr;
        }
        //pop register values
        popRegisterOperation(irTree);
        token = lexer.nextToken();
        return returnValue;
    }

    private Operand predefinedFuncCall(IntermediateTree irTree, boolean fromStatement) throws IOException, SyntaxException {
        Operand returnValue = null;
        if (token.id == ReservedWords.InputNumDefaultId.ordinal()) {
            if (fromStatement) {
                error(ErrorInfo.UNEXPECTED_FUNCTION_TYPE_PARSER_ERROR, "void");
            }
            Operators ops = Operators.read;
            Instruction readInstr = new Instruction(ops);
            irTree.current.instructions.add(readInstr);
            irTree.current.instructionIDs.add(readInstr.IDNum);
            token = lexer.nextToken();
            if (token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                token = lexer.nextToken();
            }

            returnValue = new Operand(true, -1, readInstr.IDNum, -1);
            returnValue.returnVal = readInstr;
        } else if (token.id == ReservedWords.OutputNumDefaultId.ordinal()) {
            if (!fromStatement) {
                error(ErrorInfo.UNEXPECTED_FUNCTION_TYPE_PARSER_ERROR, "non-void");
            }
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
            if (!fromStatement) {
                error(ErrorInfo.UNEXPECTED_FUNCTION_TYPE_PARSER_ERROR, "non-void");
            }
            Operators ops = Operators.writeNL;
            Instruction writeNLInstr = new Instruction(ops);
            irTree.current.instructions.add(writeNLInstr);
            irTree.current.instructionIDs.add(writeNLInstr.IDNum);
            token = lexer.nextToken();
            if (token.id == ReservedWords.startingFirstBracketDefaultId.ordinal()) {
                token = lexer.nextToken();
            }
        }
        return returnValue;
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
            thenBlock.arrayMap.putAll(parentBlock.arrayMap);
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
            joinBlock.arrayMap.putAll(irTree.current.arrayMap);

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
            elseBlock.arrayMap.putAll(parentBlock.arrayMap);
            elseBlock.parentBlocks.add(parentBlock);
            parentBlock.childBlocks.add(elseBlock);
            irTree.current = elseBlock;
            statSequence(irTree);
        } else { //empty else block
            elseBlock.valueInstructionMap.putAll(parentBlock.valueInstructionMap);
            elseBlock.dominatorTree = parentBlock.dominatorTree.clone();
            elseBlock.declaredVariables.addAll(parentBlock.declaredVariables);
            elseBlock.arrayMap.putAll(parentBlock.arrayMap);
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
        joinBlock.arrayMap.putAll(irTree.current.arrayMap);

        if (elseBlock.instructions.isEmpty()) {
            Instruction emptyInstr = new Instruction(Operators.empty);
            elseBlock.instructions.add(emptyInstr);
            elseBlock.instructionIDs.add(emptyInstr.IDNum);
        }
        Instruction firstInstr = elseBlock.instructions.get(0);
        Operand op = new Operand(false, 0, firstInstr.IDNum, -1);
        op.returnVal = firstInstr;
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
                joinBlock.arrayMap.putAll(block.arrayMap);
            }
            //create phi instructions for changed variables
            createPhiInstructions(joinBlock);
            irTree.current = joinBlock;
            if (parentBlock.whileBlock) {
                for (int identity : joinBlock.assignedVariables) {
                    Operand phiOp = new Operand(false, 0, joinBlock.valueInstructionMap.get(identity).IDNum, identity);
                    phiOp.returnVal = joinBlock.valueInstructionMap.get(identity);
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
            opp.returnVal = first;
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
        condBlock.assignedVariables.addAll(irTree.current.assignedVariables);
        condBlock.arrayMap.putAll(irTree.current.arrayMap);
        condBlock.parentBlocks.add(irTree.current);
        irTree.current.childBlocks.add(condBlock);
        irTree.current = condBlock;

        condBlock.whileBlock = condBlock.parentBlocks.get(0).whileBlock;
        condBlock.nested = condBlock.parentBlocks.get(0).nested + 1;
        condBlock.isCond = true;
        condBlock.condBlock = condBlock.parentBlocks.get(0).condBlock;

        InstructionLinkedList[] tempDomTree = null;
        if (condBlock.nested == 1) {
            tempDomTree = condBlock.dominatorTree.clone();
        }

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
        condBlock.arrayMap.putAll(irTree.current.arrayMap);
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
        newBlock.arrayMap.putAll(condBlock.arrayMap);
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
        ops.returnVal = firstInstr;
        Instruction branchCond = condBlock.getLastInstruction();
        branchCond.secondOp = ops;
        condBlock.setLastInstruction(branchCond);

        firstInstr = condBlock.instructions.get(0);
        Operand op = new Operand(false, 0, firstInstr.IDNum, -1);
        op.returnVal = firstInstr;
        Instruction branch = new Instruction(Operators.bra, op);
        condBlock.parentBlocks.get(1).instructions.add(branch);
        condBlock.parentBlocks.get(1).instructionIDs.add(branch.IDNum);

        if (newBlock.nested == 0) {
            InstructionLinkedList[] newDomTree = commonSubExpressionWhile(irTree, condBlock, newBlock, tempDomTree);
            newBlock.dominatorTree = newDomTree.clone();
        }
    }

    private InstructionLinkedList[] commonSubExpressionWhile(IntermediateTree irTree, BasicBlock condBlock, BasicBlock newBlock, InstructionLinkedList[] tempDomTree) {
        condBlock.dominatorTree = tempDomTree.clone();
        LinkedList<BasicBlock> blocksInWhile = new LinkedList<>();
        blocksInWhile.add(condBlock);
        ArrayList<BasicBlock> visited = new ArrayList<>();
        visited.add(condBlock);
        BasicBlock current = blocksInWhile.poll();
        while (current != newBlock) {
            for (int i = current.childBlocks.size(); i >= 0; i--) {
                if (!visited.contains(current.childBlocks.get(i))) {
                    blocksInWhile.addFirst(current.childBlocks.get(i));
                }
            }
            for (Instruction i : current.instructions) {
                //cases: add, sub, mul, div, constant, neg, adda, load, cmp, read, write, empty, store, phi, branching, kill, addFP
                // normal double dupl: add, sub, mul, div, adda, store
                // no dupl needed: cmp, read, write, writeNL, branching, phi
                //single op dupl: neg, kill
                // special dupl: add FP, load, store
//                if (i.operator== Operators.phi || i.operator == Operators.write || i.operator == Operators.writeNL || i.operator == Operators.read || i.operator == Operators.cmp || i.operator == Operators.kill || i.operator.toString().charAt(0)=='b'){

                if (i.operator == Operators.sub || i.operator == Operators.mul || i.operator == Operators.div || i.operator == Operators.adda || (i.operator == Operators.add && i.firstOp.arraybase == null)) {
                    Instruction duplicate = getDuplicateInstruction(current.dominatorTree[i.operator.ordinal()], i);
                    if (duplicate != null) {


                    } else {
                        InstructionLinkedList node = new InstructionLinkedList();
                        node.value = i;
                        node.previous = current.dominatorTree[i.operator.ordinal()];
                        current.dominatorTree[i.operator.ordinal()] = node;
                    }
                } else if (i.operator == Operators.neg) {
                    Instruction duplicate = getDuplicateInstructionSingleOp(current.dominatorTree[i.operator.ordinal()], i);
                    if (duplicate != null) {


                    } else {
                        InstructionLinkedList node = new InstructionLinkedList();
                        node.value = i;
                        node.previous = current.dominatorTree[i.operator.ordinal()];
                        current.dominatorTree[i.operator.ordinal()] = node;
                    }
                } else if (i.operator == Operators.add && i.firstOp.arraybase != null) {

                } else if (i.operator == Operators.load) {

                } else if (i.operator == Operators.store) {

                }
            }
            for (int i = current.childBlocks.size(); i >= 0; i--) {
                if (!visited.contains(current.childBlocks.get(i))) {
                    current.childBlocks.get(i).dominatorTree = current.dominatorTree.clone();
                    visited.add(current.childBlocks.get(i));
                }
            }
            current = blocksInWhile.poll();
        }
        return null;
    }

    private Operand returnStatement(IntermediateTree irTree) throws SyntaxException, IOException {
        Operand result;
        if (token.kind == TokenKind.reservedSymbol && (token.id == ReservedWords.endingCurlyBracketDefaultId.ordinal() || token.id == ReservedWords.semicolonDefaultId.ordinal())) {
            result = null;
        } else if (token.kind == TokenKind.reservedWord && (token.id == ReservedWords.elseDefaultId.ordinal() || token.id == ReservedWords.fiDefaultId.ordinal() || token.id == ReservedWords.odDefaultId.ordinal())) {
            result = null;
        } else {
            result = Expression(irTree);
        }
        //move SP to FP
        //move stack pointer to frame pointer
        Operand fpOp=new Operand(Registers.FP.name());
        Operand zeroOp = new Operand(true, 0, null, -1);
        Instruction zeroOpInstr = new Instruction(Operators.constant, zeroOp, zeroOp);
        zeroOp = constantDuplicate(irTree, zeroOp, zeroOpInstr);
        Instruction assignSPInstruction = new Instruction(Operators.add, fpOp, zeroOp);
        assignSPInstruction.storeRegister = Registers.SP.name();
        irTree.current.instructions.add(assignSPInstruction);
        irTree.current.instructionIDs.add(assignSPInstruction.IDNum);
        //pop previous FP
        Instruction popFPInstr=popOperation(irTree);
        popFPInstr.storeRegister = Registers.FP.name();
        //pop previous return address
        Instruction popR31Instr=popOperation(irTree);
        popR31Instr.storeRegister = Registers.R31.name();
        // set result in stack
        if(!irTree.isVoid){
            pushOperation(irTree, result);
        }
        Instruction retInstr = new Instruction(Operators.ret);
        irTree.current.instructions.add(retInstr);
        irTree.current.instructionIDs.add(retInstr.IDNum);
        irTree.retAdded = true;
        return result;
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
            opcmp.returnVal = cmp;
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
        ArrayList<Operand> dimOps = new ArrayList<>();
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
                Operand constantDim = new Operand(true, token.val, null, -1);
                Instruction constantDimInstr = new Instruction(Operators.constant, constantDim, constantDim);
                constantDuplicate(irTree, constantDim, constantDimInstr);
                dimOps.add(new Operand(true, token.val, constantDimInstr.IDNum, -1));
                token = lexer.nextToken();
                token = lexer.nextToken();
            }
            irTree.current.declaredVariables.add(token.id);
            ArrayIdent arrayIdent = new ArrayIdent(token);
            arrayIdent.dimensions = dimensionArray;
            arrayIdent.opDims = dimOps;
//            irTree.current.ArrayIdentifiers.add(arrayIdent);
            irTree.current.arrayMap.put(token.id, arrayIdent);
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
                irTree.current.arrayMap.put(token.id, arrayIdent);
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

    private Operand constantDuplicate(IntermediateTree irTree, Operand constant, Instruction constantInstr) {
        Instruction duplicate = getDuplicateInstruction(irTree.constants.dominatorTree[Operators.constant.ordinal()], constantInstr);
        if (duplicate != null) {
            constant.valGenerator = duplicate.IDNum;
            constant.returnVal = duplicate;
            Instruction.instrNum--;
        } else {
            irTree.constants.instructions.add(constantInstr);
            irTree.constants.instructionIDs.add(constantInstr.IDNum);

            constant = new Operand(true, constant.constVal, constantInstr.IDNum, -1);
            constant.returnVal = constantInstr;
            InstructionLinkedList node = new InstructionLinkedList();
            node.value = constantInstr;
            node.previous = irTree.constants.dominatorTree[Operators.constant.ordinal()];
            irTree.constants.dominatorTree[Operators.constant.ordinal()] = node;
        }
        return constant;
    }

    private Operand loadDuplicate() {
        return null;
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

        boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!instruction.firstOp.constant || !instruction.secondOp.constant);
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
                boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!negInstr.firstOp.constant);
                if (duplicate != null && !allowdupl) {
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
                result.returnVal = negInstr;
                Instruction duplicate = getDuplicateInstructionSingleOp(irTree.current.dominatorTree[Operators.neg.ordinal()], negInstr);
                boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!negInstr.firstOp.constant);
                if (duplicate != null && !allowdupl) {
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
                boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!negInstr.firstOp.constant);
                if (duplicate != null && !allowdupl) {
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
                Instruction loadInstr = null;
                if (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingThirdBracketDefaultId.ordinal()) {
                    Operand load = arrayDesignator(irTree, ident);
                    loadInstr = new Instruction(Operators.load, load); //check dupl
                    loadInstr.arrayID = ident.id;

                    result = new Operand(false, -1, loadInstr.IDNum, -1);
                    result.returnVal = loadInstr;
//                    irTree.current.instructions.add(loadInstr); //check
                    Instruction duplicate = getDuplicateInstructionLoad(irTree.current.dominatorTree[Operators.load.ordinal()], loadInstr);
                    boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock);
                    if (duplicate != null && !allowdupl) {
                        result.valGenerator = duplicate.IDNum;
                        result.returnVal = duplicate;
                        Instruction.instrNum--;
                    } else {
                        InstructionLinkedList node = new InstructionLinkedList();
                        node.value = loadInstr;
                        node.previous = irTree.current.dominatorTree[Operators.load.ordinal()];
                        irTree.current.dominatorTree[Operators.load.ordinal()] = node;
                        irTree.current.instructions.add(loadInstr);
                        irTree.current.instructionIDs.add(loadInstr.IDNum);
                    }
                }
                if (loadInstr == null) {
                    Instruction valueGenerator = irTree.current.valueInstructionMap.get(ident.id);
                    if (valueGenerator == null) {
                        warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                        valueGenerator = assignZeroInstruction;
                    }
                    result = new Operand(false, 0, valueGenerator.IDNum, ident.id);
                    result.returnVal = valueGenerator;
                }
//                } else {
//                    result = new Operand(false, -1, loadInstr.IDNum, -1);
//                    result.returnVal = loadInstr;
//                }

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
            result = funcCall(irTree, false);
            if (token.id != ReservedWords.semicolonDefaultId.ordinal()) {
                token = lexer.nextToken();
            }
        }
        return result;
    }

    private Instruction getDuplicateInstructionLoad(InstructionLinkedList list, Instruction instr) {
        InstructionLinkedList tail = list;
        while (tail != null) {
            if (storeInstruction(tail.value, instr)) {
                return null;
            }
            if (killInstruction(tail.value, instr)) {
                return null;
            }
            if (sameInstructionSingleOp(tail.value, instr)) {
                break;
            }
            tail = tail.previous;
        }
        if (tail == null) {
            return null;
        }
        return tail.value;
    }

    private boolean killInstruction(Instruction value, Instruction instr) {
        if (value.operator == Operators.kill && value.arrayID == instr.arrayID) {
            return true;
        }
        return false;
    }

    private boolean storeInstruction(Instruction value, Instruction instr) {
        if (value.operator == Operators.store && value.arrayID == instr.arrayID) {
            return true;
        }
        return false;
    }

    private Operand arrayDesignator(IntermediateTree irTree, Token ident) throws SyntaxException, IOException {
//        while (token.kind == TokenKind.reservedSymbol && token.id == ReservedWords.startingThirdBracketDefaultId.ordinal()) {
//            token = lexer.nextToken();
//            Operand opp = Expression(irTree);
//
//            token = lexer.nextToken();
//        }
        ArrayIdent arr = irTree.current.arrayMap.get(ident.id);
        Operand curOp;
        ArrayList<Operand> indexes = new ArrayList<>();
        for (int i = 0; i < arr.dimensions.size(); i++) {
            token = lexer.nextToken();
            curOp = Expression(irTree);
            indexes.add(curOp);
            token = lexer.nextToken();
        }
        Operand ret;
        Operand ofs;
        if (indexes.size() == 1) {
            //simple array
            Operand four = new Operand(true, 4, null, -1);
            Instruction constantFour = new Instruction(Operators.constant, four, four);
            four = constantDuplicate(irTree, four, constantFour);
            Instruction mul = new Instruction(Operators.mul, indexes.get(0), four);
            ofs = new Operand(false, -1, mul.IDNum, -1);
            ofs.returnVal = mul;
            Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.mul.ordinal()], mul);
            boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!mul.firstOp.constant || !mul.secondOp.constant);
            if (duplicate != null && !allowdupl) {
                ofs.valGenerator = duplicate.IDNum;
                ofs.returnVal = duplicate;
                Instruction.instrNum--;
            } else {
                InstructionLinkedList node = new InstructionLinkedList();
                node.value = mul;
                node.previous = irTree.current.dominatorTree[Operators.mul.ordinal()];
                irTree.current.dominatorTree[Operators.mul.ordinal()] = node;
                irTree.current.instructions.add(mul);
                irTree.current.instructionIDs.add(mul.IDNum);
            }
        } else {
            Operand op = indexes.get(indexes.size() - 1);
            for (int i = indexes.size() - 2; i >= 0; i--) { //map multi D to 1D storage
                Instruction mul = new Instruction(Operators.mul, op, arr.opDims.get(i));
                op = new Operand(false, -1, mul.IDNum, -1);
                op.returnVal = mul;
                Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.mul.ordinal()], mul);
                boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!mul.firstOp.constant || !mul.secondOp.constant);
                if (duplicate != null && !allowdupl) {
                    op.valGenerator = duplicate.IDNum;
                    op.returnVal = duplicate;
                    Instruction.instrNum--;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = mul;
                    node.previous = irTree.current.dominatorTree[Operators.mul.ordinal()];
                    irTree.current.dominatorTree[Operators.mul.ordinal()] = node;
                    irTree.current.instructions.add(mul);
                    irTree.current.instructionIDs.add(mul.IDNum);
                }

                Instruction add = new Instruction(Operators.add, op, indexes.get(i));
                op = new Operand(false, -1, add.IDNum, -1);
                op.returnVal = add;
                duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.add.ordinal()], add);
                allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!add.firstOp.constant || !add.secondOp.constant);
                if (duplicate != null && !allowdupl) {
                    op.valGenerator = duplicate.IDNum;
                    op.returnVal = duplicate;
                    Instruction.instrNum--;
                } else {
                    InstructionLinkedList node = new InstructionLinkedList();
                    node.value = mul;
                    node.previous = irTree.current.dominatorTree[Operators.add.ordinal()];
                    irTree.current.dominatorTree[Operators.add.ordinal()] = node;
                    irTree.current.instructions.add(add);
                    irTree.current.instructionIDs.add(add.IDNum);
                }
            }
            Operand four = new Operand(true, 4, null, -1);
            Instruction constantFour = new Instruction(Operators.constant, four, four);
            four = constantDuplicate(irTree, four, constantFour);
            Instruction offset = new Instruction(Operators.mul, op, four);
            ofs = new Operand(false, -1, offset.IDNum, -1);
            ofs.returnVal = offset;
            Instruction duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.mul.ordinal()], offset);
            boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!offset.firstOp.constant || !offset.secondOp.constant);
            if (duplicate != null && !allowdupl) {
                ofs.valGenerator = duplicate.IDNum;
                ofs.returnVal = duplicate;
                Instruction.instrNum--;
            } else {
                InstructionLinkedList node = new InstructionLinkedList();
                node.value = offset;
                node.previous = irTree.current.dominatorTree[Operators.mul.ordinal()];
                irTree.current.dominatorTree[Operators.mul.ordinal()] = node;
                irTree.current.instructions.add(offset);
                irTree.current.instructionIDs.add(offset.IDNum);
            }
        }
        Operand FP = new Operand(Registers.FP.name());
        Operand arrayBase = new Operand(arr.getStartingAddress() + Registers.FP.name());
        Instruction base = new Instruction(Operators.add, FP, arrayBase);
        Operand bas = new Operand(false, -1, base.IDNum, -1);
        bas.returnVal = base;
        Instruction duplicate = getDuplicateInstructionFP(irTree.current.dominatorTree[Operators.add.ordinal()], base);
        if (duplicate != null) {
            bas.valGenerator = duplicate.IDNum;
            bas.returnVal = duplicate;
            Instruction.instrNum--;
        } else {
            InstructionLinkedList node = new InstructionLinkedList();
            node.value = base;
            node.previous = irTree.current.dominatorTree[Operators.add.ordinal()];
            irTree.current.dominatorTree[Operators.add.ordinal()] = node;
            irTree.current.instructions.add(base);
            irTree.current.instructionIDs.add(base.IDNum);
        }

        Instruction adda = new Instruction(Operators.adda, ofs, bas);
        duplicate = getDuplicateInstruction(irTree.current.dominatorTree[Operators.adda.ordinal()], adda);
        ret = new Operand(false, -1, adda.IDNum, -1);
        ret.returnVal = adda;
        boolean allowdupl = (irTree.current.isCond || irTree.current.whileBlock) && (!adda.firstOp.constant || !adda.secondOp.constant);
        if (duplicate != null && !allowdupl) {
            ret.valGenerator = duplicate.IDNum;
            ret.returnVal = duplicate;
            Instruction.instrNum--;
        } else {
            InstructionLinkedList node = new InstructionLinkedList();
            node.value = adda;
            node.previous = irTree.current.dominatorTree[Operators.adda.ordinal()];
            irTree.current.dominatorTree[Operators.adda.ordinal()] = node;
            irTree.current.instructions.add(adda);
            irTree.current.instructionIDs.add(adda.IDNum);
        }
        return ret;
    }

    private Instruction getDuplicateInstructionFP(InstructionLinkedList list, Instruction base) {
        InstructionLinkedList tail = list;
        while (tail != null) {
            if (sameInstructionFP(tail.value, base)) {
                break;
            }
            tail = tail.previous;
        }
        if (tail == null) {
            return null;
        }
        return tail.value;
    }

    private boolean sameInstructionFP(Instruction first, Instruction second) {
        if (first.firstOp.arraybase.equals(second.firstOp.arraybase) && first.secondOp.arraybase.equals(second.secondOp.arraybase)) {
            return true;
        }
        return false;
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
//        if (first.valGenerator == second.valGenerator){
//            return true;
//        }
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
            ArrayIdent arr = ifParent.arrayMap.get(identity);
            if (arr != null) {
                Operand killOp = new Operand(true, identity, null, -1);
                Instruction killInstr = new Instruction(Operators.kill, killOp); //check duplicates
                killInstr.arrayID = identity;
                joinBlock.instructions.add(killInstr);
                joinBlock.instructionIDs.add(killInstr.IDNum);
                InstructionLinkedList node = new InstructionLinkedList();
                node.value = killInstr;
                node.previous = joinBlock.dominatorTree[Operators.load.ordinal()];
                joinBlock.dominatorTree[Operators.load.ordinal()] = node;
            } else {
                Instruction ifValueGenerator = ifParent.valueInstructionMap.get(identity);
                if (ifValueGenerator == null) {
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                    ifValueGenerator = assignZeroInstruction;
                }
                Operand firstOp = new Operand(false, 0, ifValueGenerator.IDNum, identity);
                firstOp.returnVal = ifValueGenerator;

                BasicBlock elseParent = joinBlock.parentBlocks.get(1);
                Instruction elseValueGenerator = elseParent.valueInstructionMap.get(identity);
                if (elseValueGenerator == null) {
                    warning(ErrorInfo.UNINITIALIZED_VARIABLE_PARSER_WARNING);
                    elseValueGenerator = assignZeroInstruction;
                }
                Operand secondOp = new Operand(false, 0, elseValueGenerator.IDNum, identity);
                secondOp.returnVal = elseValueGenerator;
                Instruction phiInstruction = new Instruction(Operators.phi, firstOp, secondOp);
                joinBlock.instructions.add(phiInstruction);
                joinBlock.instructionIDs.add(phiInstruction.IDNum);
                joinBlock.valueInstructionMap.put(identity, phiInstruction);
            }
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
        condOp.returnVal = valueGenerator;
        Instruction phiInstruction = new Instruction(Operators.phi, condOp, whileOp);
        condBlock.instructions.add(condBlock.phiIndex, phiInstruction);
        condBlock.instructionIDs.add(phiInstruction.IDNum);
        condBlock.phiIndex++;
        condBlock.valueInstructionMap.put(identity, phiInstruction);
        condBlock.assignedVariables.add(identity);
        whileOp = new Operand(whileOp.constant, whileOp.constVal, phiInstruction.IDNum, whileOp.id);
        whileOp.returnVal = phiInstruction;
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