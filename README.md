# spandex

Elasticsearch (fast) [rest client](https://www.elastic.co/blog/benchmarking-rest-client-transport-client) wrapper (wip)

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

## License

Copyright © 2016 [Max Penet](http://twitter.com/mpenet)

Distributed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html),
the same as Clojure.
