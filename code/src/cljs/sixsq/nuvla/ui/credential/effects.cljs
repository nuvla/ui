(ns sixsq.nuvla.ui.credential.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-fx
  ::get-credentials
  (fn [[client callback]]
    (go
      (let [credentials (-> (<! (api/get client "credential")) :resources)]
        (callback credentials)))))
