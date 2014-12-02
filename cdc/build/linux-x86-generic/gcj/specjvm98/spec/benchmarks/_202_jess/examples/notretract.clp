;; -*- clips -*-

(defrule rule1
  (not (foo bar))
  ?f1 <- (foo baz)
  (not (foo biddle))
  ?f2 <- (foo bam)
  (not (foo bozo))
  ?f3 <- (foo big)
  (not (foo bimbo))
  ?f4 <- (foo bad)
  =>
  (retract ?f4))

(watch facts)
(reset)
(assert (foo bam) (foo baz) (foo big) (foo bad))
(run)