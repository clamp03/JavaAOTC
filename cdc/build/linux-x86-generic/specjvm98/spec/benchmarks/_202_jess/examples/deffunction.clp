;; ------------------------------------------------------------
;; Deffunctions
;; ------------------------------------------------------------

(deffunction test (?A)
  "returning without return!"
  1
  2
  3
  ?A
  )

(printout t (test 123) crlf)

