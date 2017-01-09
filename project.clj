(defproject cc.qbits/spandex "0.1.8"
  :description "Clojure Wrapper of the new/official ElasticSearch REST client"
  :url "https://github.com/mpenet/spandex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/core.async "0.2.395"]
                 [org.elasticsearch.client/rest "5.1.1"]
                 [org.elasticsearch.client/sniffer "5.1.1"]
                 [cc.qbits/commons "0.4.6"]
                 [cheshire "5.6.3"]]
  :source-paths ["src/clj"]
  :global-vars {*warn-on-reflection* true}
  :codox {:source-uri "https://github.com/mpenet/spandex/blob/master/{filepath}#L{line}"
          :metadata {:doc/format :markdown}
          :output-path "docs"
          :source-paths ["src/clj"]}
  :profiles {:dev {:plugins [[lein-cljfmt "0.5.6"]
                             [codox "0.10.2"]]}})
