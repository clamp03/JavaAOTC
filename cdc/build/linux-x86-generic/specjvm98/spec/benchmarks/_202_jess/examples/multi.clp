;; -*- clips -*-

(deftemplate foo
  (multislot X))

(defrule test-multi
  (foo (X $?stuff 6 $?more))
  (foo (X $?more))
  =>
  (printout t "stuff is " $?stuff " and more is " $?more crlf)
  )

(reset)
(assert (foo (X 1 2 3 4 5 6)))
(assert (foo (X 6 1 2 3 4 5)))
(assert (foo (X 1 2 3 4 5)))
(run)