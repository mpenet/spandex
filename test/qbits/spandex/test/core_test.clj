(ns qbits.spandex.test.core-test
  (:require
   [clojure.core.async :as async]
   [clojure.spec.alpha :as spec]
   [clojure.test :refer :all]
   [orchestra.spec.test :as spec.test]
   [qbits.spandex :as s]
   [qbits.spandex.url :as url]
   [qbits.spandex.utils :as utils])
  (:import (qbits.spandex Response)))

(def index (java.util.UUID/randomUUID))
(def doc {:some {:fancy "thing"}})
(def doc-id (java.util.UUID/randomUUID))


(def client 
  "The traditional client configuration used with Elasticsearch 7.6 containers for testing."
  (s/client))
#_
(def client 
  "Opensearch client for typical Opensearch demo container. E.g. docker container
  'opensearchproject/opensearch:latest'.  Recent testing used Opensearch 2.0.1"
  (s/client {:hosts ["https://127.0.0.1:9200"]
             :http-client {:ssl-context (qbits.spandex.client-options/ssl-context-trust-all)
                           :basic-auth  {:user "admin" :password "admin"}}}))

(def sniffer
  "If a sniffer is created, asynchronous sniffer logic will query the Elastic/Open-search
  service and try to determine what hosts can be used for requests.  It will fail verbosely
  if you have not configured your docker setup to work with sniffers. This article _may_ help:
  https://www.elastic.co/blog/elasticsearch-sniffing-best-practices-what-when-why-how

  You may wish to comment out the sniffer definition if you haven't configured your docker
  setup to work with it.  It may be broken for other reasons with Opensearch, Opensearch
  sniffer testing has not yet been done successfully."
  (s/sniffer client))

(defn once-fixture [f]
  (try 
    (binding [spec/*fspec-iterations* 0]
      (spec.test/instrument)
      ;; Ensure we can do basic service access, otherwise all the tests will fail
      ;; with tedious and unhelpful stack traces about connections being closed.
      (s/request client {:url [:_cat :templates]})
      ;; Note that F's exceptions will not reach this point, it's intercepted
      ;; by the unit test machinery and reported as an uncaught exception
      (f)
      (spec.test/unstrument))
    (catch Exception e
      (binding [*out* *err*]
        (println "Exception in :once fixture:" (.getMessage e))
        (println "Opensearch / Elasticsearch service probably not reachable or requires credentials missing from the client."))
      (.printStackTrace e))))

(use-fixtures :once once-fixture)
(use-fixtures
  :each
  (fn [t]
    (try
      (s/request client
                 {:method :delete
                  :url [index]})
      (catch clojure.lang.ExceptionInfo ei
        ;; Index not found is okay, anything else is not okay.
        (when-not (= (-> ei ex-data :body :error :root_cause first :type)
                     "index_not_found_exception")
          (println "OOPS:" ei)))
      (catch Exception e 
        (println "OOPS:" e) nil))
    (t)))

(deftest test-url
  (is (= (url/encode [:foo 1 "bar"]) "/foo/1/bar"))
  (is (= (url/encode [:foo 1 nil "bar" nil]) "/foo/1/bar"))
  (is (= (url/encode [:foo 1 ["bar" :baz 2]]) "/foo/1/bar,baz,2"))
  (is (= (url/encode [:foo 1 ["bar" nil :baz nil 2]]) "/foo/1/bar,baz,2"))
  (is (= (url/encode [:foo 1 "bar baz"]) "/foo/1/bar%20baz"))
  (is (= (url/encode []) "/"))
  (is (= (url/encode "/") "/"))
  (is (= (url/encode "/foo") "/foo"))
  (is (= (url/encode nil) "/")))

(deftest test-chunks
  (is (= (:value (s/chunks->body [{:foo "bar"} {"bar" {:baz 1}}]))
         "{\"foo\":\"bar\"}\n{\"bar\":{\"baz\":1}}\n"))
  (is (= (:value (s/chunks->body [])) "")))

(deftest test-sync-query
  (is (->> (s/request client
                      {:url [index :_doc doc-id]
                       :method :post
                       :body doc})
           :status
           (contains? #{200 201})))

  (is (-> (s/request client
                     {:url [index :_doc doc-id]
                      :method :get})
          :body
          :_source
          (= doc)))

  (is (-> (s/request client
                     {:url [index :_doc doc-id]
                      :method :get
                      :keywordize? false})
          :body
          (get "_source")
          (= (clojure.walk/stringify-keys doc)))))

(deftest test-head-req
  (s/request client
             {:url [index :_doc doc-id]
              :method :post
              :body doc})
  (is (-> (s/request client
                     {:url [index :_doc doc-id]
                      :method :head})
          :body
          nil?)))

(deftest test-async-sync-query
  (s/request client
             {:url [index :_doc doc-id]
              :method :post
              :body doc})
  (let [p (promise)]
    (s/request-async client
                     {:url [index :_doc doc-id]
                      :method :get
                      :success (fn [response]
                                 (deliver p response))
                      :error (fn [response]
                               (deliver p response))})
    (is (contains? #{200 201} (:status @p)))))

(defn post-99-docs-and-refresh! []
  "Load 99 documents (one at a time, for no other reason than that's how it's been done) and
  request a shard refresh so that all docs will be visible to subsequent tests."
  (dotimes [i 99]
    (s/request client
                {:url [index :_doc (str i)]
                 :method :post
                 :body doc}))
  (s/request client {:url [index :_refresh]
                     :method :post}))

(deftest test-scrolling-chan
  (post-99-docs-and-refresh!)
  (let [ch (s/scroll-chan client {:url [index :_search]})]
    (-> (async/go
          (loop [docs []]
            (if-let [docs' (-> (async/<! ch) :body :hits :hits seq)]
              (recur (concat docs docs'))
              docs)))
        async/<!!
        count
        (= 99)
        is)))

(deftest test-scrolling-chan-interupted
  (post-99-docs-and-refresh!)
  (let [ch (s/scroll-chan client {:url [index :_search]
                                  :body {:size 1}})]
    (-> (async/go
          (loop [docs []
                 countdown 33]
            (when (zero? countdown)
              (async/close! ch))
            (if-let [docs' (-> (async/<! ch) :body :hits :hits seq)]
              (do
                (recur (concat docs docs')
                       (dec countdown)))
              docs)))
        async/<!!
        count
        (= 33)
        is)))

(deftest test-exceptions []
  (is (try (s/request client {:url "a/b/c/d/"})
           (catch clojure.lang.ExceptionInfo ex
             (->> ex ex-data (instance? Response)))))
  (is (try (s/request client {:url "a/b/c/d/" :exception-handler #(throw %)})
           (catch org.elasticsearch.client.ResponseException ex
             true)
           (catch Throwable _
             (.printStackTrace _)
             false)))
  (->> (async/<!! (s/request-chan client {:url "a/b/c/d/"}))
       ex-data
       (instance? Response)
       is)
  (->> (async/<!! (s/request-chan client {:url "a/b/c/d/" :exception-handler identity}))
       (instance? org.elasticsearch.client.ResponseException)
       is))

(deftest chan->seq-test
  (let [ch (async/chan)]
    (async/put! ch ::a)
    (async/put! ch ::b)
    (async/close! ch)
    (is (= [::a ::b]
           (utils/chan->seq ch)))))
