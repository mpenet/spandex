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
    ElasticsearchHostsSniffer$Scheme
    SniffOnFailureListener)
   (org.elasticsearch.client
    RestClient
    ResponseListener)
   (org.apache.http
    Header
    HttpEntity)
   (org.apache.http.message
    BasicHeader)
   (org.apache.http.nio.entity
    NStringEntity)
   (org.apache.http.entity
    InputStreamEntity)
   (java.nio.charset
    StandardCharsets)))

(defn client
  "Returns a client instance to be used to perform requests"
  ([]
   (client {:hosts ["http://localhost:9200"]}))
  ([options]
   (client-options/builder options)))

(def ^:no-doc sniffer-scheme (enum/enum->fn ElasticsearchHostsSniffer$Scheme))

(defn sniffer
  "Takes a Client instance (and possible sniffer options) and returns
  a sniffer instance that will initially be bound to passed client."
  ([client]
   (sniffer client nil))
  ([client {:as options
            :keys [scheme timeout]
            :or {scheme :http
                 timeout ElasticsearchHostsSniffer/DEFAULT_SNIFF_REQUEST_TIMEOUT}}]
   (let [sniffer (ElasticsearchHostsSniffer. client
                                             timeout
                                             (sniffer-scheme scheme))]
     (sniffer-options/builder client sniffer options))))

(defn set-sniff-on-failure!
  "Register a SniffOnFailureListener that allows to perform sniffing
  on failure."
  [^Sniffer sniffer]
  (doto (SniffOnFailureListener.)
    (.setSniffer sniffer)))

(defprotocol Closable
  (close! [this]))

(extend-protocol Closable
  Sniffer
  (close! [sniffer] (.close sniffer))
  RestClient
  (close! [client] (.close client)))

(defrecord Raw [value])
(defn raw
  "Marks body as raw, which allows to skip JSON encoding"
  [body]
  (Raw. body))

(defprotocol BodyEncoder
  (encode-body [x]))

(extend-protocol BodyEncoder
  java.io.InputStream
  (encode-body [x]
    (InputStreamEntity. x))

  Raw
  (encode-body [x]
    (NStringEntity. ^String (:value x)
                    StandardCharsets/UTF_8))

  Object
  (encode-body [x]
    (NStringEntity. (json/generate-string x)
                    StandardCharsets/UTF_8))

  nil
  (encode-body [x] nil))

(defn ^:no-doc encode-headers
  [headers]
  (into-array Header
              (map (fn [[k v]]
                     (BasicHeader. (name k) v))
                   headers)))

(defn ^:no-doc response-headers
  [^org.elasticsearch.client.Response response]
  (->> response
       .getHeaders
       (reduce (fn [m ^Header h]
                 (assoc! m
                         (.getName h)
                         (.getValue h)))
               (transient {}))
       persistent!))

(defn ^:no-doc encode-query-string
  [qs]
  (reduce-kv (fn [m k v] (assoc m (name k) v))
             {}
             qs))

(defn ^:no-doc json-entity?
  [^HttpEntity entity]
  (-> entity
      .getContentType
      .getValue
      (str/index-of "application/json")
      (> -1)))

(defn ^:no-doc response-status
  [^org.elasticsearch.client.Response response]
  (some-> response .getStatusLine .getStatusCode))

(defrecord Response [body status headers hosts])

(defn ^:no-doc response-decoder
  [^org.elasticsearch.client.Response response keywordize?]
  (let [entity  (.getEntity response)
        content (.getContent entity)]
    (Response. (if (json-entity? entity)
                 (-> content io/reader (json/parse-stream keywordize?))
                 (slurp content))
               (response-status response)
               (response-headers response)
               (.getHost response))))

(defn ^:no-doc response-listener
  [success error keywordize?]
  (reify ResponseListener
    (onSuccess [this response]
      (when success
        (success (response-decoder response keywordize?))))
    (onFailure [this ex]
      (when error
        (error ex)))))

(defn request
  [^RestClient client {:keys [method url headers query-string body
                              keywordize?]
                       :or {method :get
                            keywordize? true}
                       :as request-params}]
  (-> client
      (.performRequest
       (name method)
       url
       (encode-query-string query-string)
       (encode-body body)
       (encode-headers headers))
      (response-decoder keywordize?)))

(defn request-async
  [^RestClient client
   {:keys [method url headers query-string body
           success error keywordize?
           response-consumer-factory]
    :or {method :get
         keywordize? true}
    :as request-params}]
  ;; eeek we can prolly avoid duplication here
  (if response-consumer-factory
    (.performRequestAsync client
                          (name method)
                          url
                          (encode-query-string query-string)
                          (encode-body body)
                          response-consumer-factory
                          (response-listener success error keywordize?)
                          (encode-headers headers))
    (.performRequestAsync client
                          (name method)
                          url
                          (encode-query-string query-string)
                          (encode-body body)
                          (response-listener success error keywordize?)
                          (encode-headers headers))))

(defn request-chan
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

(defn bulk->body
  "Utility function to create _bulk bodies. It takes a sequence of clj
  maps representing _bulk document fragments and returns a newline
  delimited string of JSON fragments"
  [fragments]
  (let [sb (StringBuilder.)]
    (run! #(do (.append sb (json/generate-string %))
               (.append sb"\n"))
          fragments)
    (-> sb .toString Raw.)))

;; (def x (client ["http://localhost:9200"]))
;; (def s (sniffer x))
;; (request x {:url "entries/entry/_search" :method :get :body {}} )
;; (async/<!! (request-ch x {:url "/entries/entry" :method :post :body {:foo "bar"}} ))
