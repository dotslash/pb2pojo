grammar Proto;

proto:
    (messages OPT_WS_MUST_NL+)+
    WS?
    EOF;

messages: 'message' WS name Open_Brace OPT_WS_MUST_NL+
                        (field_declaration OPT_WS_MUST_NL*)+
                      Close_Brace;
field_declaration: (field_rule WS)? type WS name Equals number ';';


Open_Brace : WS? '{' WS?;
Close_Brace : WS? '}' WS?;
Equals : WS? '=' WS?;

name: WS? Alphabet (Digit| Alphabet | '_')* WS?;
number: WS? Digit+ WS?;


Digit : ('0'..'9');
Alphabet: ('a'..'z' | 'A'..'Z');


type: WS? Type_Main WS?;
Type_Main : 'int32'| 'int64' |'double' |
            'float' |'bool' | 'bytes' | 'string';

field_rule: WS? Field_rule_main WS?;
Field_rule_main: ('optional' | 'required' | 'repeated');

OPT_WS_MUST_NL: WS? NL;
WS: (' ' | '\t')+;
NL:  '\r'? '\n';
