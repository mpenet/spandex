# qbits.spandex 





## `->request`
``` clojure

(->request
 {:as request-map,
  :keys [url method query-string headers body response-consumer-factory request-options],
  :or {method :get, headers default-headers}})
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L259-L289)</sub>
## `BodyEncoder`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L148-L149)</sub>
## `Closable`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L133-L134)</sub>
## `ExceptionDecoder`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L228-L229)</sub>
## `bulk-chan`

Bulk-chan takes a client, a partial request/option map, returns a
  map of `:input-ch`, `:output-ch` and `:flush-ch`.

  `:input-ch` is a channel that will accept bulk fragments to be
  sent (either single or collection). It then will
  wait (:flush-interval request-map) or (:flush-threshold request-map)
  and then trigger an async request with the bulk payload accumulated.

  Parallelism of the async requests is controllable
  via (:max-concurrent-requests request-map).

  If the number of triggered requests exceeds the capacity of the job
  buffer, puts! in `:input-ch` will block (if done with async/put! you
  can check the return value before overflowing the put! pending
  queue).

  Jobs results returned from the processing are a pair of job and
  responses map, or exception.  The :output-ch will allow you to
  inspect [job responses] the server returned and handle potential
  errors/failures accordingly (retrying etc).

  If you close! the `:input-ch` it will close the underlying resources
  and exit cleanly (consuming all jobs that remain in queues).

  By default requests are run against _bulk, but the option map is
  passed as is to request-chan, you can overwrite options here and
  provide your own url, headers and so on.

  `:flush-ch` is provided to afford an immediate flush of the contents
  of the job buffer by putting anything onto this channel.
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L417-L522)</sub>
## `chunks->body`
``` clojure

(chunks->body chunks)
```


Utility function to create _bulk/_msearch bodies. It takes a
  sequence of clj fragments and returns a newline delimited string of
  JSON fragments
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L406-L415)</sub>
## `client`
``` clojure

(client)
(client options)
```


Returns a client instance to be used to perform requests.

  Options:

  * `:hosts` : Collection of URIs of nodes - Defaults to ["http://localhost:9200"]

  * `:default-headers` : Sets the default request headers, which will be
  sent along with each request. Request-time headers will always
  overwrite any default headers.

  * `:failure-listener` : Sets the RestClient.FailureListener to be
  notified for each request failure

  * `:request` : request scoped config - extendable via the `qbits.client-options/set-request-option!` multimethod)
      *  `:authentication?`
      *  `:circular-redirect-allowed?`
      *  `:connect-timeout`
      *  `:connection-request-timeout`
      *  `:content-compression?`
      *  `:expect-continue?`
      *  `:local-address`
      *  `:max-redirects`
      *  `:proxy`
      *  `:redirects?`
      *  `:relative-redirects-allowed?`
      *  `:socket-timeout`
      *  `:target-preferred-auth-schemes`

  * `:http-client` : http-client scoped config - extendable via the `qbits.client-options/set-http-client-option!` multimethod)
      *  `:max-conn-per-route`
      *  `:max-conn-total`
      *  `:proxy`
      *  `:ssl-context`
      *  `:ssl-noop-hostname-verifier?`
      *  `:user-agent`
      *  `:auth-caching?`
      *  `:cookie-management?`
      *  `:basic-auth` (map of `:user` `:password`)

  If you need extra/custom building you can hook into the builder by
  extending the multimethod
  `qbits.spandex.client-options/set-option!`
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L40-L86)</sub>
## `close!`
``` clojure

(close! this)
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L133-L134)</sub>
## `decode-exception`
``` clojure

(decode-exception x)
```


Controls how to translate a client exception
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L228-L229)</sub>
## `default-exception-handler`
``` clojure

(default-exception-handler ex)
```


Exception handler that will try to decode Exceptions via
  ExceptionDecoder/decode-exception. If after decoding we still have a
  throwable it will rethrow, otherwise it will pass on the value
  returned. You can then extend ExceptionDecoder/decode-exception to
  do whatever you'd like.
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L237-L247)</sub>
## `default-headers`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L168-L168)</sub>
## `encode-body`
``` clojure

(encode-body x)
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L148-L149)</sub>
## `node-selector`
``` clojure

(node-selector f)
```


Creates a NodeSelector instance that will call `f` as select():
  see: https://github.com/elastic/elasticsearch/blob/master/client/rest/src/main/java/org/elasticsearch/client/NodeSelector.java#L29
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L125-L131)</sub>
## `raw`
``` clojure

(raw body)
```


Marks body as raw, which allows to skip JSON encoding
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L143-L146)</sub>
## `request`
``` clojure

