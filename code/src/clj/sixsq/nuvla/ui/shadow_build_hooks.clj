(ns sixsq.nuvla.ui.shadow-build-hooks
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [shadow.build :as build]))

(defn copy-assets
  {:shadow.build/stage :configure}
  [build-state sources-targets]
  (doseq [[source-path target-path] sources-targets]
    (sh/sh "cp" "-r" source-path target-path))
  build-state)

(defn generate-version-css
  {:shadow.build/stage :configure}
  [{::build/keys [config] :as build-state} target]
  (io/make-parents target)
  (spit (io/file target)
        (str "#release-version:after {content: '"
             (:release-version config)
             "';}\n"))
  build-state)
