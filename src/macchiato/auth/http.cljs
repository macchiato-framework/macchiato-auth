(ns macchiato.auth.http
  (:require
    [clojure.string :as s]))

(defn find-header
  [headers header-name]
  (->> headers
       (filter #(= (s/lower-case header-name) (s/lower-case (key %))))
       (first)
       (second)))
