(def es-client-version "5.4.2")
(defproject cc.qbits/spandex "0.5.2-SNAPSHOT"
  :description "Clojure Wrapper of the new/official ElasticSearch REST client"
  :aot :all
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;;[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/core.async "0.3.443"]
                 [org.elasticsearch.client/rest ~es-client-version]
;;                 [org.elasticsearch.client/sniffer ~es-client-version]
                 [cc.qbits/commons "0.4.6"]
                 [cheshire "5.7.1"]]
  :source-paths ["src/clj"]
  :global-vars {*warn-on-reflection* true}
  ;; :codox {:source-uri "https://github.com/mpenet/spandex/blob/master/{filepath}#L{line}"
  ;;         :metadata {:doc/format :markdown}
  ;;         :output-path "docs"
  ;;         :doc-files ["docs/quickstart.md"]
  ;;         :source-paths ["src/clj"]}
  :profiles {:dev {:plugins [[lein-cljfmt "0.5.6"]
                             [codox "0.10.3"]]}}

  :plugins [[lein-modules "0.3.9"]
            [s3-wagon-private "1.1.2"]]
  :repositories [["releases"
                  {:url "s3p://shareablee-jar-repo/releases"
                   :username :env/shareablee_aws_access_key
                   :passphrase :env/shareablee_aws_secret_access_key
                   :sign-releases false}]
                 ["snapshots"
                  {:url "s3p://shareablee-jar-repo/snapshots"
                   :username :env/shareablee_aws_access_key
                   :passphrase :env/shareablee_aws_secret_access_key}]])
