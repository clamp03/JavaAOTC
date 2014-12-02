
(printout t (integerp 1.1) crlf)
(printout t (integerp 1.0) crlf)
(printout t (floatp 1.0) crlf)
(printout t (floatp 1.1) crlf)
(printout t (lexemep foo) crlf)
(printout t (symbolp "foo") crlf)
(printout t (multifieldp 1) crlf)

(reset)
(assert (foo 1 2 3))
(defrule foo
        (foo $?bar)
=>
(printout t (multifieldp $?bar) crlf)
(printout t (first$ $?bar) crlf)
(printout t (rest$ $?bar) crlf)

)

(run)
