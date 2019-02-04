(ns sixsq.slipstream.webui.dashboard.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<! timeout]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.client.api.runs :as runs]
    [sixsq.slipstream.webui.utils.general :as general-utils]))

(reg-fx
  ::get-virtual-machines
  (fn [[client params]]
    (go
      (let [virtual-machines (<! (cimi/search client "virtualMachines" (general-utils/prepare-params params)))]
        (dispatch [:sixsq.slipstream.webui.dashboard.events/set-virtual-machines virtual-machines])))))

(reg-fx
  ::get-deployments
  (fn [[client params]]
    (go
      (let [response (<! (runs/search-runs client params))
            item (get-in response [:runs :item] [])         ; workaround gson issue, when one element in item it give element instead of a vector of element
            deployments-list (if (= (type item) cljs.core/PersistentVector) item [item])
            deployments-uuid (map #(str "deployment/href=\"run/" (:uuid %) "\"") deployments-list)
            filter-str (str/join " or " deployments-uuid)
            vm-aggregation-params (general-utils/prepare-params
                                    {:$last        0
                                     :$aggregation "terms:deployment/href"
                                     :$filter      filter-str})
            active-vms (if (not-empty deployments-list)
                         (<! (cimi/search client "virtualMachines" vm-aggregation-params)) {})
            active-vms-per-deployment (->> (get-in active-vms [:aggregations :terms:deployment/href :buckets] [])
                                           (map #(vector (keyword (:key %)) (:doc_count %)))
                                           (into {}))
            deployments-with-vms (->> deployments-list
                                      (map #(assoc % :activeVm
                                                     (get active-vms-per-deployment
                                                          (keyword (str "run/" (:uuid %))) 0)))
                                      (assoc-in response [:runs :item]))]
        (dispatch [:sixsq.slipstream.webui.dashboard.events/set-deployments deployments-with-vms])))))

(reg-fx
  ::pop-deleted-deployment
  (fn [[uuid]]
    (go (<! (timeout 30000))
        (dispatch [:sixsq.slipstream.webui.dashboard.events/pop-deleted-deployment uuid]))))

(reg-fx
  ::delete-deployment
  (fn [[client uuid]]
    (go
      (let [result (<! (runs/terminate-run client uuid))
            error (when (instance? js/Error result)
                    (:error (js->clj
                              (->> result ex-data :body (.parse js/JSON))
                              :keywordize-keys true)))]
        (if error
          (dispatch [:sixsq.slipstream.webui.dashboard.events/set-error-message-deployment error])
          (dispatch [:sixsq.slipstream.webui.dashboard.events/deleted-deployment uuid]))))))
