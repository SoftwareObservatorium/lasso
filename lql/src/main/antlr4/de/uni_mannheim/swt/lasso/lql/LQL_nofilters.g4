/** Author: Marcus Kessel (kessel@informatik.uni-mannheim.de) */
grammar LQL_nofilters;
/** Allow zero or one system and zero or more filters */
notation : interfaceSpec? EOF;
/** Interface has zero or more method signatures */
interfaceSpec : NAME '{' methodSig* '}';
/** Method signature */
methodSig : NAME ( '(' inputs? ')->' outputs? | '(' inputs? ')' );
/** Parameters (input or output) */
parameters : (simpletype | qualifiedtype | arraytype | namedparam) ( ',' (simpletype | qualifiedtype | arraytype | namedparam) )*;
/** alias */
inputs : parameters;
/** alias */
outputs : parameters;
/** a fully qualified type like aaa.bbb.ccc */
qualifiedtype : NAME ( '.' NAME )*;
/** a simple type without "." */
simpletype : NAME;
/** an array type */
arraytype : (simpletype | qualifiedtype) ('[]')*;
/** a named parameter */
namedparam : NAME ':' (simpletype | qualifiedtype | arraytype);
NAME         : [a-zA-Z_] [a-zA-Z0-9_]*;