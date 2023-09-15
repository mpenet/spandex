(ns qbits.spandex.test-runner
  (:gen-class)
  (:require [clojure.test]
            eftest.report.pretty
            [eftest.runner :as ef]))

(def default-options
  {:dir "test"
   :capture-output? true
   :fail-fast? true
   :multithread? false
   :reporters [eftest.report.pretty/report]})

(defn- ret->exit-code
  [{:as _ret :keys [error fail]}]
  (System/exit
   (cond
     (and (pos? fail) (pos? error)) 30
     (pos? fail) 20
     (pos? error) 10
     :else 0)))

(defn combined-reporter
  "Combines the reporters by running first one directly,
  and others with clojure.test/*report-counters* bound to nil."
  [[report & rst]]
  (fn [m]
    (report m)
    (doseq [report rst]
      (binding [clojure.test/*report-counters* nil]
        (report m)))))

(defn run
  [options]
  (let [options (merge default-options options)]
    (-> (ef/find-tests (:dir options))
        (ef/run-tests options)
        ret->exit-code)))
