(ns sixsq.slipstream.webui.dnd.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.dnd.utils :as dnd-utils]

    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.utils.general :as general]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [taoensso.timbre :as log]))


(defn file-stats
  [file]
  (log/error "FILE:"
             (.-name file)
             (.-size file)
             (.-type file)
             #_(js/window.URL.createObjectURL file)))


(defn file-select
  [file-handler]
  (let [tr (subscribe [::i18n-subs/tr])
        id (str "file-select-" (general/random-element-id))
        on-change #(some-> (.getElementById js/document id)
                           .-files
                           (aget 0)
                           file-handler)]
    (fn [file-handler]
      [:span
       ^{:key (str (str id "-button"))}
       [uix/Button {:text     (@tr [:select-file])
                    :on-click #(some-> (.getElementById js/document id)
                                       .click)}]
       ^{:key id}
       [:input {:id        id
                :hidden    true
                :type      "file"
                :label     "select file"
                :on-change on-change}]])))


(defn drop-zone
  [file-handler]
  (let [tr (subscribe [::i18n-subs/tr])
        inside? (reagent/atom false)]
    (fn [file-handler]
      [uix/Button
       {:text          (@tr [:drop-file])
        :primary       @inside?

        :on-drag-enter (fn [evt _] (dnd-utils/prevent-default evt) (reset! inside? true))
        :on-drag-leave (fn [evt _] (dnd-utils/prevent-default evt) (reset! inside? false))
        :on-drag-over  (fn [evt _] (dnd-utils/prevent-default evt))
        :on-drop       (fn [evt _]
                         (dnd-utils/prevent-default evt)
                         (when-let [file (-> evt .-dataTransfer .-files (aget 0))]
                           (when file-handler
                             (file-handler file)))
                         (reset! inside? false))}])))
