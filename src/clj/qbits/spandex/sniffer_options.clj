(ns qbits.spandex.sniffer-options)
;;   (:import
;;    (org.elasticsearch.client.sniff
;;     SnifferBuilder
;;     Sniffer)))

;; (defmulti ^:no-doc set-option! (fn [k builder option] k))

;; (defmethod set-option! :sniff-interval
;;   [_ ^SnifferBuilder builder interval]
;;   (-> builder (.setSniffIntervalMillis (int interval))))

;; (defmethod set-option! :sniff-after-failure-delay
;;   [_ ^SnifferBuilder builder delay]
;;   (-> builder (.setSniffAfterFailureDelayMillis (int delay))))

;; (defmethod set-option! :default
;;   [_ ^SnifferBuilder b x]
;;   b)

;; (defn ^:no-doc set-options! ^SnifferBuilder
;;   [^SnifferBuilder builder options]
;;   (reduce (fn [builder [k option]]
;;             (set-option! k builder option))
;;           builder
;;           options))

;; (defn builder [client sniffer options]
;;   (-> (Sniffer/builder client)
;;       (.setHostsSniffer sniffer)
;;       (set-options! options)
;;       .build))
