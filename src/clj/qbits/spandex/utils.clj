(ns qbits.spandex.utils
  "Functions that can help with building a decent DSL on top of the
  client")

(defprotocol URLFragment
  (encode [value]))

(defn string-builder
  ([] (StringBuilder.))
  ([^StringBuilder sb x] (.append sb x))
  ([^StringBuilder sb] (.toString sb)))

(declare comma-sep+encoded-xform)

(extend-protocol URLFragment

  clojure.lang.Sequential
  (encode [value]
    ;; multi index fragment
    (transduce comma-sep+encoded-xform string-builder value))

  clojure.lang.Keyword
  (encode [value] (name value))

  Object
  (encode [value] value))

(def comma-sep+encoded-xform
  (comp (remove nil?)
        (map encode)
        (interpose ",")))

(def url
  "Encodes a sequence of fragments into a valid url. Fragments are
  delimited by / and can be either a scalar value or a collection, in
  that case the fragment is itself delimited by commas. nil fragments
  values are ignored/skipped"
  (let [xform (comp (remove nil?)
                    (map encode)
                    (interpose "/"))]
    (fn [parts]
      (transduce xform string-builder parts))))
