package main.visitor.codeGenerator;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.FunctionDeclaration;
import main.ast.nodes.declaration.MainDeclaration;
import main.ast.nodes.declaration.VarDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.value.FunctionPointer;
import main.ast.nodes.expression.value.ListValue;
import main.ast.nodes.expression.value.primitive.BoolValue;
import main.ast.nodes.expression.value.primitive.IntValue;
import main.ast.nodes.expression.value.primitive.StringValue;
import main.ast.nodes.statement.*;
import main.ast.type.FptrType;
import main.ast.type.ListType;
import main.ast.type.NoType;
import main.ast.type.Type;
import main.ast.type.primitiveType.BoolType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExists;
import main.symbolTable.exceptions.ItemNotFound;
import main.symbolTable.item.FunctionItem;
import main.symbolTable.item.VarItem;
import main.visitor.Visitor;
import main.visitor.type.TypeChecker;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

public class CodeGenerator extends Visitor<String> {
    private final String outputPath;
    private FileWriter mainFile;
    private final TypeChecker typeChecker;
    private final Set<String> visited;
    private FunctionItem curFunction;
    private final HashMap<String, Integer> slots = new HashMap<>();
    private int curLabel = 0;

    private Stack<String> startLoopLabels;
    private Stack<String> endLoopLabels;

    public CodeGenerator(TypeChecker typeChecker){
        this.typeChecker = typeChecker;
        this.visited = typeChecker.visited;
        outputPath = "./codeGenOutput/";
        prepareOutputFolder();
        startLoopLabels = new Stack<>();
        endLoopLabels = new Stack<>();
    }
    private int slotOf(String var) {
        if (!slots.containsKey(var)) {
            slots.put(var, slots.size());
            return slots.size() - 1;
        }
        return slots.get(var);
    }
    public String getFreshLabel(){
        String fresh = "Label_" + curLabel;
        curLabel++;
        return fresh;
    }
    public String getType(Type element,boolean wantSignature){
        String type = "";
        switch (element){
            case StringType stringType -> type += "Ljava/lang/String;";
            case IntType intType -> {
                if (wantSignature) {
                    type += "I";
                } else {
                    type += "Ljava/lang/Integer;";
                }
            }
            case FptrType fptrType -> type += "LFptr;";
            case ListType listType -> type += "Ljava/util/ArrayList;";
            case BoolType boolType -> {
                if (wantSignature) {
                    type += "Z";
                } else {
                    type += "Ljava/lang/Boolean;";
                }
            }
            case null, default -> {
                type += "V";
            }
        }
        return type;
    }
    public String getClass(Type element){
        String className = "";
        switch (element){
            case StringType stringType -> className += "java/lang/String";
            case IntType intType -> className += "java/lang/Integer";
            case BoolType boolType -> className += "java/lang/Boolean";
            case null -> className += "java/lang/Object";
            default -> {}
        }
        return className;
    }
    private void prepareOutputFolder(){
        String jasminPath = "utilities/jarFiles/jasmin.jar";
        String listClassPath = "utilities/codeGenerationUtilityClasses/List.j";
        String fptrClassPath = "utilities/codeGenerationUtilityClasses/Fptr.j";
        try{
            File directory = new File(this.outputPath);
            File[] files = directory.listFiles();
            if(files != null)
                for (File file : files)
                    file.delete();
            directory.mkdir();
        }
        catch(SecurityException e){
            // ignore
        }
        copyFile(jasminPath, this.outputPath + "jasmin.jar");
        copyFile(listClassPath, this.outputPath + "List.j");
        copyFile(fptrClassPath, this.outputPath + "Fptr.j");

        try {
            String path = outputPath + "Main.j";
            File file = new File(path);
            file.createNewFile();
            mainFile = new FileWriter(path);
        } catch (IOException e){
            // ignore
        }
    }
    private void copyFile(String toBeCopied, String toBePasted){
        try {
            File readingFile = new File(toBeCopied);
            File writingFile = new File(toBePasted);
            InputStream readingFileStream = new FileInputStream(readingFile);
            OutputStream writingFileStream = new FileOutputStream(writingFile);
            byte[] buffer = new byte[1024];
            int readLength;
            while ((readLength = readingFileStream.read(buffer)) > 0)
                writingFileStream.write(buffer, 0, readLength);
            readingFileStream.close();
            writingFileStream.close();
        } catch (IOException e){
            // ignore
        }
    }
    private void addCommand(String command){
        try {
            command = String.join("\n\t\t", command.split("\n"));
            if(command.startsWith("Label_"))
                mainFile.write("\t" + command + "\n");
            else if(command.startsWith("."))
                mainFile.write(command + "\n");
            else
                mainFile.write("\t\t" + command + "\n");
            mainFile.flush();
        } catch (IOException e){
            // ignore
        }
    }
    private void handleMainClass(){
        String commands = """
                .method public static main([Ljava/lang/String;)V
                .limit stack 128
                .limit locals 128
                new Main
                invokespecial Main/<init>()V
                return
                .end method
                """;
        addCommand(commands);
    }

