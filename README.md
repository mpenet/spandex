# spandex

Elasticsearch new [rest-client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest.html) wrapper

## Why?

Because the native client is a bit or a nightmare to deal with (for
many reasons) and the new REST client is quite fast.

See ["Benchmarking REST client and transport client "](https://www.elastic.co/blog/benchmarking-rest-client-transport-client)

## Goals

* Be very minimal and performant

* RING inspired

* All "exotic" features should be optional

* Not a giant DSL over another DSL (ESes), just maps everywhere

* Provide minimal (and totally optional) utils to do the boring stuff
  (bulk, compose urls)

* Can do async via simple callbacks based api or `core.async`

* Provide clj.specs


**here be dragons**

``` clojure

(require '[qbits.spandex :as s])

(def c (s/client ["http://127.0.0.1:9200" "http://foo2:3838"] {... options ...}))

;; add optional sniffer
(def s (s/sniffer c {... options ...}))


;; blocking
(s/request c {:url "/entries/entry/_search"
              :method :get
              :body {:some {:fancy :query}}})

>> {:body {:_index "entries", :_type "entry", :_id "AVkDDJvdkd2OsNWu4oYk", :_version 1, :_shards {:total 2, :successful 1, :failed 0}, :created true}, :status 201, :headers {"Content-Type" "application/json; charset=UTF-8", "Content-Length" "141"}, :host #object[org.apache.http.HttpHost 0x62b90fad "http://127.0.0.1:9200"]}

;; async: callback based
(s/request-async c {:url "/urls/url/"
                    :method :get
                    :body {:some {:fancy :query}}
                    :success (fn [response-as-clj] ... )
                    :error (fn [ex] :boom)})


;; async: as a clj.core.async/promise-chan
(s/request-ch c {:url "/urls/url/"
                 :method :get
                 :body {:some {:fancy :query}}})

```

## API Docs

[Codox generated docs](https://mpenet.github.io/spandex/)

## License

Copyright © 2017 [Max Penet](http://twitter.com/mpenet)

Distributed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html),
the same as Clojure.
