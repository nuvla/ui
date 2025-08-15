(ns sixsq.nuvla.ui.pages.callback.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as main-components]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(defn CallbackView
  [{{:keys [callback-url]} :query-params :as f}]
  (let [tr @(subscribe [::i18n-subs/tr])]
    [:div {:style {:display         :flex
                   :justify-content :center}}
     [ui/Button {:size                     "large"
                 :primary                  true
                 :href                     callback-url
                 :data-reitit-handle-click "false"}
      [:b (tr [:please-click-to-proceed])]]]))
