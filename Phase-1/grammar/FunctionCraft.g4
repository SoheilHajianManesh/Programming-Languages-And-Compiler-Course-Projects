grammar FunctionCraft;


// Parser rules
// The parser rules start with the program rule, which defines the overall structure of a
// valid program. They then specify how tokens can be combined to form declarations, control
// structures, expressions, assignments, function calls, and other constructs within a program.
// The parser rules collectively define the syntax of the language.

// TODO

program
    : (function | pattern | comment)* main
    ;

main
    : DEF MAIN LPAR RPAR {System.out.println("MAIN"); }
    function_body
    END
    ;



function
    : DEF name=IDENTIFIER {System.out.println("FuncDec: " + $name.text);} function_parameter
    function_body
    END
    ;

function_parameter
    :
        LPAR parameter (COMMA paramete_with_default_value)? RPAR
        | LPAR paramete_with_default_value? RPAR
    ;

function_body
    :   (statement | comment)*
    ;

statement
    : ((
    function_return
    |expr
    |assignment) SEMICOLON)
    |if_block
    |loop_do_block
    |for_block
    ;

in_loop_statement
    : ((
    expr
    |function_return
    |assignment
    |stop_control) SEMICOLON)
    |in_loop_if_block
    |loop_do_block
    |for_block
    ;

expr
    :
        expr1 expr_utils
    ;

expr_utils
    :
        APPEND expr1 {System.out.println("Operator: <<"); } expr_utils
        |
    ;
expr1
    :
        expr2 expr1_utils
    ;

expr1_utils
    :
        name=equal_comparative_operators expr2 {System.out.println("Operator: " + $name.text);} expr1_utils
        |
    ;

expr2
    :
        expr3 expr2_utils
    ;

expr2_utils
    :
        name=comparative_operators expr3 {System.out.println("Operator: " + $name.text);} expr2_utils
        |
    ;

expr3
    :
        expr4 expr3_utils
    ;

expr3_utils
    :
         name=low_priority_mathematical_operators expr4 {System.out.println("Operator: " + $name.text);} expr3_utils
        |
    ;

expr4
    :
        expr5 expr4_utils
    ;

expr4_utils
    :
         name=high_priority_mathematical_operators expr5 {System.out.println("Operator: " + $name.text);} expr4_utils
         |
    ;

expr5
    :
        name=prefix_operator expr6 {System.out.println("Operator: " + $name.text);}
        | expr6
    ;

expr6
    :
         expr7 expr6_utils
    ;

expr6_utils
    :
        LBRACKET expr6 RBRACKET expr6_utils
        |

    ;

expr7
    :
        IDENTIFIER expr7_utils
        | value expr7_utils
        | list_value expr7_utils
        | built_in_function_call expr7_utils
        | pattern_call expr7_utils
        | LPAR expr RPAR expr7_utils
        | postfix_operation expr7_utils
        | function_pointer expr7_utils
        | logical_operation expr7_utils
    ;

expr7_utils
    :
    LPAR {System.out.println("Function Call"); } function_argument? RPAR expr7_utils
    |
    ;

function_return
    :
        RETURN {System.out.println("RETURN");} expr?
    ;

pattern_call
    :
        IDENTIFIER DOT MATCH {System.out.println("Built-In: MATCH");} LPAR expr RPAR
    ;

lambda_function
    :
        LAMBDA_FUNCTION_SIGN {System.out.println("Structure: LAMBDA");} function_parameter LBRACE function_body RBRACE
    ;

built_in_function_call
    :
        PUTS {System.out.println("Built-In: PUTS");} LPAR expr RPAR
        | PUSH {System.out.println("Built-In: PUSH");} LPAR expr COMMA expr RPAR
        | LEN {System.out.println("Built-In: LEN");} LPAR expr RPAR
        | CHOP {System.out.println("Built-In: CHOP");} LPAR expr RPAR
        | CHOMP {System.out.println("Built-In: CHOMP");} LPAR expr RPAR
    ;

assignment
    :
        name=IDENTIFIER {System.out.println("Assignment: " + $name.text);} (ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | MULT_ASSIGN | DIV_ASSIGN | REM_ASSIGN)  expr
    ;

if_block
    :
        IF {System.out.println("Decision: IF");} if_argument statement*
        (ELSEIF {System.out.println("Decision: ELSE IF");} if_argument statement*)*
        (ELSE {System.out.println("Decision: ELSE");} statement*)?
        END
    ;

loop_do_block
    :
        LOOP DO {System.out.println("Loop: DO");} in_loop_statement* END
    ;

for_block
    :
        FOR IDENTIFIER {System.out.println("Loop: FOR");} IN (range | expr) END
    ;

range
    :
    LPAR(expr DOT DOT expr)
    ;

in_loop_if_block
    :
        IF {System.out.println("Decision: IF");} if_argument in_loop_statement*
        (ELSEIF {System.out.println("Decision: ELSE IF");} if_argument in_loop_statement*)*
        (ELSE {System.out.println("Decision: ELSE");} in_loop_statement*)?
        END
    ;

