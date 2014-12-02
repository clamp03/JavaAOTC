; -*- clips -*-

(deftemplate foo
  (multislot baz))

(deffacts foo
  (foz X Y Z))

(defrule foo
  (foz $?bar)
  =>
  (assert (foo (baz (create$ X (rest$ $?bar) A B C))))
  (printout t (create$ Q $?bar A B C) crlf) 
  )
(watch all)
(reset)
(run)