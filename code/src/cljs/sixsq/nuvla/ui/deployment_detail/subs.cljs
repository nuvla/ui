(ns sixsq.nuvla.ui.deployment-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment-detail.spec :as spec]
    [clojure.string :as str]))


(reg-sub
  ::runUUID
  (fn [db]
    (::spec/runUUID db)))

(reg-sub
  ::reports
  ::spec/reports)

(reg-sub
  ::loading?
  ::spec/loading?)


(reg-sub
  ::deployment
  ::spec/deployment)


(reg-sub
  ::events
  (fn [db]
    (::spec/events db)))


(reg-sub
  ::jobs
  (fn [db]
    (::spec/jobs db)))


(reg-sub
  ::force-refresh-events-steps
  (fn [db]
    (::spec/force-refresh-events-steps db)))


(reg-sub
  ::deployment-parameters
  (fn [db]
    (::spec/deployment-parameters db)))



(reg-sub
  ::url
  :<- [::deployment-parameters]
  (fn [deployment-parameters [_ url-pattern]]
    (when url-pattern
      (let [pattern-in-params (re-seq #"\$\{([^}]+)\}+" url-pattern)
            pattern-value (map (fn [[param-pattern param-name]]
                                 (some->> (get deployment-parameters param-name)
                                          :value
                                          (conj [param-pattern])))
                               pattern-in-params)]
        (when (every? some? pattern-value)
          (reduce
            (fn [url [param-pattern param-value]]
              (str/replace url param-pattern param-value))
            url-pattern pattern-value))))))

(reg-sub
  ::node-parameters-modal
  (fn [db]
    (::spec/node-parameters-modal db)))


(reg-sub
  ::node-parameters
  ::spec/node-parameters)


(reg-sub
  ::summary-nodes-parameters
  (fn [db]
    (::spec/summary-nodes-parameters db)))
