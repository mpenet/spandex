(ns qbits.spandex.sniffer-options
  (:import
   (org.elasticsearch.client.sniff
    SnifferBuilder
    Sniffer)))

(defmulti set-option! (fn [k builder option] k))

(defmethod set-option! :sniff-interval
  [_ ^SnifferBuilder builder interval]
  (-> builder (.setSniffIntervalMillis (int interval))))

(defmethod set-option! :sniff-after-failure-interval
  [_ ^SnifferBuilder builder interval]
  (-> builder (.setSniffAfterFailureDelayMillis (int interval))))

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
  (-> (Sniffer/builder client)
      (.setHostsSniffer sniffer)
      (set-options! options)
      .build))
