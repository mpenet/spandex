(defproject cc.qbits/spandex "0.1.0-SNAPSHOT"
  :description ""
  :url "https://github.com/mpenet/spandex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/core.async "0.2.395"]
                 [org.elasticsearch.client/rest "5.1.1"]
                 [org.elasticsearch.client/sniffer "5.1.1"]
                 [cc.qbits/commons "0.4.6"]]
  :codox {:src-dir-uri "https://github.com/mpenet/spandex/blob/master/"
          :src-linenum-anchor-prefix "L"
          :defaults {:doc/format :markdown}
          :output-dir "doc/codox"}

  :source-paths ["src/clj"]
  :global-vars {*warn-on-reflection* true})
