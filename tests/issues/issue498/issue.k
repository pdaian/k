module ISSUE
  syntax Pgm ::= "program"
  configuration <foo>
                  <bar> $PGM:Pgm </bar>
                </foo>
  configuration <foo>
                  <bar> .K </bar>
                </foo>
                <blob> $PGM:Pgm </blob>
  rule [do-blob]:
    (<blob> program </blob> => .)
    <bar> . => program </bar>
endmodule


