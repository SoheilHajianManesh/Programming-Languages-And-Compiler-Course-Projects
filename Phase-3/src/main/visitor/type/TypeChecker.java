package main.visitor.type;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.*;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.*;
import main.ast.nodes.expression.value.*;
import main.ast.nodes.expression.value.primitive.*;
import main.ast.nodes.statement.*;
import main.ast.type.*;
import main.ast.type.primitiveType.*;
import main.compileError.CompileError;
import main.compileError.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.*;
import main.symbolTable.item.*;
import main.visitor.Visitor;

import java.util.*;

public class TypeChecker extends Visitor<Type> {
    public ArrayList<CompileError> typeErrors = new ArrayList<>();
    @Override
    public Type visit(Program program){
        SymbolTable.root = new SymbolTable();
        SymbolTable.top = new SymbolTable();
        for(FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()){
            FunctionItem functionItem = new FunctionItem(functionDeclaration);
            try {
                SymbolTable.root.put(functionItem);
            }catch (ItemAlreadyExists ignored){}
        }
        for(PatternDeclaration patternDeclaration : program.getPatternDeclarations()){
            PatternItem patternItem = new PatternItem(patternDeclaration);
            try{
                SymbolTable.root.put(patternItem);
            }catch (ItemAlreadyExists ignored){}
        }
        program.getMain().accept(this);
        return null;
    }

    private void helperFunction(ArrayList<Type> returnTypes, Statement statement){
        if(statement instanceof ReturnStatement){
            if(((ReturnStatement) statement).hasRetExpression())
                returnTypes.add(((ReturnStatement) statement).getReturnExp().accept(this));
            else
                returnTypes.add(new NoType());
        }
        else if(statement instanceof ForStatement forStatement){
            for(Statement loopBodyStmt : forStatement.getLoopBodyStmts())
                helperFunction(returnTypes, loopBodyStmt);
        }
        else if(statement instanceof IfStatement ifStatement){
            for(Statement thenBodyStmt : ifStatement.getThenBody())
                helperFunction(returnTypes, thenBodyStmt);

            for(Statement elseBodyStmt : ifStatement.getElseBody())
                helperFunction(returnTypes, elseBodyStmt);
        }
        else if(statement instanceof LoopDoStatement loopDoStatement) {
            for (Statement loopBodyStmt : loopDoStatement.getLoopBodyStmts())
                helperFunction(returnTypes, loopBodyStmt);
        }
        else
            statement.accept(this);
    }

