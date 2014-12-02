;; -*- clips -*-

(deftemplate foo        
  (multislot Y)
  (multislot Z))


(watch facts)
(watch activations)
(watch compilations)

(defrule fiz
  (foo (Z $?A&:(eq $?A (create$ A B C))))
  =>
  (printout t "HA!" crlf))

(reset)


(assert (foo (Y Q)))


(run)
