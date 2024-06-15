package main.visitor.nameAnalyzer;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.FunctionDeclaration;
import main.ast.nodes.declaration.MainDeclaration;
import main.ast.nodes.declaration.PatternDeclaration;
import main.ast.nodes.declaration.VarDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.value.FunctionPointer;
import main.ast.nodes.expression.value.ListValue;
import main.ast.nodes.expression.value.primitive.BoolValue;
import main.ast.nodes.expression.value.primitive.FloatValue;
import main.ast.nodes.expression.value.primitive.IntValue;
import main.ast.nodes.expression.value.primitive.StringValue;
import main.ast.nodes.statement.*;
import main.compileError.CompileError;
import main.compileError.nameErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExists;
import main.symbolTable.exceptions.ItemNotFound;
import main.symbolTable.item.FunctionItem;
import main.symbolTable.item.PatternItem;
import main.symbolTable.item.SymbolTableItem;
import main.symbolTable.item.VarItem;
import main.visitor.Visitor;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.Condition;

public class NameAnalyzer extends Visitor<Void> {
    public ArrayList<CompileError> nameErrors = new ArrayList<>();

    @Override
    public Void visit(Program program) {
        SymbolTable.root = new SymbolTable();
        SymbolTable.top = new SymbolTable();

        //addFunctions,

        int duplicateFunctionId = 0;
        ArrayList<FunctionItem> functionItems = new ArrayList<>();
        for (FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()) {
            FunctionItem functionItem = new FunctionItem(functionDeclaration);
            try {
                SymbolTable.root.put(functionItem);
                functionItems.add(functionItem);
            } catch (ItemAlreadyExists e) {
                nameErrors.add(new RedefinitionOfFunction(functionDeclaration.getLine(),
                        functionDeclaration.getFunctionName().getName()));
                duplicateFunctionId += 1;
                String freshName = functionItem.getName() + "#" + String.valueOf(duplicateFunctionId);
                Identifier newId = functionDeclaration.getFunctionName();
                newId.setName(freshName);
                functionDeclaration.setFunctionName(newId);
                FunctionItem newItem = new FunctionItem(functionDeclaration);
                functionItems.add(newItem);
                try {
                    SymbolTable.root.put(newItem);
                } catch (ItemAlreadyExists ignored) {
                }
            }
        }

        //addPatterns
        int duplicatePatternId = 0;
        ArrayList<PatternItem> patternItems = new ArrayList<>();
        for (PatternDeclaration patternDeclaration : program.getPatternDeclarations()) {
            PatternItem patternItem = new PatternItem(patternDeclaration);
            try {
                SymbolTable.root.put(patternItem);
                patternItems.add(patternItem);
            } catch (ItemAlreadyExists e) {
                nameErrors.add(new RedefinitionOfPattern(patternDeclaration.getLine(),
                        patternDeclaration.getPatternName().getName()));
                duplicatePatternId += 1;
                String freshName = patternItem.getName() + "#" + String.valueOf(duplicatePatternId);
                Identifier newId = patternDeclaration.getPatternName();
                newId.setName(freshName);
                patternDeclaration.setPatternName(newId);
                PatternItem newItem = new PatternItem(patternDeclaration);
                patternItems.add(newItem);
                try {
                    SymbolTable.root.put(newItem);
                } catch (ItemAlreadyExists ignored) {
                }
            }
        }
        //visitFunctions

        int visitingFunctionIndex = 0;
        for (FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()) {
            FunctionItem functionItem = functionItems.get(visitingFunctionIndex);
            SymbolTable functionSymbolTable = new SymbolTable();
            functionItem.setFunctionSymbolTable(functionSymbolTable);
            SymbolTable.push(functionSymbolTable);
            functionDeclaration.accept(this);
            SymbolTable.pop();
            visitingFunctionIndex += 1;
        }

        //visitPatterns
        int visitingPatternIndex = 0;
        for (PatternDeclaration patternDeclaration : program.getPatternDeclarations()) {
            PatternItem patternItem = patternItems.get(visitingPatternIndex);
            SymbolTable patternSymbolTable = new SymbolTable();
            patternItem.setPatternSymbolTable(patternSymbolTable);
            SymbolTable.push(patternSymbolTable);
            patternDeclaration.accept(this);
            SymbolTable.pop();
            visitingPatternIndex += 1;
        }
        //visitMain
        program.getMain().accept(this);
        return null;
    }


