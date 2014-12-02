(assert (foo))
(assert (bar))
(assert (baz))

(save-facts "foofile")
(reset)
(load-facts "foofile")
(facts)
