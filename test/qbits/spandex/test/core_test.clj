(ns qbits.spandex.test.core-test
  (:refer-clojure :exclude [type])
  (:require
   [clojure.test :refer :all]
   [clojure.core.async :as async]
   [qbits.spandex :as s]
   [qbits.spandex.url :as url]
   [qbits.spandex.utils :as utils])
  (:import (qbits.spandex Response)
           (clojure.lang ExceptionInfo)))

(try
  (require 'qbits.spandex.spec)
  (require 'clojure.spec.test.alpha)
  ((resolve 'clojure.spec.test.alpha/instrument))
  (println "Instrumenting qbits.spandex with clojure.spec")
  (catch Exception e
    (.printStackTrace e)))

(def server "http://127.0.0.1:9200")
(def index (java.util.UUID/randomUUID))
(def type :_doc)

(def doc {:some {:fancy "thing"}})
(def doc-id (java.util.UUID/randomUUID))
(def client (s/client {:hosts [server]}))
(def sniffer (s/sniffer client))

(use-fixtures
  :each
  (fn [t]
    (t)
    (try
      (s/request client
                 {:method :delete
                  :url [index]})
      (catch Exception _ nil))))

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
                      {:url [index type doc-id]
                       :method :post
                       :body doc})
           :status
           (contains? #{200 201})))

  (is (-> (s/request client
                     {:url [index type doc-id]
                      :method :get})
          :body
          :_source
          (= doc)))

  (is (-> (s/request client
                     {:url [index type doc-id]
                      :method :get
                      :keywordize? false})
          :body
          (get "_source")
          (= (clojure.walk/stringify-keys doc)))))

(deftest test-head-req
  (try
    (s/request client
               {:url [index type doc-id]
                :method :head})
    (is false)
    (catch ExceptionInfo ex
      (is (-> ex ex-data :status (= 404))))))

(deftest test-async-sync-query
  (s/request client
             {:url [index type doc-id]
              :method :post
              :body doc})
  (let [p (promise)]
    (s/request-async client
                     {:url [index type doc-id]
                      :method :get
                      :success (fn [response]
                                 (deliver p response))
                      :error (fn [response]
                               (deliver p response))})
    (is (contains? #{200 201} (:status @p)))))

(defn- fill-in-docs!
  [i]
  (s/request client
             {:url [index type "_bulk"]
              :method :post
              :query-string {:refresh true}
              :body (->> (for [i (range i)]
                           [{:index {:_id i}}
                            {:value i}])
                         (mapcat identity)
                         (s/chunks->body))}))

(deftest test-scrolling-chan
  (fill-in-docs! 100)
  (let [ch (s/scroll-chan client {:url [index type :_search]
                                  :body {:sort {:value :asc}}})]
    (-> (async/go
          (loop [docs []]
            (if-let [docs' (-> (async/<! ch) :body :hits :hits seq)]
              (recur (concat docs docs'))
              docs)))
        async/<!!
        (->> (map :_source))
        (= (for [i (range 100)] {:value i}))
        is)))

(deftest test-scrolling-chan-interupted
  (fill-in-docs! 100)
  (let [ch (s/scroll-chan client {:url [index type :_search]
                                  :body {:size 1
                                         :sort {:value :asc}}})]
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
        (->> (map :_source))
        (= (for [i (range 33)] {:value i}))
        is)))

(deftest test-search-after-chan
  (fill-in-docs! 100)
  (let [ch (s/search-after-chan client
                                {:url [index type :_search]
                                 :body {:size 1
                                        :sort {:value :asc}}})]
    (-> (async/go
          (loop [docs []]
            (if-let [docs' (-> (async/<! ch) :body :hits :hits seq)]
              (recur (concat docs docs'))
              docs)))
        async/<!!
        (->> (map :_source))
        (= (for [i (range 100)] {:value i}))
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
