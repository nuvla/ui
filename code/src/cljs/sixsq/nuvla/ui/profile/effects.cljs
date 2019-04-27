(ns sixsq.nuvla.ui.profile.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.apps.utils-detail :as utils-detail]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-fx
  ::get-credentials
  (fn [[client callback]]
    (go
      (let [credentials (-> (<! (api/get client "credential")) :resources)]
        (callback credentials)))))

