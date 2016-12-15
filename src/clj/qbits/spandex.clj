(ns qbits.spandex
  (:require
   [clojure.core.async :as async]
   [qbits.spandex.client-options :as client-options]
   [qbits.spandex.sniffer-options :as sniffer-options]
   [qbits.commons.enum :as enum]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import
   (org.elasticsearch.client.sniff
    Sniffer
    ElasticsearchHostsSniffer
    ElasticsearchHostsSniffer$Scheme)
   (org.elasticsearch.client
    RestClient
    Response
    HttpAsyncResponseConsumerFactory
    ResponseListener)
   (org.apache.http
    Header
    HttpEntity)
   (org.apache.http.message
    BasicHeader)
   (org.apache.http.entity
    StringEntity
    InputStreamEntity)))

(defn client
  ([hosts]
   (client hosts {}))
  ([hosts options]
   (client-options/builder hosts options)))

(def sniffer-scheme (enum/enum->fn ElasticsearchHostsSniffer$Scheme))

(defn sniffer
  ([client]
   (sniffer client nil))
  ([client {:as options
            :keys [scheme timeout]
            :or {scheme :http
                 timeout ElasticsearchHostsSniffer/DEFAULT_SNIFF_REQUEST_TIMEOUT}}]
   (let [sniffer (ElasticsearchHostsSniffer.
                  client
                  timeout
                  (sniffer-scheme scheme))]
     (sniffer-options/builder client sniffer options))))

(defprotocol BodyEncoder
  (encode-body [x]))

(extend-protocol BodyEncoder
  java.io.InputStream
  (encode-body [x]
    (InputStreamEntity. (json/generate-stream (io/writer x))))

  Object
  (encode-body [x]
    (StringEntity. (json/generate-string x)))

  nil
  (encode-body [x] nil))

(defprotocol BodyDecoder
  (decode-body [x] x))

(defn encode-headers
  [headers]
  (into-array Header
              (map (fn [[k v]]
                     (BasicHeader. (name k) v))
                   headers)))

(defn response-headers
  [^Response response]
  (->> response
      .getHeaders
      (reduce (fn [m ^Header h]
                (assoc! m
                        (.getName h)
                        (.getValue h)))
              (transient {}))
      persistent!))

(defn encode-query-string
  [qs]
  (reduce-kv (fn [m k v] (assoc m (name k) v))
             {}
             qs))

(defn json-entity?
  [^HttpEntity entity]
  (-> entity
      .getContentType
      .getValue
      (str/index-of "application/json")
      (> -1)))

(defn response-status
  [^Response response]
  (some-> response .getStatusLine .getStatusCode))

(defn response-decoder
  [^Response response]
  (let [entity  (.getEntity response)
        content (.getContent entity)]
    {:body (if (json-entity? entity)
             (-> content io/reader (json/parse-stream true))
             (slurp content))
     :status (response-status response)
     :headers (response-headers response)
     :host (.getHost response)}))

(defn response-listener [success error]
  (reify ResponseListener
    (onSuccess [this response]
      (when success (success (response-decoder response))))
    (onFailure [this ex]
      (when error (error ex)))))

(defn request
  [^RestClient client {:keys [method url headers query-string body]
                       :or {method :get}
                       :as request-params}]
  (-> client
      (.performRequest
       (name method)
       url
       (encode-query-string query-string)
       (encode-body body)
       (encode-headers headers))
      response-decoder))

(defn request-async
  [^RestClient client
   {:keys [method url headers query-string body
           success error
           response-consumer-factory]
    :or {method :get}
    :as request-params}]
  ;; eeek we can prolly avoid duplication here
  (if response-consumer-factory
    (.performRequestAsync client
                          (name method)
                          url
                          (encode-query-string query-string)
                          (encode-body body)
                          response-consumer-factory
                          (response-listener success error)
                          (encode-headers headers))
    (.performRequestAsync client
                          (name method)
                          url
                          (encode-query-string query-string)
                          (encode-body body)
                          (response-listener success error)
                          (encode-headers headers))))

(defn request-ch
  [^RestClient client options]
  (let [ch (async/promise-chan)]
    (try
      (request-async client
                     (assoc options
                            :success (fn [response] (async/put! ch response))
                            :error (fn [ex] (async/put! ch ex))))
      (catch Throwable t
        (async/put! ch t)))
    ch))

;; (def x (client ["http://localhost:9200"]))
;; (def s (sniffer x))
;; (request x {:url "/entries/entry" :method :post :body {:foo "bar"}} )
;; (async/<!! (request-ch x {:url "/entries/entry" :method :post :body {:foo "bar"}} ))
