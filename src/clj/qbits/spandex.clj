(ns qbits.spandex
  (:require
   [qbits.spandex.options :as options])
  (:import
   (org.elasticsearch.client HttpAsyncResponseConsumerFactory)
   (org.elasticsearch.client ResponseListener)
   (org.elasticsearch.client RestClient)
   (org.apache.http Header)
   (org.apache.http.message BasicHeader)
   (org.apache.http.entity
    StringEntity
    InputStreamEntity)))

(defn client
  ([hosts]
   (client hosts {}))
  ([hosts options]
  (options/builder hosts options)))

(defprotocol BodyEncodable
  (encode-body [x]))

(extend-protocol BodyEncodable
  String
  (encode-body [x]
    (StringEntity. x))
  java.io.InputStream
  (encode-body [x]
    (InputStreamEntity. x)))

(defn encode-headers
  [headers]
  (into-array Header
              (map (fn [[k v]]
                     (BasicHeader. (name k) v))
                   headers)))

(defn encode-query-string [qs]
  (reduce-kv (fn [m k v] (assoc m (name k) v))
             {}
             qs))

(defn response-listener [success error]
  (reify ResponseListener
    (onSuccess [this response]
      (when success (success response)))
    (onFailure [this ex]
      (when error (error ex)))))

(defn request [^RestClient client
               {:keys [method url headers query-string body]
                :as request-params}]
  (.performRequest client
                   (name method)
                   url
                   (encode-query-string query-string)
                   (encode-body body)
                   (encode-headers headers)))

(defn request-async [^RestClient client
                     {:keys [method url headers query-string body
                             success error
                             response-consumer-factory]
                      :as request-params}
                     handler]
  (.performRequestAsync client
                        (name method)
                        url
                        (encode-query-string query-string)
                        (encode-body body)
                        (or response-consumer-factory
                            HttpAsyncResponseConsumerFactory/DEFAULT)
                        (response-listener success error)
                        (encode-headers headers)))
