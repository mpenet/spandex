(ns qbits.spandex.client-options
  (:import
   (org.apache.http
    HttpHost)
   (org.apache.http.message
    BasicHeader)
   (org.elasticsearch.client
    RestClient
    RestClientBuilder
    RestClientBuilder$RequestConfigCallback
    RestClientBuilder$HttpClientConfigCallback)
   (org.elasticsearch.client.sniff
    SniffOnFailureListener)))

(defn ^:no-doc request-config-callback [f]
  (reify RestClientBuilder$RequestConfigCallback
    (customizeRequestConfig [this builder]
      (f builder))))

(defn ^:no-doc request-http-client-config-callback [f]
  (reify RestClientBuilder$HttpClientConfigCallback
    (customizeHttpClient [this builder]
      (f builder))))

(defmulti ^:no-doc set-option! (fn [k builder option] k))

(defmethod set-option! :max-retry-timeout
  [_ ^RestClientBuilder builder timeout-ms]
  (-> builder (.setMaxRetryTimeoutMillis (int timeout-ms))))

(defmethod set-option! :default-headers
  [_ ^RestClientBuilder builder headers]
  (-> builder (.setDefaultHeaders (into-array BasicHeader
                                              (map (fn [[k v]]
                                                     (BasicHeader. (name k) v)))))))

(defmethod set-option! :http-client-config-callback
  [_ ^RestClientBuilder builder f]
  (-> builder (.setHttpClientConfigCallback (request-http-client-config-callback f))))

(defmethod set-option! :request-config-callback
  [_ ^RestClientBuilder builder f]
  (-> builder (.setRequestConfigCallback (request-config-callback f))))

(defmethod set-option! :sniff-on-failure
  [_ ^RestClientBuilder builder sniffer]
  (let [listener (SniffOnFailureListener.)]
    (set-option! :failure-listener builder listener)
    (.setSniffer listener sniffer))
  builder)

(defmethod set-option! :failure-listener
  [_ ^RestClientBuilder builder failure-listener]
  (-> builder (.setFailureListener failure-listener)))

(defmethod set-option! :default
  [_ ^RestClientBuilder b x]
  b)

(defn ^:no-doc set-options!
  [^RestClientBuilder builder options]
  (reduce (fn [builder [k option]]
            (set-option! k builder option))
          builder
          options))

(defn builder [{:as options
                :keys [hosts]}]
  (let [b (RestClient/builder (into-array HttpHost
                                          (map #(HttpHost/create %) hosts)))]
    (set-options! b options)
    (.build b)))
