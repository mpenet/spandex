(ns qbits.spandex
  (:require
   [clojure.core.async :as async]
   [qbits.spandex.client-options :as client-options]
   [qbits.spandex.sniffer-options :as sniffer-options]
   [qbits.commons.enum :as enum]
   [qbits.spandex.url :as url]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import
   (org.elasticsearch.client.sniff
    Sniffer
    ElasticsearchNodesSniffer
    ElasticsearchNodesSniffer$Scheme
    SniffOnFailureListener)
   (org.elasticsearch.client
    NodeSelector
    Request
    RequestOptions
    RequestOptions$Builder
    RestClient
    ResponseListener
    HttpAsyncResponseConsumerFactory
    ResponseException)
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
    StandardCharsets)
   (java.util.zip
    GZIPInputStream)))

(defn client
  "Returns a client instance to be used to perform requests.

  Options:

  * `:hosts` : Collection of URIs of nodes - Defaults to [\"http://localhost:9200\"]

  * `:max-retry-timeout` : Sets the maximum timeout (in milliseconds) to
  honour in case of multiple retries of the same request. Defaults to
  30000

  * `:default-headers` : Sets the default request headers, which will be
  sent along with each request. Request-time headers will always
  overwrite any default headers.

  * `:failure-listener` : Sets the RestClient.FailureListener to be
  notified for each request failure

  * `:request` : request scoped config - extendable via the `qbits.client-options/set-request-option!` multimethod)
      *  `:authentication?`
      *  `:circular-redirect-allowed?`
      *  `:connect-timeout`
      *  `:connection-request-timeout`
      *  `:content-compression?`
      *  `:expect-continue?`
      *  `:local-address`
      *  `:max-redirects`
      *  `:proxy`
      *  `:redirects?`
      *  `:relative-redirects-allowed?`
      *  `:socket-timeout`
      *  `:target-preferred-auth-schemes`

  * `:http-client` : http-client scoped config - extendable via the `qbits.client-options/set-http-client-option!` multimethod)
      *  `:max-conn-per-route`
      *  `:max-conn-total`
      *  `:proxy`
      *  `:ssl-context`
      *  `:user-agent`
      *  `:auth-caching?`
      *  `:cookie-management?`
      *  `:basic-auth` (map of `:user` `:password`)

  If you need extra/custom building you can hook into the builder by
  extending the multimethod
  `qbits.spandex.client-options/set-option!`"
  ([]
   (client {:hosts ["http://localhost:9200"]}))
  ([options]
   (client-options/builder options)))

(def ^:no-doc sniffer-scheme (enum/enum->fn ElasticsearchNodesSniffer$Scheme))

(defn sniffer
  "Takes a Client instance (and possible sniffer options) and returns
  a sniffer instance that will initially be bound to passed client.
  Options:

  * `:sniff-interval` : Sets the interval between consecutive ordinary
  sniff executions in milliseconds. Will be honoured when
  sniffOnFailure is disabled or when there are no failures between
  consecutive sniff executions.

  * `:sniff-after-failure-delay` : Sets the delay of a sniff execution
  scheduled after a failure (in milliseconds)


  If you need extra/custom building you can hook into the builder by
  extending the multimethod
  `qbits.spandex.sniffer-options/set-option!`"
  ([client]
   (sniffer client nil))
  ([client {:as options
            :keys [scheme timeout]
            :or {scheme :http
                 timeout ElasticsearchNodesSniffer/DEFAULT_SNIFF_REQUEST_TIMEOUT}}]
   (let [sniffer (ElasticsearchNodesSniffer. client
                                             timeout
                                             (sniffer-scheme scheme))]
     (sniffer-options/builder client sniffer options))))

(defn set-sniff-on-failure!
  "Register a SniffOnFailureListener that allows to perform sniffing
  on failure."
  [^Sniffer sniffer]
  (doto (SniffOnFailureListener.)
    (.setSniffer sniffer)))

(defn node-selector
  "Creates a NodeSelector instance that will call `f` as select():
  see: https://github.com/elastic/elasticsearch/blob/master/client/rest/src/main/java/org/elasticsearch/client/NodeSelector.java#L29"
  [f]
  (reify NodeSelector
    (select [this nodes]
      (f nodes))))

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

(def default-headers {"Content-Type" "application/json; charset=UTF8"})

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

(defn ^:no-doc json-entity?
  [^HttpEntity entity]
  (some-> entity
          .getContentType
          .getValue
          (str/index-of "application/json")))

(defn ^:no-doc gzipped-entity?
  [^HttpEntity entity]
  (some-> entity
          .getContentEncoding
          .getValue
          (str/index-of "gzip")))

(defn ^:no-doc response-status
  [^org.elasticsearch.client.Response response]
  (some-> response .getStatusLine .getStatusCode))

(defrecord Response [body status headers hosts])

