(ns qbits.spandex.spec
  (:require
   [qbits.spandex]
   [clojure.spec.alpha :as s]
   [qbits.spandex.url]
   [qbits.spandex.utils :as u])
  (:import
   (java.net InetAddress)
   (javax.net.ssl SSLContext)
   (org.elasticsearch.client
    NodeSelector
    RestClient
    RestClient$FailureListener)))

(s/def ::chan
  #(instance? clojure.core.async.impl.channels.ManyToManyChannel %))

(s/def ::client #(instance? RestClient %))

(alias 'client-options (create-ns 'qbits.spandex.spec.client-options))
(s/def ::client-options (s/keys :opt-un [::client-options/max-retry-timeout
                                         ::client-options/default-headers
                                         ::client-options/failure-listener
                                         ::client-options/node-selector
                                         ::client-options/http-client
                                         ::client-options/request]))

(s/def ::client-options/failure-listener #(instance? RestClient$FailureListener %))
(s/def ::client-options/max-retry-timeout int?)
(s/def ::client-options/default-headers (s/map-of (s/or :kw keyword? :str string?)
                                                  string?))
(s/def ::client-options/node-selector #(instance? NodeSelector %))

(alias 'http-client-options (create-ns 'qbits.spandex.spec.client-options.http-client))
(alias 'basic-auth
       (create-ns 'qbits.spandex.spec.client-options.http-client.basic-auth))
(s/def ::client-options/http-client
  (s/keys :opt-un [::http-client-options/max-conn-per-route
                   ::http-client-options/max-conn-total
                   ::http-client-options/proxy
                   ::http-client-options/ssl-context
                   ::http-client-options/user-agent
                   ::http-client-options/basic-auth
                   ::http-client-options/auth-caching?
                   ::http-client-options/cookie-management?]))
(s/def ::http-client-options/max-conn-per-route pos-int?)
(s/def ::http-client-options/max-conn-total pos-int?)
(s/def ::http-client-options/user-agent string?)
(s/def ::http-client-options/auth-caching? boolean?)
(s/def ::http-client-options/cookie-management? boolean?)
(s/def ::http-client-options/basic-auth
  (s/keys :req-un [::basic-auth/user
                   ::basic-auth/password]))
(s/def ::http-client-options/ssl-context #(instance? SSLContext %))
(s/def ::http-client-options/proxy any?)
(s/def ::basic-auth/user string?)
(s/def ::basic-auth/password string?)

(alias 'request-options (create-ns 'qbits.spandex.spec.client-options.request))
(s/def ::client-options/request
  (s/keys :opt-un [::request-options/authentication?
                   ::request-options/circular-redirect-allowed?
                   ::request-options/connect-timeout
                   ::request-options/connect-request-timeout
                   ::request-options/content-compression?
                   ::request-options/cookie-spec
                   ::request-options/expect-continue?
                   ::request-options/local-address
                   ::request-options/cookie-spec
                   ::request-options/max-redirects
                   ::request-options/proxy
                   ::request-options/redirects?
                   ::request-options/relative-redirects-allowed?
                   ::request-options/socket-timeout
                   ::request-options/target-preferred-auth-schemes]))
(s/def ::request-options/authentication? boolean?)
(s/def ::request-options/circular-redirect-allowed? boolean?)
(s/def ::request-options/content-compression? boolean?)
(s/def ::request-options/expect-continue? boolean?)
(s/def ::request-options/redirect? boolean?)
(s/def ::request-options/relative-redirects-allowed? boolean?)

(s/def ::request-options/connect-timeout pos-int?)
(s/def ::request-options/connect-request-timeout pos-int?)
(s/def ::request-options/max-redirects int?)
(s/def ::request-options/socket-timeout pos-int?)

(s/def ::request-options/proxy any?)
(s/def ::request-options/cookie-spec any?)
(s/def ::request-options/local-address #(instance? InetAddress %))

(alias 'sniffer-options (create-ns 'qbits.spandex.spec.sniffer-options))
(s/def ::sniffer-options (s/keys :opt-un [::sniffer-options/sniff-interval
                                          ::sniffer-options/sniff-after-failure-delay]))
(s/def ::sniffer-options/sniff-interval int?)
(s/def ::sniffer-options/sniff-after-failure-delay int?)

(alias 'request (create-ns 'qbits.spandex.spec.request))
(s/def ::request (s/keys :req-un [::request/url]
                         :opt-un [::request/scheme
                                  ::request/method
                                  ::request/headers
                                  ::request/query-string
                                  ::request/body
                                  ::request/exception-handler]))

(s/def ::request/url #(satisfies? qbits.spandex.url/URL %))
(s/def ::request/scheme #{:http :https})
(s/def ::request/method #{:get :post :put :head :delete})
(s/def ::request/headers (s/map-of (s/or :kw keyword? :str string?)
                                   string?))
(s/def ::request/query-string (s/map-of (s/or :kw keyword? :str string?)
                                        any?))

(s/def ::request/body (s/or :str string?
                            :raw #(instance? qbits.spandex.Raw %)
                            :stream #(instance? java.io.InputStream %)
                            :edn any?))

(s/def ::request/exception-handler
  fn?
  ;; (s/fspec :args (s/cat :throwable #(instance? Throwable %)))
  )

(alias 'response (create-ns 'qbits.spandex.spec.response))
(s/def ::response (s/keys :req-un [::response/body
                                   ::response/status
                                   ::response/headers
                                   ::response/host]))

(s/def ::response/headers (s/map-of string? string?))
(s/def ::response/status pos-int?)
(s/def ::response/body (s/nilable any?)) ;; edn/clj?
(s/def ::response/host any?) ;; to be refine

(s/def ::request-async
  (s/merge ::request
           (s/keys :opt-un [::success ::error ::response-consumer-factory])))

(s/def ::success fn?) ;; refine
(s/def ::error fn?) ;; refine

(s/fdef qbits.spandex/request
  :args (s/cat :client ::client
               :options ::request)
  :ret ::response)

(s/fdef qbits.spandex/request-async
  :args (s/cat :client ::client
               :options ::request-async))

(s/fdef qbits.spandex/request-chan
  :args (s/cat :client ::client
               :options ::request-async)
  :ret ::chan)

(s/fdef qbits.spandex/chunks->body
  :args (s/cat :fragments (s/coll-of map?))
  :ret string?)

(s/def ::ttl int?)
(s/def ::ch int?)
(s/fdef qbits.spandex/scroll-chan
  :args (s/cat :client ::client
               :options (s/and ::request (s/keys :opt-un [::ttl ::ch])))
  :ret ::chan)

(s/def ::input-ch ::chan)
(s/def ::output-ch ::chan)
(s/def ::request-ch ::chan)
(s/def ::flush-interval pos-int?)
(s/def ::flush-threshold pos-int?)
(s/def ::max-concurrent-requests pos-int?)
(s/def ::bulk-chan-options (s/keys :opt-un [::input-ch
                                            ::output-ch
                                            ::flush-threshold
                                            ::flush-interval
                                            ::max-concurrent-requests]))
(s/fdef qbits.spandex/bulk-chan
  :args (s/cat :client ::client
               :options ::bulk-chan-options)
  :ret (s/keys :req-un [::input-ch ::output-ch ::request-ch]))

;; utils
(s/fdef qbits.spandex.url/encode
  :args (s/cat :parts (s/* (s/nilable #(satisfies? qbits.spandex.url/URLFragment %))))
  :ret string?)

(s/fdef qbits.spandex.utils/chan->seq
  :args (s/cat :chan ::chan)
  :ret (s/nilable (s/coll-of (s/or :response ::response
                                   :exception #(instance? Exception %)))))
