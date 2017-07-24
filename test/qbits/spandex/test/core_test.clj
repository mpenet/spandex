(ns qbits.spandex.test.core-test
  (:refer-clojure :exclude [type])
  (:use clojure.test)
  (:require
   [clojure.core.async :as async]
   [qbits.spandex :as s]
   [qbits.spandex.url :as url])
  (:import (qbits.spandex Response)))

(try
  (require 'qbits.spandex.spec)
  (require 'clojure.spec.test.alpha)
  ((resolve 'clojure.spec.test.alpha/instrument))
  (println "Instrumenting qbits.spandex with clojure.spec")
  (catch Exception e
    (.printStackTrace e)))

(def server "http://127.0.0.1:9200")
(def index (java.util.UUID/randomUUID))
(def type (java.util.UUID/randomUUID))

(def doc {:some {:fancy "thing"}})
(def doc-id (java.util.UUID/randomUUID))
(def client (s/client))

(defn wait! []
  (Thread/sleep 3000))

(use-fixtures
  :each
  (fn [t]
    (try
      (s/request client
                 {:method :delete
                  :url [index type]})
      (catch Exception _ nil))
    (t)))

(deftest test-url
  (is (= (url/encode [:foo 1 "bar"]) "/foo/1/bar"))
  (is (= (url/encode [:foo 1 nil "bar" nil]) "/foo/1/bar"))
  (is (= (url/encode [:foo 1 ["bar" :baz 2]]) "/foo/1/bar,baz,2"))
  (is (= (url/encode [:foo 1 ["bar" nil :baz nil 2]]) "/foo/1/bar,baz,2"))
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
  (is (-> (s/request client
                     {:url [index type doc-id]
                      :method :head})
          :body
          nil?)))

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

(deftest test-scrolling-chan
  (dotimes [i 99]
    (s/request client
               {:url [index type (str i)]
                :method :post
                :body doc}))
  (wait!)
  (let [ch (s/scroll-chan client {:url [index type :_search]})]
    (-> (async/go
          (loop [docs []]
            (if-let [docs' (-> (async/<! ch) :body :hits :hits seq)]
              (recur (concat docs docs'))
              docs)))
        async/<!!
        count
        (= 100)
        is)))

(deftest test-scrolling-chan-interupted
  (dotimes [i 99]
    (s/request client
               {:url [index type (str i)]
                :method :post
                :body doc}))
  (wait!)
  (let [ch (s/scroll-chan client {:url [index type :_search]
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
             (->> ex ex-data (instance? qbits.spandex.Response)))))
  (is (try (s/request client {:url "a/b/c/d/" :exception-handler #(throw %)})
           (catch org.elasticsearch.client.ResponseException ex
             true)
           (catch Throwable _
             false)))
  (->> (async/<!! (s/request-chan client {:url "a/b/c/d/"}))
       ex-data
       (instance? qbits.spandex.Response)
       is)
  (->> (async/<!! (s/request-chan client {:url "a/b/c/d/" :exception-handler identity}))
       (instance? org.elasticsearch.client.ResponseException)
       is))
