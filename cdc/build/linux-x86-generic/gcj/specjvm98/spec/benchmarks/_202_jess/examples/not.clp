;; -*- clips-mode -*-
(defrule foo
  (X ~key)
  =>
  (printout t "not key" crlf))

(watch all)
(reset)
(assert (X key) (Y bar))
(run)