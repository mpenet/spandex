(ns qbits.spandex.url
  (:require
   [ring.util.codec :as codec]))

(declare x-fragment-interpose-comma
         x-fragment-interpose-slash)

(defprotocol URL
  (encode [x]))

(defprotocol URLFragment
  (encode-fragment [value]))

(defn ^:no-doc string-builder
  ([] (StringBuilder.))
  ([^StringBuilder sb x] (.append sb x))
  ([^StringBuilder sb] (.toString sb)))

(defn ^:no-doc url-string-builder
  ([] (StringBuilder. "/"))
  ([^StringBuilder sb x] (.append sb x))
  ([^StringBuilder sb] (.toString sb)))

(extend-protocol URLFragment
  clojure.lang.Sequential
  (encode-fragment [value]
    ;; multi index fragment
    (transduce x-fragment-interpose-comma string-builder value))

  clojure.lang.Keyword
  (encode-fragment [value]
    (encode-fragment (name value)))

  String
  (encode-fragment [s]
    (codec/url-encode s))

  Object
  (encode-fragment [value]
    (encode-fragment (str value))))

(def ^:no-doc x-fragment-interpose-comma
  (comp (remove nil?)
        (map encode-fragment)
        (interpose ",")))

(def ^:no-doc x-fragment-interpose-slash
  (comp (remove nil?)
        (map encode-fragment)
        (interpose "/")))

(extend-protocol URL
  String
  (encode [s] s)

  Object
  (encode [parts]
    (transduce x-fragment-interpose-slash
               url-string-builder
               parts))

  nil
  (encode [_] "/"))
