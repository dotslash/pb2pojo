grammar Proto;

proto:
    (message)+
    EOF;

message: 'message' IDENTIFIER OPEN_BRACE (field_declaration)+ CLOSE_BRACE;
field_declaration: (FIELD_RULE)? IDENTIFIER IDENTIFIER EQUALS NUMBER ';';

OPEN_BRACE : '{';
CLOSE_BRACE : '}';
EQUALS : '=';
//TYPE: ('int32'| 'int64' |'double' |
//       'float' |'bool' | 'bytes' | 'string');
FIELD_RULE: ('optional' | 'required' | 'repeated');

NUMBER: [0-9]+;
IDENTIFIER : [a-zA-Z][a-zA-Z0-9_]+;

// Skip all whitespaces.
WS  : [ \t\r\n]+ -> skip ;