    @Override
    public Void visit(Identifier identifier) {
        try{
            SymbolTable.top.getItem("VAR:" + identifier.getName());
        }
        catch (ItemNotFound e){
            nameErrors.add(new VariableNotDeclared(identifier.getLine(),
                    identifier.getName()));
        }
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration){
        VarItem varItem = new VarItem(varDeclaration.getName());
        try {
            SymbolTable.top.put(varItem);
        } catch (ItemAlreadyExists e) {
        }
        //TODO : delete getname accept
        if(varDeclaration.getDefaultVal()!=null){
            varDeclaration.getDefaultVal().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration functionDeclaration){
        int duplicateArgId =0;
        for(VarDeclaration varDeclaration : functionDeclaration.getArgs()){
            if(varDeclaration.getName().getName().equals(functionDeclaration.getFunctionName().getName().split("#")[0])){
                nameErrors.add(new IdenticalArgFunctionName(functionDeclaration.getLine(),
                        varDeclaration.getName().getName()));
            }
            varDeclaration.accept(this);
        }
        for(Statement statement : functionDeclaration.getBody()){
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(PatternDeclaration patternDeclaration){
        VarItem varItem = new VarItem(patternDeclaration.getTargetVariable());
        try {
            SymbolTable.top.put(varItem);
        } catch (ItemAlreadyExists e) {
        }
        for(Expression condition : patternDeclaration.getConditions()){
            condition.accept(this);
        }
        for(Expression expression : patternDeclaration.getReturnExp()){
            expression.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(MainDeclaration mainDeclaration){
        for(Statement statement : mainDeclaration.getBody()){
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ReturnStatement returnStatement){
        if(returnStatement.hasRetExpression()){
            returnStatement.getReturnExp().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(IfStatement ifStatement){
        for(Expression condition : ifStatement.getConditions()){
            condition.accept(this);
        }
        SymbolTable ifSymbolTable = new SymbolTable();
        ifSymbolTable.copySymbolTable(SymbolTable.top);
        SymbolTable.push(ifSymbolTable);
        for(Statement statement : ifStatement.getThenBody()){
            statement.accept(this);
        }
        for(Statement statement : ifStatement.getElseBody()){
            statement.accept(this);
        }
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(PutStatement putStatement){
        putStatement.getExpression().accept(this);
        return null;
    }

    @Override
    public Void visit(LenStatement lenStatement){
        lenStatement.getExpression().accept(this);
        return null;
    }

    @Override
    public Void visit(PushStatement pushStatement){
        pushStatement.getInitial().accept(this);
        pushStatement.getToBeAdded().accept(this);
        return null;
    }

    @Override
    public Void visit(LoopDoStatement loopDoStatement){
        SymbolTable loopDoSymbolTable = new SymbolTable();
        loopDoSymbolTable.copySymbolTable(SymbolTable.top);
        SymbolTable.push(loopDoSymbolTable);
        for (Expression condition : loopDoStatement.getLoopConditions()){
            condition.accept(this);
        }
        for (Statement statement : loopDoStatement.getLoopBodyStmts()){
            statement.accept(this);
        }
        if(loopDoStatement.getLoopRetStmt()!=null){
            loopDoStatement.getLoopRetStmt().accept(this);
        }
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(ForStatement forStatement){
        SymbolTable forSymbolTable = new SymbolTable();
        forSymbolTable.copySymbolTable(SymbolTable.top);
        SymbolTable.push(forSymbolTable);
        for (Expression expression : forStatement.getRangeExpressions()){
            expression.accept(this);
        }

        VarItem varItem = new VarItem(forStatement.getIteratorId());
        try {
            SymbolTable.top.put(varItem);
        } catch (ItemAlreadyExists e) {
        }

        for (Expression expression : forStatement.getLoopBodyExpressions()){
            expression.accept(this);
        }
        for (Statement statement : forStatement.getLoopBody()){
            statement.accept(this);
        }
        if(forStatement.getReturnStatement()!=null){
            forStatement.getReturnStatement().accept(this);
        }
        SymbolTable.pop();
        return null;
    }
    @Override
    public Void visit(MatchPatternStatement matchPatternStatement){
        try{
            SymbolTable.root.getItem("Pattern:"+matchPatternStatement.getPatternId().getName());
        }
        catch (ItemNotFound e){
            nameErrors.add(new PatternNotDeclared(matchPatternStatement.getLine(),
                    matchPatternStatement.getPatternId().getName()));
        }
        matchPatternStatement.getMatchArgument().accept(this);
        return null;
    }
    @Override
    public Void visit(ChopStatement chopStatement){
        chopStatement.getChopExpression().accept(this);
        return null;
    }
    @Override
    public Void visit(ChompStatement chompStatement){
        chompStatement.getChompExpression().accept(this);
        return null;
    }
    @Override
    public Void visit(AssignStatement assignStatement){
        if(assignStatement.isAccessList()){
            assignStatement.getAccessListExpression().accept(this);
        }
        assignStatement.getAssignExpression().accept(this);
        if(assignStatement.getAssignOperator()==AssignOperator.ASSIGN){
            try{
                VarItem varItem = new VarItem(assignStatement.getAssignedId());
                SymbolTable.top.put(varItem);
            }
            catch (ItemAlreadyExists e){
            }
        }
        else
            assignStatement.getAssignedId().accept(this);
        return null;
    }

    @Override
    public Void visit(ExpressionStatement expressionStatement){
        expressionStatement.getExpression().accept(this);
        return null;
    }

    @Override
    public Void visit(AppendExpression appendExpression){
        appendExpression.getAppendee().accept(this);
        for (Expression expression : appendExpression.getAppendeds()){
            expression.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(BinaryExpression binaryExpression){
        binaryExpression.getFirstOperand().accept(this);
        binaryExpression.getSecondOperand().accept(this);
        return null;
    }

    @Override
    public Void visit(UnaryExpression unaryExpression){
        unaryExpression.getExpression().accept(this);
        return null;
    }

    public Void checkFunctionCall(AccessExpression accessExpression){
        try {
            if(accessExpression.getAccessedExpression() instanceof Identifier){
                SymbolTableItem symbolTableItem = SymbolTable.root.getItem("Function:" + ((Identifier) accessExpression.getAccessedExpression()).getName());
                FunctionItem functionItem = (FunctionItem) symbolTableItem;
                int maximumArgs = functionItem.getFunctionDeclaration().getArgs().size();
                int actualArgs = accessExpression.getArguments().size();
                if(actualArgs > maximumArgs || actualArgs < (maximumArgs - functionItem.getFunctionDeclaration().defaultArgsNumber())){
                    nameErrors.add(new ArgMisMatch(accessExpression.getAccessedExpression().getLine(),
                            ((Identifier) accessExpression.getAccessedExpression()).getName()));
                }
            }
            else if(accessExpression.getAccessedExpression() instanceof LambdaExpression){
                accessExpression.getAccessedExpression().accept(this);
                int maximumArgs = ((LambdaExpression) accessExpression.getAccessedExpression()).getDeclarationArgs().size();
                int actualArgs = accessExpression.getArguments().size();
                if(actualArgs > maximumArgs || actualArgs < (maximumArgs - ((LambdaExpression) accessExpression.getAccessedExpression()).defaultArgsNumber())) {
                    nameErrors.add(new ArgMisMatch(accessExpression.getAccessedExpression().getLine(),
                            "lambda"));
                }
            }
            else
                accessExpression.getAccessedExpression().accept(this);
        }
        catch (ItemNotFound e){
            nameErrors.add(new FunctionNotDeclared(accessExpression.getAccessedExpression().getLine(),
                    ((Identifier) accessExpression.getAccessedExpression()).getName()));
        }

        for (Expression arg : accessExpression.getArguments()){
            arg.accept(this);
        }

        return null;
    }

    public Void checkRegularExpression(AccessExpression accessExpression){
        accessExpression.getAccessedExpression().accept(this);
        for (Expression expression : accessExpression.getDimentionalAccess()){
            expression.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(AccessExpression accessExpression){
        if(accessExpression.isFunctionCall())
            checkFunctionCall(accessExpression);
        else
            checkRegularExpression(accessExpression);
        return null;
    }

    @Override
    public Void visit(LambdaExpression lambdaExpression){
        SymbolTable lambdaExpressionSymbolTable = new SymbolTable();
        lambdaExpressionSymbolTable.copySymbolTable(SymbolTable.top);
        SymbolTable.push(lambdaExpressionSymbolTable);
        for (VarDeclaration varDeclaration : lambdaExpression.getDeclarationArgs()){
            varDeclaration.accept(this);
        }
        for (Statement statement : lambdaExpression.getBody()){
            statement.accept(this);
        }
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(ListValue listValue){
        for (Expression element : listValue.getElements()){
            element.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionPointer functionPointer){
        try{
            SymbolTable.root.getItem("Function:" + functionPointer.getId().getName());
        }
        catch (ItemNotFound e){
            nameErrors.add(new FunctionNotDeclared(functionPointer.getLine(),
                    functionPointer.getId().getName()));
        }
        return null;
    }


}