    @Override
    public Type visit(FunctionDeclaration functionDeclaration){
        SymbolTable.push(new SymbolTable());

        try {
            FunctionItem functionItem = (FunctionItem) SymbolTable.root.getItem(FunctionItem.START_KEY +
                    functionDeclaration.getFunctionName().getName());
            ArrayList<Type> currentArgTypes = functionItem.getArgumentTypes();
            for (int i = 0; i < currentArgTypes.size(); i++) {
                VarItem argItem = new VarItem(functionDeclaration.getArgs().get(i).getName());
                argItem.setType(currentArgTypes.get(i));
                try {
                    SymbolTable.top.put(argItem);
                }catch (ItemAlreadyExists ignored){}
            }
            for (int i = 0; i < functionDeclaration.getArgs().size() - currentArgTypes.size(); i++) {
                VarItem argItem = new VarItem(functionDeclaration.getArgs().get(i + currentArgTypes.size()).getName());
                argItem.setType(functionDeclaration.getArgs().get(i + currentArgTypes.size()).getDefaultVal().accept(this));
                try {
                    SymbolTable.top.put(argItem);
                }catch(ItemAlreadyExists ignored){}
            }
        }catch (ItemNotFound ignored){}
        //TODO:Figure out whether return types of functions are not the same. DONE

        ArrayList<Type> returnTypes = new ArrayList<>();
        for(Statement statement : functionDeclaration.getBody())
            helperFunction(returnTypes, statement);

        if(returnTypes.isEmpty())
            returnTypes.add(new NoType());
        else{
            Type firstType = returnTypes.get(0);
            for(int i=1; i<returnTypes.size(); i++) {
                if(!firstType.sameType(returnTypes.get(i))){
                    if(firstType instanceof NoType && returnTypes.get(i) instanceof NoType)
                        continue;
                    typeErrors.add(new FunctionIncompatibleReturnTypes(functionDeclaration.getLine(), functionDeclaration.getFunctionName().getName()));
                    SymbolTable.pop();
                    return new NoType();
                }
            }
        }
        //TODO:Return the infered type of the function DONE
        SymbolTable.pop();
        return returnTypes.get(0);
    }
    @Override
    public Type visit(PatternDeclaration patternDeclaration){
        SymbolTable.push(new SymbolTable());
        try {
            PatternItem patternItem = (PatternItem) SymbolTable.root.getItem(PatternItem.START_KEY +
                    patternDeclaration.getPatternName().getName());
            VarItem varItem = new VarItem(patternDeclaration.getTargetVariable());
            varItem.setType(patternItem.getTargetVarType());
            try {
                SymbolTable.top.put(varItem);
            }catch (ItemAlreadyExists ignored){}
            for(Expression expression : patternDeclaration.getConditions()){
                if(!(expression.accept(this) instanceof BoolType)){
                    typeErrors.add(new ConditionIsNotBool(expression.getLine()));
                    SymbolTable.pop();
                    return new NoType();
                }
            }
        //TODO:1-figure out whether return expression of different cases in pattern are of the same type/2-return the infered type DONE
            ArrayList<Type> returnTypes = new ArrayList<>();
            for(Expression returnExpr : patternDeclaration.getReturnExp()){
                Type type = returnExpr.accept(this);
                returnTypes.add(type);
            }
            if(returnTypes.isEmpty())
                returnTypes.add(new NoType());
            else {
                Type firstType = returnTypes.get(0);
                for (int i = 1; i < returnTypes.size(); i++) {
                    if (!firstType.sameType(returnTypes.get(i))) {
                        typeErrors.add(new PatternIncompatibleReturnTypes(patternDeclaration.getLine(), patternDeclaration.getPatternName().getName()));
                        SymbolTable.pop();
                        return new NoType();
                    }
                }
            }
            SymbolTable.pop();
            return returnTypes.get(0);
        }catch (ItemNotFound ignored){}

        SymbolTable.pop();
        return new NoType();
    }
    @Override
    public Type visit(MainDeclaration mainDeclaration){
        //TODO:visit main DONE
        for(Statement statement : mainDeclaration.getBody())
            statement.accept(this);
        return new NoType();
    }
    @Override
    public Type visit(AccessExpression accessExpression){
        if(accessExpression.isFunctionCall()){
            //TODO:function is called here.set the arguments type and visit its declaration DONE
            String functionName = ((Identifier) accessExpression.getAccessedExpression()).getName();;
            FunctionItem functionItem;
            try {
                if(accessExpression.getAccessedExpression() instanceof Identifier){
                    functionItem = (FunctionItem) SymbolTable.root.getItem(FunctionItem.START_KEY + functionName);
                    ArrayList<Type> currentArgTypes = functionItem.getArgumentTypes();
                    currentArgTypes.clear();

                    for (int i = 0; i < accessExpression.getArguments().size(); i++) {
                        Type argType = accessExpression.getArguments().get(i).accept(this);
                        currentArgTypes.add(argType);
                    }
                    return functionItem.getFunctionDeclaration().accept(this);
                }
            } catch (ItemNotFound ignored) {
                if(accessExpression.getAccessedExpression().accept(this) instanceof FptrType fptrType) {
                    try{
                        SymbolTable.top.getItem(VarItem.START_KEY + functionName);
                        functionItem = (FunctionItem) SymbolTable.root.getItem(FunctionItem.START_KEY + fptrType.getFunctionName());
                        ArrayList<Type> currentArgTypes = functionItem.getArgumentTypes();
                        currentArgTypes.clear();

                        for (int i = 0; i < accessExpression.getArguments().size(); i++) {
                            Type argType = accessExpression.getArguments().get(i).accept(this);
                            currentArgTypes.add(argType);
                        }
                        return functionItem.getFunctionDeclaration().accept(this);
                    }
                    catch (ItemNotFound ignored2) {}
                }
            }

        }
        else{
            Type accessedType = accessExpression.getAccessedExpression().accept(this);
            if(!(accessedType instanceof StringType) && !(accessedType instanceof ListType)){
                typeErrors.add(new IsNotIndexable(accessExpression.getLine()));
                return new NoType();
            }
            //TODO:index of access list must be int DONE
            for(Expression expression : accessExpression.getDimentionalAccess())
                if(!(expression.accept(this) instanceof IntType)){
                    typeErrors.add(new AccessIndexIsNotInt(expression.getLine()));
                    return new NoType();
                }
            if(accessedType instanceof StringType)
                return new StringType();
            else
                return ((ListType) accessedType).getType();
        }
        return new NoType();
    }

