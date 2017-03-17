(ns macchiato.test.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [macchiato.test.auth.core-test]))

(doo-tests 'macchiato.test.auth.core-test)
