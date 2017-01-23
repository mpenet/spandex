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

(defmethod set-option! :http-client-config-callback
  [_ ^RestClientBuilder builder f]
  (-> builder (.setHttpClientConfigCallback (http-client-config-callback f))))

(defmethod set-option! :request-config-callback
  [_ ^RestClientBuilder builder f]
  (-> builder (.setRequestConfigCallback (request-config-callback f))))

(defmethod set-option! :request
  [_ ^RestClientBuilder builder
   {:as opts
    :keys [connection-request-timeout
           connect-timout
           cookie-spec
           decompression?
           content-compression?
           authentication?
           circular-redirect?
           expect-continue?
           local-address
           max-redirects
           proxy
           redirects?
           relative-redirects?
           socket-timeout
           preferred-auth-schemes]}]
  ;; https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.Builder.html
  (-> builder
      (.setRequestConfigCallback
       (request-config-callback
        #(cond-> ^RequestConfig$Builder %
           authentication?
           (.setAuthenticationEnabled (boolean authentication?))
           circular-redirect?
           (.setCircularRedirectsAllowed (boolean circular-redirect?))
           connect-timout
           (.setConnectTimeout (int connect-timout))
           connection-request-timeout
           (.setConnectionRequestTimeout (int connection-request-timeout))
           content-compression?
           (.setContentCompressionEnabled (boolean content-compression?))
           cookie-spec
           (.setCookieSpec cookie-spec)
           decompression?
           (.setDecompressionEnabled (boolean decompression?))
           expect-continue?
           (.setExpectContinueEnabled (boolean expect-continue?))
           local-address
           (.setLocalAddress local-address)
           max-redirects
           (.setMaxRedirects max-redirects)
           proxy
           (.setProxy proxy)
           redirects?
           (.setRedirectsEnabled redirects?)
           relative-redirects?
           (.setRelativeRedirectsAllowed relative-redirects?)
           socket-timeout
           (.setSocketTimeout (int socket-timeout))
           preferred-auth-schemes
           (.setTargetPreferredAuthSchemes preferred-auth-schemes))))))

(defmethod set-option! :http-client
  [_ ^RestClientBuilder builder
   {:as opts
    :keys [max-conn-per-route
           max-conn-total
           proxy
           ssl-context
           user-agent
           ;; automatic-retries?
           auth-caching?
           cookie-management?
           basic-auth]}]
  ;; https://hc.apache.org/httpcomponents-asyncclient-dev/httpasyncclient/apidocs/org/apache/http/impl/nio/client/HttpAsyncClientBuilder.html
  (-> builder
      (.setRequestConfigCallback
       (http-client-config-callback
        #(cond-> ^HttpAsyncClientBuilder %
           max-conn-per-route
           (.setMaxConnPerRoute (int max-conn-per-route))
           max-conn-total
           (.setMaxConnTotal (int max-conn-total))
           proxy
           (.setProxy proxy)
           ssl-context
           (.setSSLContext ssl-context)
           user-agent
           (.setUserAgent user-agent)
           (not auth-caching?)
           (.disableAuthCaching)
           (not cookie-management?)
           (.disableCookieManagement)
           basic-auth
           (.setDefaultCredentialsProvider
            (doto (BasicCredentialsProvider.)
              (.setCredentials AuthScope/ANY
                               (UsernamePasswordCredentials.
                                (:user basic-auth)
                                (:password basic-auth))))))))))

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
