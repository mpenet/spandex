# Changelog

## 0.6.4

* bump dependencies

* kill deprecated request decompression param

## 0.6.3 - Breaking change

* fix bug in scroll-chan where request-chan and scroll chan output
  chan would be mixed up if the user passed a :ch option.  scroll-chan
  output chan is now called `:ouput-ch`, same as `bulk-chan`.


## 0.6.2

* bump ES dep to 6.2.1
* make response consumer factory configurable in sync requests and
  refactor related code a bit

## 0.6.1

* bump qbits.commons

## 0.6.0

* bump ES dep to 6.2.1
* bump clojure to 1.9 final
* bump core.async to latest

## 0.5.5

* properly url-encode url fragments when passing url as data
* update deps: core.async, clojure, es rest client, cheshire

## 0.5.4

* properly decompress body when returned as gzipped content. thx to
  @ahjones for the report and first patch

## 0.5.3

* bump ES dep to 5.5.0

## 0.5.2

* Add chan->seq utility function

* bump ES dep to 5.5.0

## 0.5.1

* fix bug with scroll_id parameter with ES 5.4 (they removed a
  backward compat with raw param in body, body is assumed to be JSON.

## 0.5.0

* `:url` key in request-maps is not protocolized, as a result all
  request functions now accept the request `:url` key as String or
  Keyword or Sequential (potentially 2d), the same way
  `qbits.spandex.utils/url` used to work in that case.

  you can now write:
  `(request client {:url "/foo" ...})`
  `(request client {:url :foo ...})`
  `(request client {:url [:foo :bar "baz"] ...})`
  `(request client {:url [:foo [:bar :baz "prout"]] ...})`

* depreciated `qbits.spandex.utils/url`. It now lives under
  `qbits.spandex.url/encode`, there's an alias so it should be
  backward compatible.

* bump cheshire and elasticsearch client dependency

## 0.4.2

* specify utf-8 charset in content-type header

## 0.4.1

* bump clj + core.async deps

## 0.4.0

* proper content-type defaults to prevent warnings with ES 5.3+

* request-chan now accepts a `:ch` option which defaults to the
  original `promise-chan` returned by the function.

## 0.3.11

* Swap use of Throwable to Exception (thx @axel-angel)

## 0.3.10

* bulk-chan: if the user closes :input-ch to signal the need to end
  the process all pending messages will be processed and upon
  completion :output-ch will be closed (it wasn't the case before)

## 0.3.9

* Bump clj to 1.9a16

## 0.3.8

* Bump ES client dependency to 5.3.1
* Prefix _bulk endpoint with leading slash

## 0.3.7

* Bump ES client dependency to 5.3.0
* Bump clj to 1.9a15
* Bump core.async to 0.3.442

## 0.3.5

* Bump dependencies (cheshire, codox)

* make `scroll-chan` 2.x- compatible

## 0.3.5

* Bump ES client dependency to 5.2.2

## 0.3.4

* Bump core.async dependency to 0.3.441

## 0.3.3

* Bump core.async dependency to 0.3.436

## 0.3.2

* Bump core.async dependency to 0.3.426

## 0.3.1

* Bump ES client dependency to 5.2.1

## 0.3.0

ResponseExceptions are now returned as ex-info instances wrapping the
real response decoded as clojure map.

That means you can just call ex-data on it and get to it that way

```clojure
 (try (s/request client {:url "/a/a/a/a/a"})
    (catch clojure.lang.ExceptionInfo ex
       (prn (ex-data ex))))

=> #qbits.spandex.Response{
    :type :qbits.spandex/response-exception
    :body "No handler found for uri [/a/a/a/a/a] and method [GET]"
    :status 400
    :headers {"Content-Type" "text/plain; charset=UTF-8", "Content-Length" "54"}
    :hosts #object[org.apache.http.HttpHost 0x968acb8 "http://localhost:9200"]}

```

You can overwrite this behavior in a few ways:

* the default `:exception-handler` (aka `default-exception-handler`)
  calls `ExceptionDecoder/decode-exception` which is a protocol based
  function, when the result is a throwable it will rethrow otherwise
  it returns the value. That means you can just extend it if you
  prefer to have `qbits.spandex.Response` as a value. Doing so will
  also be valid for async request triggering functions (minus the
  throwing).

  something like the following
  ```clojure
  (extend-protocol ExceptionDecoder
  ResponseException
  (decode-exception [x] (response-ex->response x)))
  ```

* Or you can pass your own `:exception-handler` to all request triggering
  functions to do whatever you'd like and bypass the protocol altogether.


## 0.2.8

* Bump es client dependencies

## 0.2.7

* better spec coverage for client options

## 0.2.6

* qbits.spandex.utils/url now generates urls with a leading slash

## 0.2.5

* extended the http-client/request options, see [qbits.spandex/client](https://mpenet.github.io/spandex/qbits.spandex.html#var-client)
* removed *-callback options (they are superseeded by the above)