(defn ^:no-doc response-decoder
  [^org.elasticsearch.client.Response response keywordize?]
  (let [entity (.getEntity response)
        content (when entity (.getContent entity))]
    (Response. (when content
                 (let [content (cond-> content
                                 (gzipped-entity? entity)
                                 (GZIPInputStream.))]
                   (if (json-entity? entity)
                     (-> content io/reader (json/parse-stream keywordize?))
                     (slurp content))))
               (response-status response)
               (response-headers response)
               (.getHost response))))

(defn response-ex->response
  "Return the response-map wrapped in a ResponseException"
  [^ResponseException re]
  (response-decoder (.getResponse re) true))

(defn response-ex->ex-info
  "Utility function to transform an ResponseException into an ex-info"
  [re]
  (ex-info "Response Exception"
           (assoc (response-ex->response re)
                  :type ::response-exception)))

(defprotocol ExceptionDecoder
  (decode-exception [x] "Controls how to translate a client exception"))

(extend-protocol ExceptionDecoder
  ResponseException
  (decode-exception [x] (response-ex->ex-info x))
  Object
  (decode-exception [x] x))

(defn default-exception-handler
  "Exception handler that will try to decode Exceptions via
  ExceptionDecoder/decode-exception. If after decoding we still have a
  throwable it will rethrow, otherwise it will pass on the value
  returned. You can then extend ExceptionDecoder/decode-exception to
  do whatever you'd like."
  [ex]
  (let [x (decode-exception ex)]
    (if (instance? Exception x)
      (throw x)
      x)))

(defn ^:no-doc response-listener
  [success error keywordize? exception-handler]
  (reify ResponseListener
    (onSuccess [this response]
      (when success
        (success (response-decoder response keywordize?))))
    (onFailure [this ex]
      (when error
        (error (exception-handler ex))))))

(defn ->request
  [{:as request-map
    :keys [url method query-string headers body
           response-consumer-factory
           ^RequestOptions$Builder request-options]
    :or {method :get
         headers default-headers}}]
  (let [request-options (or request-options
                            (.toBuilder (RequestOptions/DEFAULT)))
        request (Request. (name method)
                          (url/encode url))]
    (when headers
      (run! (fn [[k v]]
              (.addHeader request-options
                          (name k)
                          (str v)))
            headers))
    (when response-consumer-factory
      (.setHttpAsyncResponseConsumerFactory request-options
                                            response-consumer-factory))
    (when query-string
      (run! (fn [[k v]]
              (.addParameter request
                             (name k)
                             (str v)))
            query-string))
    (when body
      (.setEntity request (encode-body body)))

    (doto request
      (.setOptions request-options))))

(defn request
  [^RestClient client {:keys [method url headers query-string body
                              keywordize?
                              response-consumer-factory
                              exception-handler]
                       :or {method :get
                            keywordize? true
                            exception-handler default-exception-handler
                            response-consumer-factory HttpAsyncResponseConsumerFactory/DEFAULT}
                       :as request-params}]
  (try
    (-> client
        (.performRequest (->request request-params))
        (response-decoder keywordize?))
    (catch Exception e
      (exception-handler e))))

(defn request-async
  "Similar to `qbits.spandex/request` but returns immediately and works
  asynchronously and triggers option `:success` once a results is
  received, or `:error` if it was a failure"
  [^RestClient client
   {:keys [method url headers query-string body
           success error keywordize?
           response-consumer-factory
           exception-handler]
    :or {method :get
         keywordize? true
         exception-handler decode-exception
         response-consumer-factory HttpAsyncResponseConsumerFactory/DEFAULT}
    :as request-params}]
  (try
    (.performRequestAsync client
                          (->request request-params)
                          (response-listener success
                                             error
                                             keywordize?
                                             exception-handler))
    (catch Exception e
      (exception-handler e))))

(defn request-chan
  "Similar to `qbits.spandex/request` but runs asynchronously and
  returns a `core.async/promise-chan` that will have the result (or
  error) delivered upon reception"
  [^RestClient client {:as options
                       :keys [ch]}]
  (let [ch (or ch (async/promise-chan))]
    (try
      (request-async client
                     (assoc options
                            :success (fn [response]
                                       (async/put! ch response))
                            :error (fn [ex]
                                     (async/put! ch ex))))
      (catch Exception e
        (async/put! ch e)))
    ch))

