grammar FunctionCraft;


// Parser rules
// The parser rules start with the program rule, which defines the overall structure of a
// valid program. They then specify how tokens can be combined to form declarations, control
// structures, expressions, assignments, function calls, and other constructs within a program.
// The parser rules collectively define the syntax of the language.

// TODO

program
    : (function | comment)* main
    ;

main
    : DEF MAIN LPAR RPAR
    function_body
    END
    ;



function
    : DEF IDENTIFIER function_parameter
    function_body
    END
    ;

function_parameter
    :
        LPAR parameter? paramete_with_default_value? RPAR
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
    IDENTIFIER
    | value
    | function_call
    | LPAR expr RPAR
    | expr two_operand_operators expr
    | IDENTIFIER postfix_operator
    | prefix_operator expr
    | lambda_function
    | function_pointer
    | 

    ;

function_return
    :
        RETURN expr?
    ;

function_call
    :
        (IDENTIFIER | lambda_function) LPAR function_argument? RPAR
    ;

lambda_function
    :
        LAMBDA_FUNCTION_SIGN function_parameter LBRACE function_body RBRACE
    ;

built_in_function_call
    :
        PUTS LPAR expr RPAR
        | PUSH LPAR expr COMMA expr RPAR
        | LEN LPAR expr RPAR
        | CHOP LPAR expr RPAR
        | CHOMP LPAR expr RPAR
    ;

assignment
    :
        IDENTIFIER ASSIGN expr
    ;

if_block
    :
        IF if_argument statement*
        (ELSEIF if_argument statement*)*
        (ELSE statement*)?
        END
    ;

loop_do_block
    :
        LOOP DO in_loop_statement* END
    ;

for_block
    :
        FOR IDENTIFIER IN (range | expr) END
    ;

range
    :
    LPAR(expr DOT DOT expr)
    ;

in_loop_if_block
    :
        IF if_argument in_loop_statement*
        (ELSEIF if_argument in_loop_statement*)*
        (ELSE in_loop_statement*)?
        END
    ;

type
    :
    INT
    | STRING
    | FLOAT
    | BOOLEAN
    | LIST
    | FPTR
    ;

function_argument
    :
    expr (COMMA expr)*
    ;

function_pointer
    :
        METHOD LPAR COLON IDENTIFIER RPAR
    ;

parameter
    :   IDENTIFIER (COMMA IDENTIFIER)*
    ;

paramete_with_default_value
    :   LBRACKET IDENTIFIER ASSIGN value (COMMA IDENTIFIER ASSIGN value)* RBRACKET
    ;

if_argument
    :
        LPAR expr RPAR
    ;

number
    : INT_VAL
    |FLOAT_VAL
    ;

pattern_matching
    :
    PATTERN IDENTIFIER LPAR parameter* paramete_with_default_value? RPAR
    (PATTERN_INDENTATION if_argument ASSIGN expr)*
    SEMICOLON
    ;

two_operand_operators
    :
    MULT
    |DIV
    |PLUS
    |MINUS
    ;

postfix_operator
    :
    POSTFIX_MINUS
    |POSTFIX_PLUS
    ;

prefix_operator
    :
    NEG
    ;


value
    :
    BOOLEAN_VAL
    | STRING_VAL
    | number
    ;

logical_operator
    :
    NOT
    | OR
    | AND
    ;

stop_control
    :
    BREAK
    | BREAK IF if_argument
    | NEXT
    | NEXT IF if_argument
    ;

comment
    :    SINGLE_LINE_COMMENT|
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
INT:               'int';
INT_VAL:     [1-9][0-9]* | [0];
FLOAT:           'float';
FLOAT_VAL: [0-9]*'.'[0-9]+ | INT_VAL;
STRING:         'string';
STRING_VAL:    '"'.*?'"';
BOOLEAN:       'boolean';
BOOLEAN_VAL:   TRUE|FALSE;
TRUE:             'true';
FALSE:           'false';
LIST:             'list';
FPTR:           METHOD'(:'IDENTIFIER')';
LPAR:                '(';
RPAR:                ')';
LBRACKET:            '[';
RBRACKET:            ']';
POSTFIX_PLUS:       '++';
POSTFIX_MINUS:      '--';
NOT:                 '!';
NEG:                 '-';
MULT:                '*';
DIV:                 '/';
PLUS:                '+';
MINUS:               '-';
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
INSERT:             '<<';
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
