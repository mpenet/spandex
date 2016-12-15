(ns qbits.spandex
  (:require
   [qbits.spandex.options :as options]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import
   (org.elasticsearch.client
    RestClient
    Response
    HttpAsyncResponseConsumerFactory
    ResponseListener)
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

(defn encode-query-string [qs]
  (reduce-kv (fn [m k v] (assoc m (name k) v))
             {}
             qs))

(defn response-decoder
  [^Response response]
  (let [entity  (.getEntity response)
        json? (-> entity .getContentType .getValue (str/index-of "application/json") (> -1))
        content (.getContent entity)]
    {:body (if json?
             (-> content
                 io/reader
                 (json/parse-stream true))
             (slurp content))
     :status (some-> response .getStatusLine .getStatusCode)
     :headers (->> response .getHeaders
                   (reduce (fn [m ^Header h]
                             (assoc! m
                                     (.getName h)
                                     (.getValue h)))
                           (transient {}))
                   persistent!)
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

(defn request-async [^RestClient client
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

;; (def x (client ["http://localhost:9200/asdf"]))
;; (request x {:url "/entries/entry" :method :post :body {:foo "bar"}} )
