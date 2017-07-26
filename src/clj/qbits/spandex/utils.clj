(ns qbits.spandex.utils
  (:require [qbits.spandex.url]
            [clojure.core.async :as async]))

;; backward compat
(def url qbits.spandex.url/encode)

(defn chan->seq
  "Convert a channel to a lazy sequence.

  Will block on after the last element if the channel is not closed."
  [ch]
  (when-let [v (async/<!! ch)]
    (cons v (lazy-seq (chan->seq ch)))))
