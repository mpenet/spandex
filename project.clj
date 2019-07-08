(def es-client-version "7.2.0")
(defproject cc.qbits/spandex "0.7.0"
  :description "Clojure Wrapper of the new/official ElasticSearch REST client"
  :url "https://github.com/mpenet/spandex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 [org.elasticsearch.client/elasticsearch-rest-client ~es-client-version]
                 [org.elasticsearch.client/elasticsearch-rest-client-sniffer ~es-client-version
                  :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [cc.qbits/commons "0.5.2"]
                 [cheshire "5.8.1"]
                 [ring/ring-codec "1.1.1"]]
  :source-paths ["src/clj"]
  :global-vars {*warn-on-reflection* true}
  :pedantic? :warn
  :profiles {:dev {:plugins [[lein-cljfmt "0.6.1"
                              :exclusions [org.clojure/clojurescript]]]}})
