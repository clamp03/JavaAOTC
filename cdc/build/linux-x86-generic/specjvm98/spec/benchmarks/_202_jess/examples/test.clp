(deffacts a
        (foo bar)
        (bar foo))

(deffacts b
        (bar bar)
        (foo foo))


(defrule init
   ?o <- (initial-fact)
   =>
   (retract ?o)
   (printout t "Of the numbers 1-5:" crlf)
   (assert (data 1) (data 2) (data 3) (data 4) (data 5)))


(defrule ex9a
   (data ?x)
   (data ?y)
   =>
   (assert (data (+ ?x ?y)))
   (printout t ?x " and " ?y " instantiate this rule: existence only" crlf))

(defrule ex9b
   (declare (salience 10))
   (data ?x&:(> ?x 10000))
   =>
   (printout t "Execution halted at " ?x " with the HALT command!" crlf)
   (halt))


(watch facts)
(reset) 
(run)  

