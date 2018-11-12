# spandex
<!-- [![Build Status](https://travis-ci.org/mpenet/spandex.svg?branch=master)](https://travis-ci.org/mpenet/spandex) -->

Elasticsearch new low level [rest-client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/index.html) wrapper

## Why?

To quote ["State of the official Elasticsearch Java clients"](https://www.elastic.co/blog/state-of-the-official-elasticsearch-java-clients)

> The Java REST client is the future for Java users of Elasticsearch.

Because the legacy native client is a bit of a nightmare to deal with
(for many reasons) and the new REST client is quite capable and fast
too, see ["Benchmarking REST client and transport client"](https://www.elastic.co/blog/benchmarking-rest-client-transport-client)

Not to mention it supports some interesting features:

* compatibility with any Elasticsearch version

* load balancing across all available nodes

* failover in case of node failures and upon specific response codes

* failed connection penalization

* persistent connections

* trace logging of requests and responses

* optional automatic discovery of cluster nodes (also known as sniffing)

## Goals

* Be minimal & performant

* RING inspired

* All "exotic" features should be optional

* Not a giant DSL over another DSL, just maps everywhere.
  Read ElasticSearch doc -> done, not another layer of indirection

* Provide minimal (and optional) utils to do the boring stuff
  (bulk, scroll queries, compose urls)

* Can do async via simple callbacks based api or `core.async`

* Provide [specs](https://github.com/mpenet/spandex/blob/master/src/clj/qbits/spandex/spec.clj)

### Setup

```clojure
(require '[qbits.spandex :as s])

(def c (s/client {:hosts ["http://127.0.0.1:9200" "https://foo2:3838"]}))

;; add optional sniffer
(def s (s/sniffer c {... options ...}))
```

#### Constructing URLs

Most of spandex request functions take a request map as parameter. The
`:url` key differs a bit from the original RING spec, it allows to
pass a raw string but also a sequence (potentially 2d) of encodable
things, keywords, .toString'able objects that make sense or nil (which
could be caused by a missing :url key).

``` clojure
(s/request c {:url [:foo :bar :_search] ...})
(s/request c {:url [:foo [:bar :something "more"] :_search] ...})
(s/request c {:url :_search ...})
(s/request c {:url "/index/_search" ...})
(s/request c {:url (java.util.UUID/randomUUID) ...})
(s/request c {...}) ;; defaults to "/"
```
### Blocking requests

```clojure
(s/request c {:url [:entries :entry :_search]
              :method :get
              :body {:query {:match_all {}}}})

>> {:body {:_index "entries", :_type "entry", :_id "AVkDDJvdkd2OsNWu4oYk", :_version 1, :_shards {:total 2, :successful 1, :failed 0}, :created true}, :status 201, :headers {"Content-Type" "application/json; charset=UTF-8", "Content-Length" "141"}, :host #object[org.apache.http.HttpHost 0x62b90fad "http://127.0.0.1:9200"]}
```

### Async requests (callbacks)

```clojure
(s/request-async c {:url "/urls/url/"
                    :method :get
                    :body {:query {:match {:message "this is a test"}}}
                    :success (fn [response-as-clj] ... )
                    :error (fn [ex] :boom)})
```

### Async requests: `core.async/promise-chan`

``` clojure
(async/<!! (s/request-chan c {:url "/urls/url/"
                              :method :get
                              :body {:query {:match {:message "this is a test"}}}}))
```

### Scrolling

Scrolling via core.async (fully NIO internally), interruptible if you
async/close! the returned chan.

``` clojure
(async/go
  (let [ch (s/scroll-chan client {:url "/foo/_search" :body {:query {:match_all {}}}})]
    (loop []
      (when-let [page (async/<! ch)]
        (do-something-with-page page)
        (recur)))))
```

### Bulk requests scheduling

"Faux streaming" of _bulk requests (flushes bulk request after
configurable interval or threshold. Uses request-chan internally, so
it's quite cheap.

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

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/cc.qbits/spandex.svg)](https://clojars.org/cc.qbits/spandex)

## API Docs

[![cljdoc badge](https://cljdoc.xyz/badge/cc.qbits/spandex)](https://cljdoc.xyz/d/cc.qbits/spandex/CURRENT)

Or the [clj.specs](https://github.com/mpenet/spandex/blob/master/src/clj/qbits/spandex/spec.clj) if that's your thing:

## Patreon

If you wish to support the work on this project you can do this via my
[patreon](https://www.patreon.com/mpenet) page.

## License

Copyright © 2018 [Max Penet](http://twitter.com/mpenet)

Distributed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html),
the same as Clojure.
