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

(defn- git-rev-parse
  [ref]
  (let [short-id (str/trim-newline (:out (sh/sh "git" "rev-parse" "--short" ref)))]
    (when-not (str/blank? short-id)
      short-id)))

(defn- git-commit-short-id
  []
  (or
    (git-rev-parse (System/getenv "GITHUB_SHA"))
    (git-rev-parse "HEAD")
    (str "rand" (rand-int 99999))))

(defn generate-version
  {:shadow.build/stage :configure}
  [{::build/keys [config mode] :as build-state} target]
  (io/make-parents target)
  (let [release-version (:release-version config)
        snapshot?       (str/ends-with? release-version "SNAPSHOT")
        dev?            (= mode :dev)
        version         (if (and snapshot? (not dev?))
                          (str release-version "-" (git-commit-short-id))
                          release-version)]
    (prn "UI version:" version)
    (spit (io/file target) version))
  build-state)
