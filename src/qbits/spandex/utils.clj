(ns qbits.spandex.utils
  (:require [qbits.spandex.url]
            [clojure.core.async :as async]
            [clojure.string :as string]))

;; backward compat
(def url qbits.spandex.url/encode)

(defn chan->seq
  "Convert a channel to a lazy sequence.

  Will block on after the last element if the channel is not closed."
  [ch]
  (when-let [v (async/<!! ch)]
    (cons v (lazy-seq (chan->seq ch)))))

(defn escape-query-string
  "Escape or remove special characters in query string coming from users.

  See:
  https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters"
  [query]
  (if (= query "*")
    query
    (-> query
        ;; escape the reserved characters
        (string/replace #"[\+\-\!\(\)\{\}\[\]^\"~\*\?\\:/]|&&|\|\|" #(str "\\" %1))
        ;; according to the doc we can't even escape those
        (string/replace #"[<>]" ""))))
