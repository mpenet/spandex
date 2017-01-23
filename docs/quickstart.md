# Quickstart

## Setup

```clojure
(require '[qbits.spandex :as s])

(def c (s/client {:hosts ["http://127.0.0.1:9200" "https://foo2:3838"]}))

;; add optional sniffer
(def s (s/sniffer c {... options ...}))
```

## Blocking requests

```clojure
(s/request c {:url "/entries/entry/_search"
              :method :get
              :body {:query {:match_all {}}}})

>> {:body {:_index "entries", :_type "entry", :_id "AVkDDJvdkd2OsNWu4oYk", :_version 1, :_shards {:total 2, :successful 1, :failed 0}, :created true}, :status 201, :headers {"Content-Type" "application/json; charset=UTF-8", "Content-Length" "141"}, :host #object[org.apache.http.HttpHost 0x62b90fad "http://127.0.0.1:9200"]}

```

## Async requests (callbacks)

```clojure
(s/request-async c {:url "/urls/url/"
                    :method :get
                    :body {:query {:match {:message "this is a test"}}}
                    :success (fn [response-as-clj] ... )
                    :error (fn [ex] :boom)})
```

## Async requests: `core.async/promise-chan`

```clojure

(require '[clojure.core.async :as async])

(async/<!! (s/request-chan c {:url "/urls/url/"
                              :method :get
                              :body {:query {:match {:message "this is a test"}}}}))
```

## Scrolling
Scrolling via core.async (fully NIO internally), interuptable if you
async/close! the returned chan.

```clojure
(async/go
  (let [ch (s/scroll-chan client {:url "/foo/_search" :body {:query {:match_all {}}}})]
    (loop []
      (when-let [page (async/<! ch)]
        (do-something-with-page page)
        (recur)))))
```

## Simple bulk request

You can just do it this way, but `bulk-chan` (below) is way nicer for this.

``` clojure
(s/request c {:url "/_bulk"
              :method :put
              :body (s/chunks->body [{:delete {:_index "foo" :_id "1234"}}
                                     {:_index :bar}
                                     {:create {...}}])})
```

## Bulk requests scheduling

"Faux streaming" of _bulk requests (flushes bulk request after
interval or threshold, you can specify these as options). Uses
request-chan internally, so it's quite cheap.

```clojure
(let [{:keys [input-ch output-ch]} (bulk-chan client {:flush-threshold 100
                                                      :flush-interval 5000
                                                      :max-concurrent-requests 3})]
  ;; happily takes a sequence of actions or single fragments
  (async/put! input-ch [{:delete {:_index "foo" :_id "1234"}} {:_index :bar} {:create {...}}])
  (async/put! input-ch {"delete" {"_index" "website" "_type" "blog" "_id" "123"}}))

;; setup an response consumer (we just want to make sure we don't clog this channel)
(future (loop [] (async/<!! (:output-ch c))))
```
