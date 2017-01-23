(ns qbits.spandex.client-options
  (:import
   (org.apache.http
    HttpHost)
   (org.apache.http.client.config
    RequestConfig$Builder)
   (org.apache.http.impl.nio.client
    HttpAsyncClientBuilder)
   (org.apache.http.impl.client
    BasicCredentialsProvider)
   (org.apache.http.auth
    UsernamePasswordCredentials)
   (org.apache.http.auth
    AuthScope)
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

(defn ^:no-doc http-client-config-callback [f]
  (reify RestClientBuilder$HttpClientConfigCallback
    (customizeHttpClient [this builder]
      (f builder))))

;; request opts
(defmulti set-request-option! (fn [k builder option] k))

(defmethod set-request-option! :authentication?
  [_ ^RequestConfig$Builder builder authentication?]
  (.setAuthenticationEnabled builder (boolean authentication?)))

(defmethod set-request-option! :circular-redirect-allowed?
  [_ ^RequestConfig$Builder builder circular-redirect-allowed?]
  (.setCircularRedirectsAllowed builder (boolean circular-redirect-allowed?)))

(defmethod set-request-option! :connect-timeout
  [_ ^RequestConfig$Builder builder connect-timout]
  (.setConnectTimeout builder (int connect-timout)))

(defmethod set-request-option! :connection-request-timeout
  [_ ^RequestConfig$Builder builder connection-request-timeout]
  (.setConnectionRequestTimeout builder (int connection-request-timeout)))

(defmethod set-request-option! :content-compression?
  [_ ^RequestConfig$Builder builder content-compression?]
  (.setContentCompressionEnabled builder (boolean content-compression?)))

(defmethod set-request-option! :cookie-spec
  [_ ^RequestConfig$Builder builder cookie-spec]
  (.setCookieSpec builder cookie-spec))

(defmethod set-request-option! :decompression?
  [_ ^RequestConfig$Builder builder decompression?]
  (.setDecompressionEnabled builder (boolean decompression?)))

(defmethod set-request-option! :expect-continue?
  [_ ^RequestConfig$Builder builder expect-continue?]
  (.setExpectContinueEnabled builder (boolean expect-continue?)))

(defmethod set-request-option! :local-address
  [_ ^RequestConfig$Builder builder local-address]
  (.setLocalAddress builder local-address))

(defmethod set-request-option! :max-redirects
  [_ ^RequestConfig$Builder builder max-redirects]
  (.setMaxRedirects builder (int max-redirects)))

(defmethod set-request-option! :proxy
  [_ ^RequestConfig$Builder builder proxy]
  (.setProxy builder proxy))

(defmethod set-request-option! :redirects?
  [_ ^RequestConfig$Builder builder redirects?]
  (.setRedirectsEnabled builder (boolean redirects?)))

(defmethod set-request-option! :relative-redirects-allowed?
  [_ ^RequestConfig$Builder builder relative-redirects-allowed?]
  (.setRelativeRedirectsAllowed builder (boolean relative-redirects-allowed?)))

(defmethod set-request-option! :socket-timeout
  [_ ^RequestConfig$Builder builder socket-timeout]
  (.setSocketTimeout builder (int socket-timeout)))

(defmethod set-request-option! :target-preferred-auth-schemes
  [_ ^RequestConfig$Builder builder target-preferred-auth-schemes]
  (.setTargetPreferredAuthSchemes builder target-preferred-auth-schemes))


;; http-client options
(defmulti set-http-client-option! (fn [k builder option] k))

(defmethod set-http-client-option! :max-conn-per-route
  [_ ^HttpAsyncClientBuilder builder max-conn-per-route]
  (.setMaxConnPerRoute builder (int max-conn-per-route)))

(defmethod set-http-client-option! :max-conn-total
  [_ ^HttpAsyncClientBuilder builder max-conn-total]
  (.setMaxConnTotal builder (int max-conn-total)))

(defmethod set-http-client-option! :proxy
  [_ ^HttpAsyncClientBuilder builder proxy]
  (.setProxy builder proxy))

(defmethod set-http-client-option! :ssl-context
  [_ ^HttpAsyncClientBuilder builder ssl-context]
  (.setSSLContext builder ssl-context))

(defmethod set-http-client-option! :user-agent
  [_ ^HttpAsyncClientBuilder builder user-agent]
  (.setUserAgent builder user-agent))

(defmethod set-http-client-option! :auth-caching?
  [_ ^HttpAsyncClientBuilder builder auth-caching?]
  (cond-> builder
    (not auth-caching?) (.disableAuthCaching)))

(defmethod set-http-client-option! :cookie-management?
  [_ ^HttpAsyncClientBuilder builder cookie-management?]
  (cond-> builder
    (not cookie-management?) (.disableCookieManagement)))

(defmethod set-http-client-option! :basic-auth
  [_ ^HttpAsyncClientBuilder builder {:keys [user password]}]
  (-> builder
      (.setDefaultCredentialsProvider
       (doto (BasicCredentialsProvider.)
         (.setCredentials AuthScope/ANY
                          (UsernamePasswordCredentials. user
                                                        password))))))

;; top level options
(defmulti ^:no-doc set-option! (fn [k builder option] k))

(defmethod set-option! :max-retry-timeout
  [_ ^RestClientBuilder builder timeout-ms]
  (-> builder (.setMaxRetryTimeoutMillis (int timeout-ms))))

(defmethod set-option! :default-headers
  [_ ^RestClientBuilder builder headers]
  (-> builder (.setDefaultHeaders (into-array BasicHeader
                                              (map (fn [[k v]]
                                                     (BasicHeader. (name k) v))
                                                   headers)))))

(defmethod set-option! :request
  [_ ^RestClientBuilder builder options]
  ;; this dispatches on set-request-option!, see the following link for more details
  ;; https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.Builder.html
  (.setRequestConfigCallback builder
                             (request-config-callback
                              #(reduce (fn [builder [k option]]
                                         (set-request-option! k builder option))
                                       %
                                       options))))

(defmethod set-option! :http-client
  [_ ^RestClientBuilder builder options]
  ;; this dispatches on set-http-client-option!, see the following link for more details
  ;; https://hc.apache.org/httpcomponents-asyncclient-dev/httpasyncclient/apidocs/org/apache/http/impl/nio/client/HttpAsyncClientBuilder.html
  (.setHttpClientConfigCallback builder
                                (http-client-config-callback
                                 #(reduce (fn [builder [k option]]
                                            (set-http-client-option! k builder option))
                                          %
                                          options))))

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
