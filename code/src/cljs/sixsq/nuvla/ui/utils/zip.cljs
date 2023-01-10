(ns sixsq.nuvla.ui.utils.zip
  (:require ["jszip" :as jszip]))

(defn create
  [files callback]
  (let [zip (jszip)]
    (doseq [{:keys [name file]} files]
      (.file zip name file))
    (-> zip
        (.generateAsync #js{"type" "base64", "compression" "STORE"})
        (.then #(callback (str "data:application/zip;base64," %))))))
