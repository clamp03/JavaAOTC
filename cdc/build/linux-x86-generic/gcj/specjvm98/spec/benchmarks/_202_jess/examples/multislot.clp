;; -*- clips -*-

(deftemplate foo        
  (multislot Y)
  (multislot Z))


(watch facts)
(watch activations)
(watch compilations)

(defrule fiz
  (foo (Y $?A))
  (foo (Z $?A))
  =>
  (printout t "HA!" crlf))

(reset)


(assert (foo (Y A B C) (Z C)))
(assert (foo (Y D) (Z A B C)))


(run)
