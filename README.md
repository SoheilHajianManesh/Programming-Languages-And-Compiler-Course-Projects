# Programming-Languages-and-Compiler-Course-Projects

- [Programming-Languages-and-Compiler-Course-Projects](#programming-languages-and-compiler-course-projects)
  - [Intro](#intro)
    - [Code Structure](#code-structure)
    - [Data Types](#data-types)
    - [Functions](#functions)
    - [Patterns](#patterns)
    - [Lambda Functions](#lambda-functions)
    - [Function Pointers](#function-pointers)
    - [If Statement](#if-statement)
    - [Loops](#loops)
      - [For Loop](#for-loop)
      - [Do Loops](#do-loops)
    - [Default Functions](#default-functions)
      - [puts](#puts)
      - [push](#push)
      - [len](#len)
      - [chop](#chop)
      - [chomp](#chomp)
  - [Phase 1](#phase-1)
  - [Phase 2](#phase-2)
  - [Phase 3](#phase-3)
  - [Phase 4](#phase-4)

## Intro

In this repository, we tried to implement a compiler for a simple language. The compiler is written in java and it uses ANTLR4 for parsing the language and generating the parse tree then convert it to AST and then generates java bytecode using Jasmin. The new language is called `FunctionCraft`, and it is a functional programming language. The formal documentation for language is available in the `FunctionCraft\Documentation` directory.

### Code Structure

Each FunctionCraft program consists of some pattern definitions and some function definitions, then the main function is defined. Here is an example of a FunctionCraft program:

```text
def sum(a, b)
    return a + b;
end

pattern fib(n)
    | (n == 0) = 1
    | (n == 1) = 1
    | (n > 2) = fib (n-1) + fib (n-2)
    # This is a comment
;


def main()
    =begin
    This is also a comment
    =end
    a = 5;
    b = 6;
    c = sum(a, b);
    d = fib.match(5);
    return c + d;
end

```

### Data Types

The language supports the following data types:

- int
- float
- bool
- string
- list
- function pointer

### Functions

functions are defined using `def` keyword. At the end of each scope in this language, `end` keyword comes. functions can also have default values. They can have multiple return statements or none. Here is an example of a function definition:

```text
def f(x, [y = 2])
    if (x > 0)
        return x + y;
    else
        return x - y;
    end
end
```

### Patterns

Patterns are defined using the `pattern` keyword. Patterns are used to match the values of the arguments passed to the function. Patterns can have multiple cases. Here is an example of a pattern definition:

```text
pattern fib(n)
    | (n == 0) = 1
    | (n == 1) = 1
    | (n > 2) = fib (n-1) + fib (n-2)
;
```

### Lambda Functions

Lambda functions are defined using the `->` keyword. Lambda functions can be passed as arguments to other functions. Here is an example of a lambda function definition:

```text
-> (x, y) { return x + y; }
```

### Function Pointers

Function pointers are defined using the `method` keyword. Function pointers can be passed as arguments to other functions. Here is an example of a function pointer definition:

```text
def f(x)
    return x + 1;
end

def main()
    f_ptr = method(:f);
    a = f_ptr(5);
    puts(a);
end
```

### If Statement

Each condition should be in parentheses. Here is an example of an if statement:

```text
if ((a == 5) && (b == 6))
    return 1;
else 
    return 0;
end
```

### Loops

#### For Loop

The for loop is somewhat like the one in Python except for the `:`. Here are the examples of for loops:

```text
for i in 0..10
    puts(i);
end

a = [1, 2, 3, 4];
for i in a
    puts(i);
end
```

#### Do Loops

The do loop is like a while loop but with no conditions. We should use `break` and `next` keywords to control the flow of the program. Here is an example of a do loop:

```text
i = 0;
loop do
    i += 1;
    puts(i);
    if (i == 10)
        break;
    end
end
```

### Default Functions

#### puts

The `puts` function is used to print the value of a variable. Here is an example of using the `puts` function:

```text
def main()
    a = 5;
    puts(a);
end
```

#### push

The `push` function is used to append an element to a list or string. Here is an example of using the `push` function:

```text
def main()
    a = [1, 2, 3];
    push(a, 4);
    puts(a);
end
```

#### len

The `len` function is used to get the length of a list or a string. Here is an example of using the `len` function:

```text
def main()
    a = [1, 2, 3];
    b = len(a);
    puts(b);
end
```

#### chop

The `chop` function is used to remove the last element of a string. Here is an example of using the `chop` function:

```text
def main()
    a = "Hello";
    chop(a);
    puts(a);
end
```

#### chomp

The `chomp` function is used to remove the newline characters (`\n`) from the string. Here is an example of using the `chomp` function:

```text
def main()
    a = "H\nell\no";
    chomp(a);
    puts(a);
end
```

## Phase 1

In the first phase of the project, we implemented designed the grammar for the language and implemented the lexer and parser for the language. The lexer and parser are implemented using `ANTLR4`. The results of the lexer and parser are tested using the provided test cases, you can find tests and their parse tree in the respective directory of this phase. The whole structure is depicted using other graphical methods for better understanding.

A sample program and parse tree is as follows (you can use the `intelij` or `antlr` or use `run.ps1` and `check.ps1` scripts to run the program):

```text
def list_printer(l, i)
end


def printList(l, i)
    puts(l[i][0]);
    puts(l[i][1]);
    printList(l, i - 1); # helllllo
end

def f2 (a, [b = 10, c = 20])
    return a + b + c;
end

def create2dList(l, i, j)
    if(i != 10)
        create2dList(l[i] << i << j * 2, i + 1, j + 1);
    else
        printList(l, i);
    end
end

def main()
    a << b;
    puts(a);
    list_printer([[1, 2, 3], "string", true], 0);
    create2dList([], 0, 0);
    puts(chop(chop(chop("painter"))));
end
```

Parse tree of the above program is as follows:

![Parse Tree](./Phase-1/samples/sample_parse_tree.png)

## Phase 2

In the second phase of the project, first we implemented and generated an abstract syntax tree (AST) for the language. Then we implemented a part of semantic analysis called name analysis. The name analysis is implemented using the visitor pattern. The compiler is designed in a way to gather as much error as possible in one run. Other than normal name checking (for which you can find a list in the description of respective phase), a few additional checks are implemented, one of them is circular dependency check. Also in this phase we implemented a symbol table to store the information of the symbols in the program. The symbol table is implemented using a hash table.

A sample program and detected errors are as follows (you can use the `intelij` to run the program):

```text
def re(re)
    return;
end

pattern fib(fib)
 | (a == 0) = 0
 | (a > 1) = a(n-1) + a(n-2)
;

pattern help(x)
 | (x == 0) = 0
 | (x > 1) = 1
;

pattern fib(n)
 | (n == 0) = 0
 | (n > 1) = no.match(n-1) + help.match(n-2)
;

def y()
    return z();
end

def z()
    return hh();
end

def hh()
    y();
end

def f()
    g();
end

def g()
    return f();
end

def h()
    a = 1;
    b = 2;
    return myfunc2(a, b);
end


def myfunc2(a, b, [c=1])
    return;
end

def myfunc2()
    return;
end


def aa(a, b, c)
    return a+b+c;
end

def main()
    a = 5;
    b = 6;
    puts("salam");
    myFunc(c, b);
    myfunc2(a, b);
    ali(a);
    aa(a, b);
end
```

Errors detected in the above program are:

```text
Line:1-> argument re has same name with function
Line:5-> target variable fib has same name with pattern
Line:6-> variable a is not declared
Line:7-> variable a is not declared
Line:7-> function a is not declared
Line:7-> variable n is not declared
Line:7-> function a is not declared
Line:7-> variable n is not declared
Line:15-> Redefinition of pattern fib
Line:17-> pattern no is not declared
Line:51-> Redefinition of function myfunc2
Line:64-> function myFunc is not declared
Line:64-> variable c is not declared
Line:66-> function ali is not declared
Line:67-> number of arguments provided for function aa does not match with its declaration
*-> defenition of functions hh, z, y contains circular dependency
*-> defenition of functions f, g contains circular dependency
```

## Phase 3

In the third phase of the project, we implemented the type checking part of the semantic analysis. The type checking is implemented using the visitor pattern. The compiler is designed in a way to gather as much error as possible in one run. The type checking is implemented for many different types of expressions and statements. The type checker checks for valid operations and expressions, and for type compatibility. The return type of the functions and the type of the arguments passed to the functions, the type of the variables and the type of the values assigned to the variables are also checked.

A sample program and detected errors are as follows (you can use the `run.ps1` script to run the program):

```text
pattern fib(a)
    | (a>1) = 0
    | (a<2) = 2
;

def f(a, b, [c = 10])
    if (!(c == 10))
        return a+b;
    else
        return a>b;
    end
    return "ali";
end

def g(a, b)
    return a+b;
end

def main()
    b = [1, 3, 4];
    fptr = method(:f);
    b[fptr(1, 2, 30)] = 10;
    fib.match(1);
    a = 10;
    for i in a
       puts(i);
    end
    gg = ++10 + 20;
    fptr(1, 2, gg);
    index = g("hello", "world");
    b[index] = 5;
end
```

Errors detected in the above program are:

```text
Line:6-> types of return expressions of the function `f` must be the same
Line:6-> types of return expressions of the function `f` must be the same
Line:16-> unsupported operand type for operator PLUS
Line:22-> access index must be integer
Line:25-> only lists can be iterated over
Line:31-> access index must be integer
```

## Phase 4

In the fourth phase of the project, we implemented the code generation part of the compiler. The code generation is implemented using the visitor pattern. The compiler generates the code in the `Jasmin` assembly language. The code generation is implemented for many different types of expressions and statements. The generated code can be run by converting it to the `JVM` bytecode using the `Jasmin` assembler, and then run it in the `JVM`. The `Jasmin` can be found in the utilities directory of the project. Also you can use `classFileAnalyzer` to convert the `JVm` bytecode to the `Jasmin` assembly code. The compiled code is then run and the output is stored in the `codeGenOutput` directory of the project. The output of the code generation is tested using the provided test cases, you can find tests and their output in the respective directory of this phase.

A sample program that has been implemented in the language is as follows (you can use the `run.ps1` script to run the program):

```text
def insertionsort(arr, length)
    i = 1;
    loop do
        if (i >= length)
            break;
        end
        j = i;
        loop do
            if (j <= 0)
                break;
            end
            if (arr[j] < arr[j - 1])
                temp = arr[j];
                arr[j] = arr[j - 1];
                arr[j - 1] = temp;
            else
                break;
            end
            j = j - 1;
        end
        i = i + 1;
    end
end

def main() 
    list = [5, 3, 8, 6, 2];
    puts("Original List: ");
    i = 0;
    loop do
        if (i >= 5)
            break;
        end
        puts(list[i]);
        i = i + 1;
    end
    insertionsort(list, 5);
    puts("Sorted List: ");
    i = 0;
    loop do
        if (i >= 5)
            break;
        end
        puts(list[i]);
        i = i + 1;
    end
end
```

output of the above program is:

```text
---------------------------Compilation Successful---------------------------
Original List: 
5
3
8
6
2
Sorted List:
2
3
5
6
8
```
