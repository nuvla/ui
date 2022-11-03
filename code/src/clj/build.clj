(ns build
  (:require
    [shadow.css.build :as cb]
    [clojure.java.io :as io]))


(defn css-release [& args]
  (let [build-state
        (-> (cb/start)
            (cb/index-path (io/file "src") {})
            (cb/generate
             '{:ui
               {:include
                [my.app*]}})
            (cb/write-outputs-to (io/file "resources" "public" "ui" "css")))]

    (doseq [mod (:outputs build-state)
            {:keys [warning-type] :as warning} (:warnings mod)]

      (prn [:CSS (name warning-type) (dissoc warning :warning-type)]))))
