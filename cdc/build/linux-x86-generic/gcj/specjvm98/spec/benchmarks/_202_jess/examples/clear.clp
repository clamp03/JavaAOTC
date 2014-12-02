(watch all)
(defrule foo
        =>
        (printout t "and a one" crlf))

(reset)
(run)

(printout t "CLEARING" crlf);
(clear)
(watch all)
(printout t "REDEFINING" crlf);
(defrule foo
        =>
        (printout t "and a two" crlf))

(reset)
(run)