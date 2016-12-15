(ns qbits.spandex.spec
  (:require
   [qbits.spandex]
   [clojure.spec :as s]
   [clojure.core.async :as async])
  (:import (org.elasticsearch.client RestClient)))

(s/def ::client #(instance? RestClient %))

(alias 'request (create-ns 'qbits.spandex.spec.request))
(s/def ::request (s/keys :req-un [::request/url]
                         :opt-un [::request/scheme
                                  ::request/method
                                  ::request/headers
                                  ::request/query-string
                                  ::request/body]))

(s/def ::request/url string?)
(s/def ::request/scheme #{:http :https})
(s/def ::request/method #{:get :post :put :head})
(s/def ::request/headers (s/map-of (s/or :kw keyword? :str string?)
                                   string?))
(s/def ::request/query-string (s/map-of (s/or :kw keyword? :str string?)
                                        any?))

(s/def ::request/body (s/or :str string?
                            :stream #(instance? java.io.InputStream %)
                            :edn any?))

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

(s/fdef qbits.spandex/request-ch
        :args (s/cat :client ::client
                     :options ::request-async)
        :ret #(instance? clojure.core.async.impl.channels.ManyToManyChannel %))
