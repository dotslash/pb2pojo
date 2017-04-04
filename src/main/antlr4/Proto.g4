grammar Proto;

proto:
    (messages)+
    EOF;

messages: 'message' name Open_Brace (field_declaration)+ Close_Brace;
field_declaration: (field_rule)? type name Equals number ';';


name: Alphabet (Digit| Alphabet | '_')*;
number: Digit+ ;


Open_Brace : '{';
Close_Brace : '}';
Equals : '=';
Digit : ('0'..'9');
Alphabet: ('a'..'z' | 'A'..'Z');


type: ('int32'| 'int64' |'double' |
       'float' |'bool' | 'bytes' | 'string');

field_rule: ('optional' | 'required' | 'repeated');

// Skip all whitespaces.
WS  : [ \t\r\n]+ -> skip ;