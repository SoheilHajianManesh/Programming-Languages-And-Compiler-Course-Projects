grammar FunctionCraft;

// Lexer rules
// The lexer rules define patterns for recognizing tokens like numbers, booleans, strings,
// comments, keywords, identifiers, and operators in the input text. These rules are used
// by the lexer to break the input into a token stream.

MAIN:             'main';
PRINT:           'print';
DEF:               'def';
END:               'end';
RETURN:         'return';
IF:                 'if';
ELSE:             'else';
ELSEIF:         'elseif';
TRUE:             'true';
FALSE:           'false';
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
INT_VAL:     [1-9][0-9]*;
FLOAT:           'float';
FLOAT_VAL: [0-9]*'.'[0-9]+ | INT_VAL;
STRING:         'string';
STRING_VAL:    '"'.*?'"';
BOOLEAN:       'boolean';
BOOLEAN_VAL:   TRUE|FALSE;
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
NEQ:               '!=';
AND:                '&&';
OR:                 '||';
ASSIGN:              '=';
INSERT:             '<<';
LBRACE:              '{';
RBRACE:              '}';
COMMA:               ',';
DOT:                 '.';
COLON:               ':';
SEMICOLON:           ';';
IDENTIFIER:    [a-z][a-zA-Z0-9_]*;
SINGLE_LINE_COMMENT: '#' ~[\r\n]* -> skip;
MULTI_LINE_COMMENT: '=begin'.*?'=end' -> skip;
WS:                 [ \t\r\n]+ -> skip;

// Parser rules
// The parser rules start with the program rule, which defines the overall structure of a
// valid program. They then specify how tokens can be combined to form declarations, control
// structures, expressions, assignments, function calls, and other constructs within a program.
// The parser rules collectively define the syntax of the language.

// TODO