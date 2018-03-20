(def es-client-version "6.2.2")
(defproject cc.qbits/spandex "0.6.2"
  :description "Clojure Wrapper of the new/official ElasticSearch REST client"
  :url "https://github.com/mpenet/spandex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.elasticsearch.client/elasticsearch-rest-client ~es-client-version]
                 [org.elasticsearch.client/elasticsearch-rest-client-sniffer ~es-client-version
                   :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [cc.qbits/commons "0.5.1"]
                 [cheshire "5.8.0"]
                 [ring/ring-codec "1.1.0"]]
  :source-paths ["src/clj"]
  :global-vars {*warn-on-reflection* true}
  :codox {:source-uri "https://github.com/mpenet/spandex/blob/master/{filepath}#L{line}"
          :metadata {:doc/format :markdown}
          :output-path "docs"
          :doc-files ["docs/quickstart.md"]
          :source-paths ["src/clj"]}
  :pedantic? :warn
  :profiles {:dev {:plugins [[lein-cljfmt "0.5.6"
                              :exclusions [org.clojure/clojurescript]]
                             [codox "0.10.3"]]}})
