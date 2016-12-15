(ns qbits.spandex.options
  (:import
   (org.apache.http HttpHost)
   (org.elasticsearch.client
    RestClient RestClientBuilder)))

(defmulti set-option! (fn [k builder option] k))

(defmethod set-option! :failure-listener
  [_ builder listener]
  builder)

(defmethod set-option! :default
  [_ builder x]
  builder)

(defn set-options!
  [builder options]
  (reduce (fn [builder [k option]]
            (set-option! k builder option))
          builder
          options))

(defn builder [hosts options]
  (let [b (RestClient/builder (into-array HttpHost
                                          (map #(HttpHost/create %) hosts)))]
    (set-options! b options)
    (.build b)))