    @Override
    public String visit(Program program){
        String commands = """
                .class public Main
                .super java/lang/Object
                """;
        addCommand(commands);
        handleMainClass();

        for(String funcName : this.visited) {
            try {
                this.curFunction = (FunctionItem) SymbolTable.root.getItem(FunctionItem.START_KEY +
                        funcName);
                this.curFunction.getFunctionDeclaration().accept(this);
            } catch(ItemNotFound ignored) {}
        }

        program.getMain().accept(this);
        return null;
    }
    @Override
    public String visit(FunctionDeclaration functionDeclaration){
        slots.clear();
        SymbolTable.push(new SymbolTable());
        String commands = "";
        String argsSignature = "("; // TODO and add to the slots
        for (int i=0; i<this.curFunction.getArgumentTypes().size(); i++) {
            Type argType = this.curFunction.getArgumentTypes().get(i);
            argsSignature += getType(argType, true);
            slotOf(functionDeclaration.getArgs().get(i).getName().getName());
            VarItem newVarItem = new VarItem(functionDeclaration.getArgs().get(i).getName());
            newVarItem.setType(argType);
            try {
                SymbolTable.top.put(newVarItem);
            }catch (ItemAlreadyExists ignored){
                try {
                    newVarItem = (VarItem) SymbolTable.top.getItem(VarItem.START_KEY + newVarItem.getName());
                    newVarItem.setType(argType);
                } catch (ItemNotFound ignored1) {}
            }
        }
        argsSignature += ")";
        String returnType = getType(this.curFunction.getReturnType(),true); // TODO

        commands += ".method public static " + functionDeclaration.getFunctionName().getName();
        commands += argsSignature + returnType + "\n";
        // TODO headers, body and return with corresponding type
        commands += ".limit stack 128\n";
        commands += ".limit locals 128\n";
        for (Statement statement : functionDeclaration.getBody()) {
            commands += statement.accept(this);
        }
        commands += "return\n";
        commands += ".end method\n";
        addCommand(commands);
        SymbolTable.pop();
        return null;
    }
    @Override
    public String visit(MainDeclaration mainDeclaration){
        slots.clear();

        String commands = "";
        commands += ".method public <init>()V\n";
        commands += ".limit stack 128\n";
        commands += ".limit locals 128\n";
        commands += "aload_0\n";
        commands += "invokespecial java/lang/Object/<init>()V\n";
        for (var statement : mainDeclaration.getBody())
            commands += statement.accept(this);
        commands += "return\n";
        commands += ".end method\n";

        addCommand(commands);
        return null;
    }
    public String visit(AccessExpression accessExpression){
        String commands= "";
        if (accessExpression.isFunctionCall()) {
            Identifier funcId = (Identifier)accessExpression.getAccessedExpression();
            Type idType = funcId.accept(typeChecker);
            String functionName = (idType instanceof FptrType fptrType) ? fptrType.getFunctionName() :
                    funcId.getName();
            String argsSignature = "("; // TODO
            for (Expression arg : accessExpression.getArguments()) {
                argsSignature += getType(arg.accept(typeChecker), true);
                commands += arg.accept(this);
            }
            FunctionItem functionItem = null;
            try {
                functionItem = (FunctionItem) SymbolTable.root.getItem(FunctionItem.START_KEY +
                        functionName);
            } catch(ItemNotFound ignored) {}
            for (int i= accessExpression.getArguments().size();i<functionItem.getArgumentTypes().size();i++) {
                argsSignature += getType(functionItem.getArgumentTypes().get(i), true);
                commands += functionItem.getFunctionDeclaration().getArgs().get(i).getDefaultVal().accept(this);
            }
            argsSignature += ")";
            String returnType = getType(accessExpression.accept(typeChecker),true); // TODO
            commands += "invokestatic Main/" + functionName + argsSignature + returnType + "\n";
        }
        else {
            // TODO
            commands += (accessExpression.getAccessedExpression().accept(this));
            for (Expression expression : accessExpression.getDimentionalAccess()){
                commands += (expression.accept(this));
            }
            ListType type = (ListType)accessExpression.getAccessedExpression().accept(typeChecker);
            commands += ("invokevirtual java/util/ArrayList/get(I)Ljava/lang/Object;\n");
            commands += ("checkcast " + getClass(type.getType()) + "\n");
            if (type.getType() instanceof IntType)
                commands += "invokevirtual java/lang/Integer/intValue()I\n";
            else if (type.getType() instanceof BoolType){
                commands += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            }
        }
        //TODO
        return commands;
    }
    @Override
    public String visit(AssignStatement assignStatement){
        //TODO
        Type assignExpresionType = assignStatement.getAssignExpression().accept(typeChecker);
        String commands="";
        AssignOperator assignOperator = assignStatement.getAssignOperator();

        if(assignStatement.isAccessList()){
            commands += assignStatement.getAssignedId().accept(this);
            commands += assignStatement.getAccessListExpression().accept(this);
            if(assignOperator == AssignOperator.ASSIGN){
                commands += assignStatement.getAssignExpression().accept(this);
                if(assignExpresionType instanceof IntType)
                    commands += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
                else if(assignExpresionType instanceof BoolType)
                    commands += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
                commands += "checkcast " + getClass(null) + "\n";
                commands += "invokevirtual java/util/ArrayList/set(ILjava/lang/Object;)Ljava/lang/Object;\n";
            }
            else{
                commands += assignStatement.getAssignedId().accept(this);
                commands += assignStatement.getAccessListExpression().accept(this);
                commands += "invokevirtual java/util/ArrayList/get(I)Ljava/lang/Object;\n";
                commands += "checkcast " + getClass(new IntType()) + "\n";
                commands += "invokevirtual java/lang/Integer/intValue()I\n";
                commands += assignStatement.getAssignExpression().accept(this);
                if(assignOperator == AssignOperator.PLUS_ASSIGN)
                    commands += "iadd\n";
                else if(assignOperator == AssignOperator.MINUS_ASSIGN)
                    commands += "isub\n";
                else if(assignOperator == AssignOperator.MULT_ASSIGN)
                    commands += "imul\n";
                else if(assignOperator == AssignOperator.DIVIDE_ASSIGN)
                    commands += "idiv\n";
                else if(assignOperator == AssignOperator.MOD_ASSIGN)
                    commands += "irem\n";
                commands += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
                commands += "checkcast " + getClass(null) + "\n";
                commands += "invokevirtual java/util/ArrayList/set(ILjava/lang/Object;)Ljava/lang/Object;\n";
            }
        }
        else{
            if(assignOperator == AssignOperator.ASSIGN){
                commands += assignStatement.getAssignExpression().accept(this);
                VarItem varItem = new VarItem(assignStatement.getAssignedId());
                varItem.setType(assignExpresionType);
                try {
                    SymbolTable.top.put(varItem);
                } catch(ItemAlreadyExists ignored) {
                    try {
                        varItem = (VarItem) SymbolTable.top.getItem(VarItem.START_KEY + varItem.getName());
                        varItem.setType(assignExpresionType);
                    }
                    catch (ItemNotFound ignored_) {}
                }
                if(assignExpresionType instanceof IntType || assignExpresionType instanceof BoolType)
                    commands += "istore " + slotOf(assignStatement.getAssignedId().getName()) + "\n";
                else
                    commands += "astore " + slotOf(assignStatement.getAssignedId().getName()) + "\n";
            }
            else {
                commands += "iload " + slotOf(assignStatement.getAssignedId().getName()) + "\n";
                commands += assignStatement.getAssignExpression().accept(this);
                if(assignOperator == AssignOperator.PLUS_ASSIGN)
                    commands += "iadd\n";
                else if(assignOperator == AssignOperator.MINUS_ASSIGN)
                    commands += "isub\n";
                else if(assignOperator == AssignOperator.MULT_ASSIGN)
                    commands += "imul\n";
                else if(assignOperator == AssignOperator.DIVIDE_ASSIGN)
                    commands += "idiv\n";
                else if(assignOperator == AssignOperator.MOD_ASSIGN)
                    commands += "irem\n";
                commands += "istore " + slotOf(assignStatement.getAssignedId().getName()) + "\n";
            }
        }

        return commands;
    }
    @Override
    public String visit(IfStatement ifStatement){
        //TODO
        String commands = "";
        for(Expression condition : ifStatement.getConditions())
            commands += condition.accept(this);

        String elseLabel = getFreshLabel();
        String endLabel = getFreshLabel();
        commands += "ifeq " + elseLabel + "\n";

        SymbolTable.push(SymbolTable.top.copy());
        for(Statement statement : ifStatement.getThenBody())
            commands += statement.accept(this);
        SymbolTable.pop();
        commands += "goto " + endLabel + "\n";

        commands += elseLabel + ":\n";
        SymbolTable.push(SymbolTable.top.copy());
        for(Statement statement : ifStatement.getElseBody())
            commands += statement.accept(this);
        SymbolTable.pop();
        commands += endLabel + ":\n";
        return commands;
    }
    @Override
    public String visit(PutStatement putStatement){
        //TODO
        String commands = "";
        commands += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
        commands += putStatement.getExpression().accept(this);
        Type expressionType = putStatement.getExpression().accept(typeChecker);
        if(expressionType instanceof IntType)
            commands += "invokevirtual java/io/PrintStream/println(I)V\n";
        else if(expressionType instanceof BoolType)
            commands += "invokevirtual java/io/PrintStream/println(Z)V\n";
        else if(expressionType instanceof StringType)
            commands += "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n";
        return commands;
    }
    @Override
    public String visit(ReturnStatement returnStatement){
        //TODO
        Expression returnExpression = returnStatement.getReturnExp();
        if(!returnStatement.hasRetExpression())
            return "return\n";
        Type returnType = returnExpression.accept(typeChecker);
        String commands = returnExpression.accept(this);
        if(returnType instanceof IntType || returnType instanceof BoolType)
            commands += "ireturn\n";
        else if(returnType instanceof NoType)
            commands += "return\n";
        else
            commands += "areturn\n";
        return commands;
    }
    @Override
    public String visit(ExpressionStatement expressionStatement){
        return expressionStatement.getExpression().accept(this);
    }
    @Override
    public String visit(BinaryExpression binaryExpression){
        //TODO
        String commands = "";
        commands += binaryExpression.getFirstOperand().accept(this);
        commands += binaryExpression.getSecondOperand().accept(this);
        if(binaryExpression.getOperator() == BinaryOperator.PLUS)
            commands += "iadd\n";
        else if(binaryExpression.getOperator() == BinaryOperator.MINUS)
            commands += "isub\n";
        else if(binaryExpression.getOperator() == BinaryOperator.MULT)
            commands += "imul\n";
        else if(binaryExpression.getOperator() == BinaryOperator.DIVIDE)
            commands += "idiv\n";
        else{
            String jumpToTrueLabel = getFreshLabel();
            String jumpToEndLabel = getFreshLabel();
            if(binaryExpression.getOperator() == BinaryOperator.EQUAL)
                commands += "if_icmpeq " + jumpToTrueLabel + "\n";
            else if(binaryExpression.getOperator() == BinaryOperator.NOT_EQUAL)
                commands += "if_icmpne " + jumpToTrueLabel + "\n";
            else if(binaryExpression.getOperator() == BinaryOperator.GREATER_THAN)
                commands += "if_icmpgt " + jumpToTrueLabel + "\n";
            else if(binaryExpression.getOperator() == BinaryOperator.LESS_THAN)
                commands += "if_icmplt " + jumpToTrueLabel + "\n";
            else if(binaryExpression.getOperator() == BinaryOperator.LESS_EQUAL_THAN)
                commands += "if_icmple " + jumpToTrueLabel + "\n";
            else if(binaryExpression.getOperator() == BinaryOperator.GREATER_EQUAL_THAN)
                commands += "if_icmpge " + jumpToTrueLabel + "\n";
            else
                commands += "WTF\n";

            commands += "ldc 0\n";
            commands += "goto " + jumpToEndLabel + "\n";
            commands += jumpToTrueLabel + ":\n";
            commands += "ldc 1\n";
            commands += jumpToEndLabel + ":\n";
        }
        return commands;
    }
    @Override
    public String visit(UnaryExpression unaryExpression){
        //TODO
        String commands = "";
        commands += unaryExpression.getExpression().accept(this);
        UnaryOperator operator = unaryExpression.getOperator();
        if(operator == UnaryOperator.MINUS)
            commands += "ineg\n";
        else if(operator == UnaryOperator.NOT)
            commands += "ldc 1\nixor\n";
        else{
            commands+= (operator==UnaryOperator.INC) ? "ldc 1\niadd\n" : "ldc 1\nisub\n";
            if(unaryExpression.getExpression() instanceof Identifier identifier)
                commands += "istore " + slotOf(identifier.getName()) + "\n";
        }
        return commands;
    }
    @Override
    public String visit(Identifier identifier){
        //TODO
        Type identifierType = identifier.accept(typeChecker);
        if(identifierType instanceof IntType || identifierType instanceof BoolType)
            return "iload " + slotOf(identifier.getName()) + "\n";
        else
            return "aload " + slotOf(identifier.getName()) + "\n";
    }
    @Override
    public String visit(LoopDoStatement loopDoStatement){
        //TODO
        String commands = "";
        String startLabel = getFreshLabel();
        String endLabel = getFreshLabel();
        startLoopLabels.push(startLabel);
        endLoopLabels.push(endLabel);
        commands += startLabel + ":\n";
        SymbolTable.push(SymbolTable.top.copy());
        for(Statement statement : loopDoStatement.getLoopBodyStmts())
            commands += statement.accept(this);
        SymbolTable.pop();
        commands += "goto " + startLabel + "\n";
        commands += endLabel + ":\n";
        startLoopLabels.pop();
        endLoopLabels.pop();
        return commands;
    }
    @Override
    public String visit(BreakStatement breakStatement){
        //TODO
        return "goto " + endLoopLabels.lastElement() + "\n";
    }
    @Override
    public String visit(NextStatement nextStatement){
        //TODO
        return "goto " + startLoopLabels.lastElement() + "\n";
    }
    @Override
    public String visit(LenStatement lenStatement){
        //TODO
        String commands = lenStatement.getExpression().accept(this);
        Type expressionType = lenStatement.getExpression().accept(typeChecker);
        if(expressionType instanceof StringType stringType)
            commands += "invokevirtual java/lang/String/length()I\n";
        else if(expressionType instanceof ListType listType)
            commands += "invokevirtual java/util/ArrayList/size()I\n";
        return commands;
    }
    @Override
    public String visit(ChopStatement chopStatement){
        //TODO
        String commands = "";
        commands += chopStatement.getChopExpression().accept(this);
        commands += "dup\n";
        commands += "invokevirtual java/lang/String/length()I\n";
        commands += "ldc 0\n";
        commands += "swap\n";
        commands += "ldc -1\n";
        commands += "iadd\n";
        commands += "invokevirtual java/lang/String/substring(II)Ljava/lang/String;\n";
        return commands;
    }
    @Override
    public String visit(FunctionPointer functionPointer){
        FptrType fptr = (FptrType) functionPointer.accept(typeChecker);
        String commands = "";
        commands += "new Fptr\n";
        commands += "dup\n";
        commands += "aload_0\n";
        commands += "ldc " + "\"" + fptr.getFunctionName() + "\"\n";
        commands += "invokespecial Fptr/<init>(Ljava/lang/Object;Ljava/lang/String;)V\n";
        return commands;
    }
    @Override
    public String visit(ListValue listValue){
        //TODO
        String commands ="";
        commands += "new java/util/ArrayList\n";
        commands += "dup\n";
        commands += "invokespecial java/util/ArrayList/<init>()V\n";
        commands += "astore " + slotOf("array_slot") + "\n";
        for(Expression element : listValue.getElements()) {
            commands += "aload " + slotOf("array_slot") + "\n";
            commands += element.accept(this);
            Type elementType = element.accept(typeChecker);
            if(elementType instanceof IntType intType)
                commands += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
            else if(elementType instanceof BoolType boolType)
                commands += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
            commands += "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n";
            commands += "pop\n";
        }
        commands += "aload " + slotOf("array_slot") + "\n";
        return commands;
    }
    @Override
    public String visit(IntValue intValue){
        //TODO, use "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer" to convert to primitive
        return "ldc " + intValue.getIntVal() + "\n";
    }
    @Override
    public String visit(BoolValue boolValue){
        //TODO, use "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean" to convert to primitive
        return "ldc " + (boolValue.getBool() ? "1" : "0") + "\n";
    }
    @Override
    public String visit(StringValue stringValue){
        //TODO
        return "ldc " + stringValue.getStr() + "\n";
    }
}