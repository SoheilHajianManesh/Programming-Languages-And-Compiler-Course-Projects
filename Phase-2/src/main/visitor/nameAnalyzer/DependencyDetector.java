package main.visitor.nameAnalyzer;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.FunctionDeclaration;
import main.ast.nodes.declaration.VarDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.value.FunctionPointer;
import main.ast.nodes.expression.value.ListValue;
import main.ast.nodes.statement.*;
import main.compileError.CompileError;
import main.compileError.nameErrors.CircularDependency;
import main.compileError.nameErrors.FunctionNotDeclared;
import main.compileError.nameErrors.PatternNotDeclared;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExists;
import main.symbolTable.exceptions.ItemNotFound;
import main.symbolTable.item.VarItem;
import main.visitor.Visitor;
import main.symbolTable.utils.Graph;

import java.util.ArrayList;
import java.util.List;

public class DependencyDetector extends Visitor<Void> {
    public ArrayList<CompileError> dependencyError = new ArrayList<>();
    private Graph dependencyGraph = new Graph();
    private String currentFunction;
    @Override
    public Void visit(Program program){
        for(FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()){
            currentFunction = functionDeclaration.getFunctionName().getName();
            functionDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration){
        if(varDeclaration.getDefaultVal()!=null){
            varDeclaration.getDefaultVal().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration functionDeclaration){
        for(Statement statement : functionDeclaration.getBody()){
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
        for(Statement statement : ifStatement.getThenBody()){
            statement.accept(this);
        }
        for(Statement statement : ifStatement.getElseBody()){
            statement.accept(this);
        }
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
        for (Expression condition : loopDoStatement.getLoopConditions()){
            condition.accept(this);
        }
        for (Statement statement : loopDoStatement.getLoopBodyStmts()){
            statement.accept(this);
        }
        if(loopDoStatement.getLoopRetStmt()!=null){
            loopDoStatement.getLoopRetStmt().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ForStatement forStatement){
        for (Expression expression : forStatement.getRangeExpressions()){
            expression.accept(this);
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
        return null;
    }

    @Override
    public Void visit(MatchPatternStatement matchPatternStatement){
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

    public Void checkFunctionCall(AccessExpression accessExpression) {
        if(accessExpression.getAccessedExpression() instanceof Identifier)
            dependencyGraph.addEdge(currentFunction, ((Identifier) accessExpression.getAccessedExpression()).getName());
        else
            accessExpression.getAccessedExpression().accept(this);

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
        for (Statement statement : lambdaExpression.getBody()){
            statement.accept(this);
        }
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
        dependencyGraph.addEdge(currentFunction, functionPointer.getId().getName());
        return null;
    }
    public Void findDependency(){
        ArrayList<List<String>> cycles = dependencyGraph.findCycles();
        for (List<String> cycle : cycles){
            dependencyError.add(new CircularDependency(cycle));
        }
        return null;
    }

}
