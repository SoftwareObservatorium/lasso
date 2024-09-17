/** Author: Marcus Kessel (kessel@informatik.uni-mannheim.de) */
grammar LQL;

/** Allow zero or one system and zero or more filters */
parse
 : interfaceSpec? filter* EOF
 ;

/** System has zero or more method signatures */
interfaceSpec
 : (simpletype | qualifiedtype) '{' methodSig* '}'
 ;

/** Method signature */
methodSig
 : NAME ( '(' inputs? ')->' outputs? | '(' inputs? ')' )
 ;

/** Argument types (input or output) */
parameters
 : (simpletype | qualifiedtype | arraytype | namedparam | typeparam) ( ',' (simpletype | qualifiedtype | arraytype | namedparam | typeparam) )*
 ;

/** alias */
inputs
 : parameters
 ;

/** alias */
outputs
 : parameters
 ;

/** a fully qualified type like aaa.bbb.ccc */
qualifiedtype
 : NAME ( '.' NAME )*
 ;

/** a simple type without "." */
simpletype
 : NAME
 ;

/** an array type */
arraytype
 : (simpletype | qualifiedtype) ('[]')*
 ;

/** a named parameter */
namedparam
 : NAME '=' (simpletype | qualifiedtype | arraytype)
 ;

/** generics */
typeparam
  : (simpletype | qualifiedtype) '<' (simpletype | qualifiedtype | typeparam) (',' (simpletype | qualifiedtype | typeparam))* '>'
  ;

/** simply filter */
filter
 : FILTERVALUE
 ;

/** removed <> */
NAME         : [$a-zA-Z_] [$a-zA-Z0-9_]*;
FILTERVALUE  : [a-zA-Z_\\*!] [a-zA-Z0-9_:'"\\*'^']*;
TEXT         : '\'' ~[\r\n']* '\'';
SPACE        : [ \t\r\n]+ -> skip;