    @Override
    public Type visit(ReturnStatement returnStatement){
        // TODO:Visit return statement.Note that return type of functions are specified here DONE
        if(returnStatement.hasRetExpression())
            return returnStatement.getReturnExp().accept(this);
        else
            return new NoType();
    }
    @Override
    public Type visit(ExpressionStatement expressionStatement){
        return expressionStatement.getExpression().accept(this);

    }
    @Override
    public Type visit(ForStatement forStatement){
        SymbolTable.push(SymbolTable.top.copy());
        Type checkType = forStatement.getRangeExpression().accept(this);
        VarItem varItem = new VarItem(forStatement.getIteratorId());
        if((forStatement.getRangeExpression().getRangeType() == RangeType.IDENTIFIER
        || forStatement.getRangeExpression().getRangeType() == RangeType.LIST) && !(checkType instanceof NoType)){
            ListType type = (ListType) forStatement.getRangeExpression().accept(this);
            varItem.setType(type.getType());
        }
        else if(forStatement.getRangeExpression().getRangeType() == RangeType.DOUBLE_DOT && !(checkType instanceof NoType))
            varItem.setType(new IntType());

        try{
            SymbolTable.top.put(varItem);
        }catch (ItemAlreadyExists ignored){}

        for(Statement statement : forStatement.getLoopBodyStmts())
            statement.accept(this);
        SymbolTable.pop();
        return null;
    }
    @Override
    public Type visit(IfStatement ifStatement){
        SymbolTable.push(SymbolTable.top.copy());
        for(Expression expression : ifStatement.getConditions())
            if(!(expression.accept(this) instanceof BoolType))
                typeErrors.add(new ConditionIsNotBool(expression.getLine()));
        for(Statement statement : ifStatement.getThenBody())
            statement.accept(this);
        for(Statement statement : ifStatement.getElseBody())
            statement.accept(this);
        SymbolTable.pop();
        return new NoType();
    }
    @Override
    public Type visit(LoopDoStatement loopDoStatement){
        SymbolTable.push(SymbolTable.top.copy());
        for(Statement statement : loopDoStatement.getLoopBodyStmts())
            statement.accept(this);
        SymbolTable.pop();
        return new NoType();
    }
    @Override
    public Type visit(AssignStatement assignStatement) {
        Type assignExpressionType = assignStatement.getAssignExpression().accept(this);
        Type identifierType = assignStatement.getAssignedId().accept(this);
        if (assignStatement.isAccessList()) {
            // TODO:assignment to list DONE
            if (!(identifierType instanceof ListType) && !(identifierType instanceof StringType)) {
                typeErrors.add(new IsNotIndexable(assignStatement.getLine()));
                return new NoType();
            }
            Type accessListExpressionType = assignStatement.getAccessListExpression().accept(this);
            if (!(accessListExpressionType instanceof IntType)) {
                typeErrors.add(new AccessIndexIsNotInt(assignStatement.getLine()));
                return new NoType();
            }
            if (identifierType instanceof ListType) {
                if (!assignExpressionType.sameType(((ListType) identifierType).getType())) {
                    typeErrors.add(new ListElementsTypesMisMatch(assignStatement.getLine()));
                    return new NoType();
                }
                else if (assignStatement.getAssignOperator() != AssignOperator.ASSIGN
                        && !(((ListType) identifierType).getType() instanceof IntType || ((ListType) identifierType).getType() instanceof FloatType)) {
                    typeErrors.add(new UnsupportedOperandType(assignStatement.getLine(), assignStatement.getAssignOperator().toString()));
                    return new NoType();
                }
            }
            else {
                if (!(assignExpressionType instanceof StringType)) {
                    typeErrors.add(new ListElementsTypesMisMatch(assignStatement.getLine()));
                    return new NoType();
                }
                if(assignStatement.getAssignOperator() != AssignOperator.ASSIGN){
                    typeErrors.add(new UnsupportedOperandType(assignStatement.getLine(), assignStatement.getAssignOperator().toString()));
                    return new NoType();
                }
            }
        }

        else if(assignStatement.getAssignOperator() == AssignOperator.ASSIGN){
            // TODO:maybe new type for a variable DONE
            try{
                VarItem varItem = (VarItem) SymbolTable.top.getItem(VarItem.START_KEY + assignStatement.getAssignedId().getName());
                varItem.setType(assignExpressionType);
            }
            catch (ItemNotFound ignored){
                VarItem newVarItem = new VarItem(assignStatement.getAssignedId());
                newVarItem.setType(assignExpressionType);
                try {
                    SymbolTable.top.put(newVarItem);
                }catch (ItemAlreadyExists ignored2){}
            }
        }
        else if(!(identifierType instanceof IntType || identifierType instanceof FloatType)) {
                typeErrors.add(new UnsupportedOperandType(assignStatement.getLine(), assignStatement.getAssignOperator().toString()));
                return new NoType();
        }
        return new NoType();
    }

