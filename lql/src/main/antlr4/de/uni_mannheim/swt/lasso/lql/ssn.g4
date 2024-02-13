/** Author: Marcus Kessel (kessel@informatik.uni-mannheim.de) */
grammar ssn;
ssn              : methodinvocation+ BODYOUTPUT* EOF;
methodinvocation : output (', ' output)* methodname input (', ' input)*;
output           : actualparam;
methodname       : NAME | INITIALISER;
input            : actualparam;
actualparam      : COORDINATE | VALUE | BODYINPUT;

NAME             : [a-zA-Z_] [a-zA-Z0-9_]*;
INITIALISER      : 'create';
COORDINATE       : [A-Z]+ [0-9]+;
VALUE            : [a-zA-Z_\\*] [a-zA-Z0-9_:'"\\*]*;
BODYINPUT        : '?' NAME;
BODYOUTPUT       : '!' COORDINATE;