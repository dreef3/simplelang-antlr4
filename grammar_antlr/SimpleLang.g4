grammar SimpleLang;

program : input FUNCTIONS declarations output END;

input: INPUT parameters;

parameters: parameter (COMMA parameter)*;

parameter: ID;

declarations: declaration (COMMA declaration)*;

expressions: expression (COMMA expression)*;

declaration: ID LPAREN parameters RPAREN ARROW expression;

output: OUTPUT expressions;

expression: ID | call | NUMBER | MINUS expression | LPAREN expression RPAREN | expression (PLUS | MULT | DIV) expression |
	conditional;

call: ID LPAREN expressions RPAREN;

conditional: IF expression (EQ | LT) expression THEN expression ELSE expression FI;

// Keywords
INPUT : 'INPUT';
FUNCTIONS : 'FUNCTIONS';
OUTPUT: 'OUTPUT';
END: 'END';
IF: 'IF';
THEN: 'THEN';
ELSE: 'ELSE';
FI: 'FI';


ID : LETTER (LETTER | DIGIT)* ;

NUMBER: (DIGIT)+;

LETTER: [a-z];

DIGIT: [0-9];

// As separate tokens to match operations later
PLUS: '+';
MULT: '*';
DIV: '/';
MINUS: '-';

EQ: '=';
LT: '<';

ARROW: '=>';

LPAREN: '(';
RPAREN: ')';
COMMA: ',';

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines

