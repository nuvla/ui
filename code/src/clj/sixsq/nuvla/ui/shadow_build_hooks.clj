(ns sixsq.nuvla.ui.shadow-build-hooks
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [shadow.build :as build]))

(defn copy-assets
  {:shadow.build/stage :configure}
  [build-state sources-targets]
  (doseq [[source-path target-path] sources-targets]
    (sh/sh "cp" "-r" source-path target-path))
  build-state)

(defn- git-commit-short-id
  []
  (str/trim-newline (:out (sh/sh "git" "rev-parse" "--short" "HEAD"))))

(defn generate-version
  {:shadow.build/stage :configure}
  [{::build/keys [config] :as build-state} target]
  (io/make-parents target)
  (let [release-version (:release-version config)
        snapshot?       (str/ends-with? release-version "SNAPSHOT")
        version         (if snapshot?
                          (str release-version "-" (git-commit-short-id))
                          release-version)]
    (spit (io/file target) version))
  build-state)
