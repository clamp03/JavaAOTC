; -*- clips -*-

(defglobal
  ?*x* = 3
  )

(defglobal
  ?*y* = 3
  ?*z* = (+ 4 15)
  )

(deffacts foo "help"
  (bar baz bim boo)
  (foo bar 3 "hi")
  (foo (+ 3 (- (/ 20 5) (* 1.5 2)))))

(watch facts)
(reset)
(assert (foo (+ 10 10)))

( printout t ?*x* crlf)

(deftemplate baz1 "help"
  (slot foo (default 10))
  (slot bar (default hi))
  (slot bim))

(deftemplate baz2
  (slot foo)
  (slot bar)
  (slot bim (default x)))


(assert (baz1 (bim x y z)))
(assert (baz2 (bim y)))
