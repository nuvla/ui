(ns sixsq.nuvla.ui.resource-log.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.resource-log.events :as events]
            [sixsq.nuvla.ui.resource-log.subs :as subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn LogController
  [_go-live? _current-log _select-component?]
  (let [tr               (subscribe [::i18n-subs/tr])
        locale           (subscribe [::i18n-subs/locale])
        since            (subscribe [::subs/since])
        avail-components (subscribe [::subs/available-components])
        components       (subscribe [::subs/components])
        play?            (subscribe [::subs/play?])]
    (fn [go-live? current-log select-component?]
      [ui/Menu {:size      "small"
                :attached  "top"
                :stackable true}
       [ui/MenuItem
        {:aria-label (if @play? "pause" "play")
         :disabled   (and select-component? (empty? @components))
         :on-click   #(dispatch [::events/set-play? (not @play?)])}
        [ui/Icon {:class (if @play? icons/i-pause icons/i-play)}]]

       (when (pos? (count @avail-components))
         [ui/Dropdown
          {:style       (when (seq @components)
                          {:flex-direction  :column
                           :justify-content :space-around})
           :placeholder (@tr [(if select-component?
                                :select-components
                                :all-components)])
           :icon        (if (empty? @components) "caret down" nil)
           :item        true
           :multiple    true
           :on-change   (ui-callback/value
                          #(dispatch [::events/set-components %]))
           :options     (map (fn [service]
                               {:key service, :text service, :value service})
                             @avail-components)}])

       [ui/MenuItem
        [:div {:style {:display      "flex"
                       :gap          "10px"
                       :align-items  "center"
                       :padding-left "1rem"}}
         [:div (@tr [:since])]
         [:div
          [ui/DatePicker
           {:custom-input     (r/as-element
                                [ui/Input {:transparent true
                                           :style       {:width "17em"}}])
            :locale           (or (time/locale-string->locale-object @locale) @locale)
            :date-format      "MMMM d, yyyy hh:mm aa"
            :time-format      "HH:mm"
            :show-time-select true
            :timeIntervals    1
            :selected         @since
            :on-change        #(dispatch [::events/set-since %])}]]]]

       [ui/MenuMenu {:position "right"}

        [ui/MenuItem
         {:active   @go-live?
          :color    (if @go-live? "green" "black")
          :on-click #(swap! go-live? not)}
         [ui/IconGroup {:size "large"}
          [icons/BarsIcon]
          [ui/Icon {:name "chevron circle down", :corner true}]]
         (@tr [:go-live])]

        [ui/MenuItem {:on-click #(dispatch [::events/clear current-log])}
         [ui/IconGroup {:size "large"}
          [icons/BarsIcon]
          [icons/TrashIconFull {:corner true}]]
         (str/capitalize (@tr [:clear]))]]])))

(defn LogsArea
  [_log _go-live?]
  (let [tr           (subscribe [::i18n-subs/tr])
        first-render (atom true)
        scroll-down  (fn [^js view-update]
                       (when (or @first-render (.-docChanged view-update))
                         (reset! first-render false)
                         (let [scroll-dom    (-> view-update .-view .-scrollDOM)
                               scroll-height (.-scrollHeight scroll-dom)]
                           (set! (.-scrollTop scroll-dom) scroll-height))))]
    (fn [log go-live?]
      [:<>
       [ui/Segment {:attached true
                    :style    {:padding 0
                               :z-index 0
                               :height  600}}
        [uix/EditorCode
         {:value     (str/join "\n" log)
          :height    "600px"
          :read-only true
          :on-update #(when (or go-live? @first-render) (scroll-down %))}]]
       [ui/Label (@tr [:row-count])
        [ui/LabelDetail (count log)]]])))

(defn LogsViewer
  [parent components-subs _select-component?]
  (let [tr           (subscribe [::i18n-subs/tr])
        resource-log (subscribe [::subs/resource-log])
        id           (subscribe [::subs/id])
        play?        (subscribe [::subs/play?])
        components   (components-subs)
        go-live?     (r/atom true)]
    (dispatch [::events/set-parent parent])
    (fn [_parent _components-subs select-component?]
      (dispatch [::events/set-available-components @components])
      (let [log            (:log @resource-log)
            log-components (:components @resource-log)
            last-timestamp (:last-timestamp @resource-log)]
        [:div
         [LogController go-live? log select-component?]
         [:<>
          [ui/Segment {:attached    "bottom"
                       :loading     (and (nil? last-timestamp)
                                         @play?)
                       :placeholder true
                       :style       {:padding 0
                                     :z-index 0}}
           (if (and @id last-timestamp)
             (if (empty? log-components)
               [LogsArea (:_all-in-one log) @go-live?]
               [ui/Tab
                {:menu  {:tabular  false
                         :pointing true
                         :attached "top"}
                 :panes (map
                          (fn [[component-name component-log]]
                            {:menuItem {:content (name component-name)
                                        :key     (name component-name)}
                             :render   #(r/as-element
                                          [LogsArea component-log @go-live?])}) log)}])
             [ui/Header {:icon true}
              [ui/Icon {:name "search"}]
              (@tr [:get-logs])])]]]))))

(defn TabLogs
  [_parent _components-subs]
  (r/create-class
    {:display-name           "TabLogs"
     :component-will-unmount #(dispatch [::events/reset])
     :reagent-render         LogsViewer}))
