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
  ; FIXME: if the lazy-seq isn't fully read, the underlying async is never closed!
  (->> (repeatedly #(async/<!! ch))
       (take-while some?)))

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
