/*
Expected:
'ListWrap(1,,1,,1)
*/
module LABEL-AND-CONTEXT
  configuration <k> $PGM:K </k>

  syntax KLabel  ::= "'ListWrap" | "'A"
  syntax KResult ::= Int
  context 'ListWrap(_,,HOLE,,_)

  rule 'A(_) => 1
endmodule
