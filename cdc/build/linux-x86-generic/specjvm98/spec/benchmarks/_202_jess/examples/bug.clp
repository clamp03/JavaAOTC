; -*- clips -*-

(deftemplate foo
  (multislot baz))

(deffacts foo
  (foo (baz X Y Z)))

(defrule foo
  (foo (baz $?bar))
  =>
  (assert (foo (baz (create$ X (rest$ $?bar) A B C))))
  (assert (foz (create$ X (rest$ $?bar) A B C)))
  (printout t (create$ Q $?bar A B C) crlf) 
  )
(watch all)
(reset)
(run)