(request
 client
 {:keys [method url headers query-string body keywordize? response-consumer-factory exception-handler],
  :or
  {method :get,
   keywordize? true,
   exception-handler default-exception-handler,
   response-consumer-factory HttpAsyncResponseConsumerFactory/DEFAULT},
  :as request-params})
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L291-L306)</sub>
## `request-async`
``` clojure

(request-async
 client
 {:keys [method url headers query-string body success error keywordize? response-consumer-factory exception-handler],
  :or
  {method :get,
   keywordize? true,
   exception-handler decode-exception,
   response-consumer-factory HttpAsyncResponseConsumerFactory/DEFAULT},
  :as request-params})
```


Similar to `qbits.spandex/request` but returns immediately and works
  asynchronously and triggers option `:success` once a results is
  received, or `:error` if it was a failure
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L308-L330)</sub>
## `request-chan`
``` clojure

(request-chan client {:as options, :keys [ch]})
```


Similar to `qbits.spandex/request` but runs asynchronously and
  returns a `core.async/promise-chan` that will have the result (or
  error) delivered upon reception
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L332-L348)</sub>
## `response-ex->ex-info`
``` clojure

(response-ex->ex-info re)
```


Utility function to transform an ResponseException into an ex-info
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L221-L226)</sub>
## `response-ex->response`
``` clojure

(response-ex->response re)
```


Return the response-map wrapped in a ResponseException
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L216-L219)</sub>
## `scroll-chan`
``` clojure

(scroll-chan client {:as request-map, :keys [ttl output-ch], :or {ttl "1m"}})
```


Returns a core async channel. Takes the same args as
  `qbits.spandex/request`. Perform async scrolling requests for a
  query, request will be done as the user takes from the
  channel. Every take!  will request/return a page from the
  scroll. You can specify scroll :ttl in the request map otherwise
  it'll default to 1m.  The chan will be closed once scroll is
  complete. If you must stop scrolling before that, you must
  async/close! manually, this will release all used resources. You can
  also supply a :output-ch key to the request map, a core.async/chan that
  will receive the results. This allow you to have custom buffers, or
  have multiple scroll-chan calls feed the same channel instance
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L350-L404)</sub>
## `set-sniff-on-failure!`
``` clojure

(set-sniff-on-failure! sniffer)
```


Register a SniffOnFailureListener that allows to perform sniffing
  on failure.
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L118-L123)</sub>
## `sniffer`
``` clojure

(sniffer client)
(sniffer
 client
 {:as options,
  :keys [scheme timeout],
  :or {scheme :http, timeout ElasticsearchNodesSniffer/DEFAULT_SNIFF_REQUEST_TIMEOUT}})
```


Takes a Client instance (and possible sniffer options) and returns
  a sniffer instance that will initially be bound to passed client.
  Options:

  * `:sniff-interval` : Sets the interval between consecutive ordinary
  sniff executions in milliseconds. Will be honoured when
  sniffOnFailure is disabled or when there are no failures between
  consecutive sniff executions.

  * `:sniff-after-failure-delay` : Sets the delay of a sniff execution
  scheduled after a failure (in milliseconds)


  If you need extra/custom building you can hook into the builder by
  extending the multimethod
  `qbits.spandex.sniffer-options/set-option!`
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex.clj#L90-L116)</sub>
# qbits.spandex.client-options 





## `builder`
``` clojure

(builder {:as options, :keys [hosts]})
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/client_options.clj#L209-L215)</sub>
## `set-http-client-option!`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/client_options.clj#L100-L100)</sub>
## `set-request-option!`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/client_options.clj#L41-L41)</sub>
## `ssl-context-trust-all`
``` clojure

(ssl-context-trust-all)
```


Return a SSLContext that trusts all certificate
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/client_options.clj#L30-L38)</sub>
# qbits.spandex.sniffer-options 





## `builder`
``` clojure

(builder client sniffer options)
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/sniffer_options.clj#L28-L32)</sub>
# qbits.spandex.url 





## `URL`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/url.clj#L8-L9)</sub>
## `URLFragment`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/url.clj#L11-L12)</sub>
## `encode`
``` clojure

(encode x)
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/url.clj#L8-L9)</sub>
## `encode-fragment`
``` clojure

(encode-fragment value)
```

<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/url.clj#L11-L12)</sub>
# qbits.spandex.utils 





## `chan->seq`
``` clojure

(chan->seq ch)
```


Convert a channel to a lazy sequence.

  Will block on after the last element if the channel is not closed.
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/utils.clj#L9-L15)</sub>
## `escape-query-string`
``` clojure

(escape-query-string query)
```


Escape or remove special characters in query string coming from users.

  See:
  https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters
<br><sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/utils.clj#L17-L29)</sub>
## `url`
<sub>[source](https://github.com/mpenet/spandex/blob/main/src/clj/qbits/spandex/utils.clj#L7-L7)</sub>