function_argument
    :
        expr (COMMA expr)*
    ;

function_pointer
    :
        FPTR
        | lambda_function
    ;

parameter
    :   IDENTIFIER (COMMA IDENTIFIER)*
    ;

paramete_with_default_value
    :   LBRACKET IDENTIFIER ASSIGN value (COMMA IDENTIFIER ASSIGN value)* RBRACKET
    ;

logical_operation
    :
        LPAR expr RPAR name=two_operand_logical_operator {System.out.println("Operator: " + $name.text);} LPAR expr RPAR
        | NOT {System.out.println("Operator: !");} LPAR expr RPAR
    ;

postfix_operation
    :
        IDENTIFIER name=postfix_operator {System.out.println("Operator: " + $name.text);}
    ;

if_argument
    :
        LPAR expr RPAR
    ;

number
    : MINUS?
    (
    INT_VAL
    |FLOAT_VAL
    )
    ;

pattern
    :
    PATTERN name = IDENTIFIER {System.out.println("PatternDec: " + $name.text);}
    LPAR parameter* paramete_with_default_value? RPAR
    (PATTERN_INDENTATION if_argument ASSIGN expr)*
    SEMICOLON
    ;

high_priority_mathematical_operators
    :
    MULT
    |DIV
    |REM
    ;

low_priority_mathematical_operators
    :
    PLUS
    |MINUS
    ;

equal_comparative_operators
    :
        EQL
        | NEQ
    ;

comparative_operators
    :
        LEQ
        | GEQ
        | LES
        | GTR
    ;

two_operand_logical_operator
    :
        AND
        | OR
    ;

postfix_operator
    :
    POSTFIX_MINUS
    |POSTFIX_PLUS
    ;

prefix_operator
    :
    MINUS
    ;


value
    :
    BOOLEAN_VAL
    | STRING_VAL
    | number
    ;

list_value
    :
        LBRACKET (expr (COMMA expr)*)? RBRACKET
    ;

stop_control
    :
    BREAK {System.out.println("Control: BREAK");}
    | BREAK IF {System.out.println("Control: BREAK");} if_argument
    | NEXT {System.out.println("Control: NEXT");}
    | NEXT IF {System.out.println("Control: NEXT");} if_argument
    ;

comment
    :
    SINGLE_LINE_COMMENT|
    MULTI_LINE_COMMENT
    ;



// Lexer rules
// The lexer rules define patterns for recognizing tokens like numbers, booleans, strings,
// comments, keywords, identifiers, and operators in the input text. These rules are used
// by the lexer to break the input into a token stream.

MAIN:             'main';
DEF:               'def';
END:               'end';
RETURN:         'return';
IF:                 'if';
ELSE:             'else';
ELSEIF:         'elseif';
CHOP:             'chop';
CHOMP:           'chomp';
PUSH:             'push';
PUTS:             'puts';
METHOD:         'method';
LEN:               'len';
PATTERN:       'pattern';
MATCH:           'match';
NEXT:             'next';
BREAK:           'break';
LOOP:             'loop';
DO:                 'do';
FOR:               'for';
IN:                 'in';
INT_VAL:     [1-9][0-9]* | [0];
FLOAT_VAL: [0-9]*'.'[0-9]+ | INT_VAL;
STRING_VAL:    '"'.*?'"';
BOOLEAN_VAL:   TRUE|FALSE;
TRUE:             'true';
FALSE:           'false';
FPTR:           METHOD'(:'IDENTIFIER')';
LPAR:                '(';
RPAR:                ')';
LBRACKET:            '[';
RBRACKET:            ']';
POSTFIX_PLUS:       '++';
POSTFIX_MINUS:      '--';
NOT:                 '!';
MULT:                '*';
DIV:                 '/';
REM:                 '%';
PLUS:                '+';
MINUS:               '-';
APPEND:             '<<';
LEQ:                '<=';
GEQ:                '>=';
LES:                 '<';
GTR:                 '>';
EQL:                '==';
NEQ:                '!=';
AND:                '&&';
OR:                 '||';
LOGICAL_OR:          '|';
ASSIGN:              '=';
PLUS_ASSIGN:        '+=';
MINUS_ASSIGN:       '-=';
MULT_ASSIGN:        '*=';
DIV_ASSIGN:         '/=';
REM_ASSIGN:         '%=';
LBRACE:              '{';
RBRACE:              '}';
COMMA:               ',';
DOT:                 '.';
COLON:               ':';
SEMICOLON:           ';';
LAMBDA_FUNCTION_SIGN: '->';
IDENTIFIER:    [a-z][a-zA-Z0-9_]*;
SINGLE_LINE_COMMENT: '#' ~[\r\n]* -> skip;
MULTI_LINE_COMMENT: '=begin'.*?'=end' -> skip;
PATTERN_INDENTATION:    '\r\n\t|' | '\r\n    |';
WS:                 [ \t\r\n]+ -> skip;
