grammar Portugol;
options { caseInsensitive=true; }
program: lineEnd* ALGORITMO STRING lineEnd* declarations routine* INICIO block FIMALGORITMO lineEnd* EOF;
declarations: (lineEnd | DOS lineEnd* | VAR lineEnd* | CONST lineEnd* | constantDecl | variableDecl)*;
constantDecl: ID '=' expression lineEnd+;
variableDecl: idList ':' type lineEnd+;
idList: ID (',' ID)*;
type: scalarType | VETOR '[' range (',' range)* ']' DE scalarType;
scalarType: INTEIRO | REAL | CARACTERE | LOGICO;
range: expression '..' expression;
routine: (FUNCAO | PROCEDIMENTO) ID ('(' parameters? ')')? (':' scalarType)? lineEnd* declarations INICIO block (FIMFUNCAO | FIMPROCEDIMENTO) lineEnd*;
parameters: parameterGroup (';' parameterGroup)*;
parameterGroup: VAR? idList ':' scalarType;
block: (lineEnd | statement lineEnd*)*;
statement
 : reference ('<-' | ':=') expression # assignmentStmt
 | (ESCREVA | ESCREVAL) ('(' outputArgs? ')')? # outputStmt
 | LEIA '(' reference (',' reference)* ')' # inputStmt
 | SE expression ENTAO block (SENAO block)? FIMSE # ifStmt
 | ENQUANTO expression FACA block FIMENQUANTO # whileStmt
 | REPITA block ((ATE | ATE_QUE) expression lineEnd* FIMREPITA? | FIMREPITA) # repeatStmt
 | PARA ID DE expression ATE expression (PASSO expression)? FACA block FIMPARA # forStmt
 | ESCOLHA expression FACA? lineEnd* caseClause* (OUTROCASO block)? FIMESCOLHA # choiceStmt
 | RETORNE expression # returnStmt
 | INTERROMPA # breakStmt
 | LIMPatela # clearStmt
 | PAUSA # pauseStmt
 | DOS # dosStmt
 | ECO (ON | OFF) # echoStmt
 | ALEATORIO (ON | OFF | expression ',' expression (',' expression)?) # randomStmt
 | ID ('(' exprList? ')')? # callStmt
 ;
caseClause: CASO caseValue (',' caseValue)* lineEnd* block;
caseValue: expression (ATE expression)?;
outputArgs: outputArg (',' outputArg)*;
outputArg: expression (':' expression (':' expression)?)?;
exprList: expression (',' expression)*;
reference: ID ('[' exprList ']')?;
expression: logicalOr;
logicalOr: logicalAnd ((OU | XOU) logicalAnd)*;
logicalAnd: comparison (E comparison)*;
comparison: additive (('=' | '<>' | '!=' | '<' | '>' | '<=' | '>=') additive)*;
additive: multiplicative (('+' | '-') multiplicative)*;
multiplicative: power (('*' | '/' | '\\' | '%' | MOD | DIV) power)*;
power: unary ('^' power)?;
unary: ('+' | '-' | NAO) unary | primary;
primary: NUMBER | STRING | VERDADEIRO | FALSO | ID '(' exprList? ')' | reference | '(' expression ')';
lineEnd: NL | ';';
ALGORITMO:'algoritmo'; FIMALGORITMO:'fimalgoritmo'; VAR:'var'; CONST:'const'; DOS:'dos'; INICIO:'inicio';
FUNCAO:'funcao'; PROCEDIMENTO:'procedimento'; FIMFUNCAO:'fimfuncao'; FIMPROCEDIMENTO:'fimprocedimento';
INTEIRO:'inteiro'; REAL:'real'; CARACTERE:'caractere'|'caracter'|'literal'; LOGICO:'logico'; VETOR:'vetor';
ESCREVA:'escreva'; ESCREVAL:'escreval'; LEIA:'leia'; SE:'se'; ENTAO:'entao'; SENAO:'senao'; FIMSE:'fimse';
ENQUANTO:'enquanto'; FACA:'faca'; FIMENQUANTO:'fimenquanto'; REPITA:'repita'; ATE:'ate'; ATE_QUE:'ate_que'; FIMREPITA:'fimrepita';
PARA:'para'; DE:'de'; PASSO:'passo'; FIMPARA:'fimpara'; ESCOLHA:'escolha'; CASO:'caso'; OUTROCASO:'outrocaso'; FIMESCOLHA:'fimescolha';
RETORNE:'retorne'; INTERROMPA:'interrompa'; LIMPatela:'limpatela'; PAUSA:'pausa'; ECO:'eco'; ALEATORIO:'aleatorio'; ON:'on'; OFF:'off';
OU:'ou'; XOU:'xou'; E:'e'; NAO:'nao'; MOD:'mod'; DIV:'div'; VERDADEIRO:'verdadeiro'; FALSO:'falso';
NUMBER:[0-9]+ ('.' [0-9]+)?;
STRING:'"' ('""'|~["\r\n])* '"' | '\'' (~['\r\n])* '\'';
ID:[a-z_] [a-z_0-9]*;
NL: '\r'? '\n';
COMMENT:'//' ~[\r\n]* -> skip;
WS:[ \t\r\uFEFF]+ -> skip;