(defn scroll-chan
  "Returns a core async channel. Takes the same args as
  `qbits.spandex/request`. Perform async scrolling requests for a
  query, request will be done as the user takes from the
  channel. Every take!  will request/return a page from the
  scroll. You can specify scroll :ttl in the request map otherwise
  it'll default to 1m.  The chan will be closed once scroll is
  complete. If you must stop scrolling before that, you must
  async/close! manually, this will release all used resources. You can
  also supply a :output-ch key to the request map, a core.async/chan that
  will receive the results. This allow you to have custom buffers, or
  have multiple scroll-chan calls feed the same channel instance"
  [client {:as request-map :keys [ttl output-ch]
           :or {ttl "1m"}}]
  (let [ch (or output-ch (async/chan))]
    (async/go
      (let [response (async/<! (request-chan client
                                             (assoc-in request-map
                                                       [:query-string :scroll]
                                                       ttl)))
            scroll-id (some-> response :body :_scroll_id)]
        (async/>! ch response)
        (when (and (-> response :body :hits :hits count (> 0))
                   scroll-id)
          (loop [scroll-id scroll-id]
            (let [response
                  (async/<! (request-chan client
                                          (merge request-map
                                                 {:method :post
                                                  :url "/_search/scroll"
                                                  :body {:scroll_id scroll-id
                                                         :scroll ttl}})))]
              ;; it's an error and we must exit the consuming process
              (if (or (instance? Exception response)
                      (not= 200 (:status response)))
                (async/>! ch response)
                ;; we need to make sure the user didn't close the
                ;; returned chan for scroll interuption and that we
                ;; actually have more results to feed
                (let [body (:body response)]
                  (when (and (-> body :hits :hits seq)
                             (async/>! ch response))
                    (recur (:_scroll_id body))))))))

        ;; When we're done with a scroll we tidy it up early.
        ;; Otherwise we have to wait for the ttl to expire.
        ;; Let's be nice to ES.
        (async/<! (request-chan client
                                (merge request-map
                                       {:method :delete
                                        :url "/_search/scroll"
                                        :body {:scroll_id scroll-id}})))

        (async/close! ch)))
    ch))

(defn chunks->body
  "Utility function to create _bulk/_msearch bodies. It takes a
  sequence of clj fragments and returns a newline delimited string of
  JSON fragments"
  [chunks]
  (let [sb (StringBuilder.)]
    (run! #(do (.append sb (json/generate-string %))
               (.append sb "\n"))
          chunks)
    (-> sb .toString Raw.)))

(def bulk-chan
  "Bulk-chan takes a client, a partial request/option map, returns a
  map of :input-ch :output-ch. :input-ch is a channel that will accept
  bulk fragments to be sent (either single or collection). It then will
  wait (:flush-interval request-map) or (:flush-threshold request-map)
  and then trigger an async request with the bulk payload accumulated.
  Parallelism of the async requests is controllable
  via (:max-concurrent-requests request-map). If the number of triggered
  requests exceeds the capacity of the job buffer, puts! in :input-ch will
  block (if done with async/put! you can check the return value before
  overflowing the put! pending queue). Jobs results returned from the
  processing are a pair of job and responses map, or exception.  The
  :output-ch will allow you to inspect [job responses] the server
  returned and handle potential errors/failures accordingly (retrying
  etc). If you close! the :input-ch it will close the underlying
  resources and exit cleanly (consuming all jobs that remain in
  queues). By default requests are run against _bulk, but the option
  map is passed as is to request-chan, you can overwrite options here
  and provide your own url, headers and so on."
  (letfn [(par-run! [in-ch out-ch f n]
            (let [procs (async/merge (repeatedly n
                                                 (fn []
                                                   (async/go-loop []
                                                     (if-let [job (async/<! in-ch)]
                                                       (let [result (async/<! (f job))]
                                                         (async/>! out-ch [job result])
                                                         (recur))
                                                       ::exit))))
                                     n)]
              ;; if upstream ports are closed propagate close! to out-ch
              (async/go-loop []
                (if (async/<! procs)
                  (recur)
                  (async/close! out-ch)))))
          (build-map [request-map payload]
            (assoc request-map
                   :body
                   (->> payload
                        (reduce (fn [payload chunk]
                                  (if (sequential? chunk)
                                    (concat payload chunk)
                                    (conj payload chunk)))
                                [])
                        chunks->body)))]
    (fn bulk-chan
      ([client] (bulk-chan client {}))
      ([client {:as request-map
                :keys [flush-interval
                       flush-threshold
                       input-ch
                       output-ch
                       max-concurrent-requests]
                :or {flush-interval 5000
                     flush-threshold 300
                     max-concurrent-requests 3}}]
       (let [request-map (conj {:url "/_bulk"
                                :method :put
                                :headers {"Content-Type" "application/x-ndjson"}}
                               request-map)
             input-ch (or input-ch (async/chan))
             output-ch (or output-ch (async/chan))
             request-ch (async/chan max-concurrent-requests)]
         ;; run request processor
         (par-run! request-ch
                   output-ch
                   #(request-chan client (build-map request-map %))
                   max-concurrent-requests)
         (async/go-loop
             [payload []
              timeout-ch (async/timeout flush-interval)]
           (let [[chunk ch] (async/alts! [timeout-ch input-ch])]
             (cond
               (= timeout-ch ch)
               (do (when (seq payload)
                     (async/>! request-ch payload))
                   (recur [] (async/timeout flush-interval)))

               (= input-ch ch)
               (if (nil? chunk)
                 (do (async/close! input-ch)
                     (when (seq payload)
                       (async/>! request-ch payload))
                     (async/close! request-ch))
                 (let [payload (conj payload chunk)]
                   (if (= flush-threshold (count payload))
                     (do (async/>! request-ch payload)
                         (recur [] (async/timeout flush-interval)))
                     (recur payload timeout-ch)))))))
         {:input-ch input-ch
          :output-ch output-ch
          :request-ch request-ch})))))
