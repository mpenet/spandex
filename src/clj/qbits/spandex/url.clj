(ns qbits.spandex.url)

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
    (let [interposed (->> value
                          (remove nil?)
                          (map encode-fragment)
                          (interpose ","))]
      (string-builder (reduce string-builder (string-builder) interposed))))

  clojure.lang.Keyword
  (encode-fragment [value] (name value))

  String
  (encode-fragment [s] s)

  Object
  (encode-fragment [value] (str value)))


(extend-protocol URL
  String
  (encode [s] s)

  Object
  (encode [parts]
    (let [interposed (->> parts
                          (remove nil?)
                          (map encode-fragment)
                          (interpose "/"))]
      (string-builder (reduce url-string-builder (url-string-builder) interposed))))

  nil
  (encode [_] "/"))