    @Override
    public Type visit(BreakStatement breakStatement){
        for(Expression expression : breakStatement.getConditions())
            if(!((expression.accept(this)) instanceof BoolType))
                typeErrors.add(new ConditionIsNotBool(expression.getLine()));

        return null;
    }
    @Override
    public Type visit(NextStatement nextStatement){
        for(Expression expression : nextStatement.getConditions())
            if(!((expression.accept(this)) instanceof BoolType))
                typeErrors.add(new ConditionIsNotBool(expression.getLine()));

        return null;
    }
    @Override
    public Type visit(PushStatement pushStatement){
        //TODO:visit push statement DONE
        Type initialType = pushStatement.getInitial().accept(this);
        Type toBeAddedType = pushStatement.getToBeAdded().accept(this);
        if(initialType instanceof ListType){
            if(((ListType)initialType).getType() instanceof NoType)
                ((ListType)initialType).setType(toBeAddedType);
            else if(!toBeAddedType.sameType(((ListType)initialType).getType())){
                typeErrors.add(new PushArgumentsTypesMisMatch(pushStatement.getLine()));
                return new NoType();
            }
        }
        else if(initialType instanceof StringType){
            if(!(toBeAddedType instanceof StringType)){
                typeErrors.add(new PushArgumentsTypesMisMatch(pushStatement.getLine()));
                return new NoType();
            }
        }
        else{
            typeErrors.add(new IsNotPushedable(pushStatement.getLine()));
            return new NoType();
        }
        return new NoType();
    }
    @Override
    public Type visit(PutStatement putStatement){
        //TODO:visit putStatement DONE
        putStatement.getExpression().accept(this);
        return new NoType();
    }
    @Override
    public Type visit(BoolValue boolValue){
        return new BoolType();
    }
    @Override
    public Type visit(IntValue intValue){
        return new IntType();
    }
    @Override
    public Type visit(FloatValue floatValue){return new FloatType();}
    @Override
    public Type visit(StringValue stringValue){
        return new StringType();
    }
    @Override
    public Type visit(ListValue listValue){
        // TODO:visit listValue DONE
        ArrayList<Type> listTypes = new ArrayList<>();
        for(Expression expression : listValue.getElements()){
            Type type = expression.accept(this);
            listTypes.add(type);
        }
        if(listTypes.isEmpty())
            listTypes.add(new NoType());
        else {
            Type firstType = listTypes.get(0);
            for (int i = 1; i < listTypes.size(); i++) {
                if (!firstType.sameType(listTypes.get(i))) {
                    typeErrors.add(new ListElementsTypesMisMatch(listValue.getLine()));
                    return new NoType();
                }
            }
        }
        return new ListType(listTypes.get(0));
    }
    @Override
    public Type visit(FunctionPointer functionPointer){
        return new FptrType(functionPointer.getId().getName());
    }
    @Override
    public Type visit(AppendExpression appendExpression){
        Type appendeeType = appendExpression.getAppendee().accept(this);
        if(!(appendeeType instanceof ListType) && !(appendeeType instanceof StringType)){
            typeErrors.add(new IsNotAppendable(appendExpression.getLine()));
            return new NoType();
        }
        return appendeeType;
    }
    @Override
    public Type visit(BinaryExpression binaryExpression){
        //TODO:visit binary expression DONE
        Type firstOperandType = binaryExpression.getFirstOperand().accept(this);
        Type secondOperandType = binaryExpression.getSecondOperand().accept(this);
        if(!firstOperandType.sameType(secondOperandType)){
            typeErrors.add(new NonSameOperands(binaryExpression.getLine(), binaryExpression.getOperator()));
            return new NoType();
        }
        if(!(firstOperandType instanceof IntType) && !(firstOperandType instanceof FloatType)){
            typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), binaryExpression.getOperator().toString()));
            return new NoType();
        }
        if(binaryExpression.getOperator() == BinaryOperator.PLUS
                ||binaryExpression.getOperator() == BinaryOperator.MINUS
            ||binaryExpression.getOperator() == BinaryOperator.MULT
            || binaryExpression.getOperator() == BinaryOperator.DIVIDE)
            return firstOperandType;
        else
            return new BoolType();
    }
    @Override
    public Type visit(UnaryExpression unaryExpression){
        //TODO:visit unaryExpression DONE
        Type operandType = unaryExpression.getExpression().accept(this);
        if (unaryExpression.getOperator().equals(UnaryOperator.NOT)) {
            if(!(operandType instanceof BoolType)){
                typeErrors.add(new UnsupportedOperandType(unaryExpression.getLine(), unaryExpression.getOperator().toString()));
                return new NoType();
            }
            else return new BoolType();
        }
        else {
            if (!(operandType instanceof IntType) && !(operandType instanceof FloatType)) {
                typeErrors.add(new UnsupportedOperandType(unaryExpression.getLine(), unaryExpression.getOperator().toString()));
                return new NoType();
            } else {
                return operandType;
            }
        }
    }
    @Override
    public Type visit(ChompStatement chompStatement){
        if (!(chompStatement.getChompExpression().accept(this) instanceof StringType)) {
            typeErrors.add(new ChompArgumentTypeMisMatch(chompStatement.getLine()));
            return new NoType();
        }

        return new StringType();
    }
    @Override
    public Type visit(ChopStatement chopStatement){
        return new StringType();
    }
    @Override
    public Type visit(Identifier identifier){
        // TODO:visit Identifier DONE
        try{
            VarItem varItem = (VarItem) (SymbolTable.top.getItem(VarItem.START_KEY + identifier.getName()));
            return varItem.getType();
        }catch (ItemNotFound ignored){
            return new NoType();
        }
    }
    @Override
    public Type visit(LenStatement lenStatement){
        //TODO:visit LenStatement.Be carefull about the return type of LenStatement. DONE
        Type type = lenStatement.getExpression().accept(this);
        if(!(type instanceof StringType) && !(type instanceof ListType)) {
            typeErrors.add(new LenArgumentTypeMisMatch(lenStatement.getLine()));
            return new NoType();
        }
        return new IntType();
    }
    @Override
    public Type visit(MatchPatternStatement matchPatternStatement){
        try{
            PatternItem patternItem = (PatternItem)SymbolTable.root.getItem(PatternItem.START_KEY +
                    matchPatternStatement.getPatternId().getName());
            patternItem.setTargetVarType(matchPatternStatement.getMatchArgument().accept(this));
            return patternItem.getPatternDeclaration().accept(this);
        }catch (ItemNotFound ignored){}
        return new NoType();
    }
    @Override
    public Type visit(RangeExpression rangeExpression) {
        // TODO --> mind that the lists are declared explicitly in the grammar in this node, so handle the errors DONE
        RangeType rangeType = rangeExpression.getRangeType();

        if (rangeType.equals(RangeType.LIST)) {
            ArrayList<Type> listTypes = new ArrayList<>();
            for (Expression expression : rangeExpression.getRangeExpressions()) {
                Type type = expression.accept(this);
                listTypes.add(type);
            }
            if(listTypes.isEmpty())
                listTypes.add(new NoType());
            else {
                Type firstType = listTypes.get(0);
                for (int i = 1; i < listTypes.size(); i++) {
                    if (!firstType.sameType(listTypes.get(i))) {
                        typeErrors.add(new ListElementsTypesMisMatch(rangeExpression.getLine()));
                        return new NoType();
                    }
                }
            }
            return new ListType(listTypes.get(0));
        } else if (rangeType.equals(RangeType.IDENTIFIER)) {
            Type type = rangeExpression.getRangeExpressions().get(0).accept(this);
            if (!(type instanceof ListType)) {
                typeErrors.add(new IsNotIterable(rangeExpression.getLine()));
                return new NoType();
            }
            return type;
        } else if (rangeType.equals(RangeType.DOUBLE_DOT)) {
            Type firstType = rangeExpression.getRangeExpressions().get(0).accept(this);
            Type secondType = rangeExpression.getRangeExpressions().get(1).accept(this);
            if (!(firstType instanceof IntType) || !(secondType instanceof IntType)) {
                typeErrors.add(new RangeValuesMisMatch(rangeExpression.getLine()));
                return new NoType();
            }
            return firstType;
        }
        return new NoType();
    }
}
