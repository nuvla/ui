(ns sixsq.nuvla.ui.infra-service.views
  (:require
    [reagent.core :as reagent]
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.panel :as panel]
    [taoensso.timbre :as timbre]))


(defn infra-services []
  [:div "hello services"])


(defmethod panel/render :infra-service
  [path]
  (timbre/set-level! :info)
  [infra-services])
