(ns qbits.spandex.test.core-test
  (:use clojure.test)
  (:require
   [qbits.spandex :as s]
   [qbits.spandex.utils :as u]))

(deftest test-url-utils
  (is (= (u/url [:foo 1 "bar"]) "foo/1/bar"))
  (is (= (u/url [:foo 1 nil "bar" nil]) "foo/1/bar"))
  (is (= (u/url [:foo 1 ["bar" :baz 2]]) "foo/1/bar,baz,2"))
  (is (= (u/url [:foo 1 ["bar" nil :baz nil 2]]) "foo/1/bar,baz,2"))
  (is (= (u/url []) ""))
  (is (= (u/url nil) "")))

(deftest test-bulk
  (is (= (:value (s/bulk->body [{:foo "bar"} {"bar" {:baz 1}}]))
         "{\"foo\":\"bar\"}\n{\"bar\":{\"baz\":1}}\n"))
  (is (= (:value (s/bulk->body [])) "")))
