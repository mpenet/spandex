(ns qbits.spandex.sniffer-options
  (:import
   (org.elasticsearch.client.sniff
    SnifferBuilder
    Sniffer
    ElasticsearchHostsSniffer
    ElasticsearchHostsSniffer$Scheme)))

(defmulti set-option! (fn [k builder option] k))

(defmethod set-option! :sniff-interval
  [_ ^SnifferBuilder builder interval]
  (-> builder (.setSniffIntervalMillis (int interval))))

(defmethod set-option! :sniff-interval
  [_ ^SnifferBuilder builder interval]
  (-> builder (.setSniffIntervalMillis (int interval))))

(defmethod set-option! :default
  [_ ^SnifferBuilder b x]
  b)

(defn ^SnifferBuilder set-options!
  [^SnifferBuilder builder options]
  (reduce (fn [builder [k option]]
            (set-option! k builder option))
          builder
          options))

(defn builder [client sniffer options]
  (let [b (Sniffer/builder client)]
    (-> b
        (.setHostsSniffer sniffer)
        (set-options! options)
        .build)))
