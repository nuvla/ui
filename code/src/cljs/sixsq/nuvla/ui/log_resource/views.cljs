(ns sixsq.nuvla.ui.log-resource.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.log-resource.events :as events]
    [sixsq.nuvla.ui.log-resource.spec :as spec]
    [sixsq.nuvla.ui.log-resource.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn log-controller
  [_go-live? _current-log]
  (let [locale        (subscribe [::i18n-subs/locale])
        since         (subscribe [::subs/since])
        nb-status     (subscribe [::subs/nuvlabox-status])
        components    (subscribe [::subs/components])
        play?         (subscribe [::subs/play?])]
    (fn [go-live? current-log]
      (let [avail-components (:components @nb-status)]
        [ui/Menu {:size "small", :attached "top"}

         [ui/MenuItem
          {:on-click #(dispatch [::events/set-play? (not @play?)])}
          [ui/Icon {:name (if @play? "pause" "play")}]]

         (when (pos? (count avail-components))
           [ui/Dropdown
            {:placeholder (if (empty? @components) "All components" "")
             :item        true
             :multiple    true
             :on-change   (ui-callback/value #(dispatch [::events/set-components %]))
             :options     (map (fn [service]
                                 {:key service, :text service, :value service}) avail-components)}])

         [ui/MenuItem
          [:span
           "Since:  "
           [ui/DatePicker
            {:custom-input     (r/as-element
                                 [ui/Input {:transparent true
                                            :style       {:width "17em"}}])
             :locale           @locale
             :date-format      "LLL"
             :show-time-select true
             :timeIntervals    1
             :selected         @since
             :on-change        #(dispatch [::events/set-since %])}]]]

         [ui/MenuMenu {:position "right"}

          [ui/MenuItem
           {:active   @go-live?
            :color    (if @go-live? "green" "black")
            :on-click #(swap! go-live? not)}
           [ui/IconGroup {:size "large"}
            [ui/Icon {:name "bars"}]
            [ui/Icon {:name "chevron circle down", :corner true}]]
           "Go Live"]

          [ui/MenuItem {:on-click #(dispatch [::events/clear current-log])}
           [ui/IconGroup {:size "large"}
            [ui/Icon {:name "bars"}]
            [ui/Icon {:name "trash", :corner true}]]
           "Clear"]]]))))

(defn print-logs
  [log scroll-info go-live?]
  [:<>
   [ui/Segment {:attached    true
                :style       {:padding 0
                              :z-index 0
                              :height  600}}
    [ui/CodeMirror {:value    (str/join "\n" log)
                    :scroll   {:x (:left @scroll-info)
                               :y (if go-live?
                                    (.-MAX_VALUE js/Number)
                                    (:top @scroll-info))}
                    :onScroll #(reset! scroll-info
                                       (js->clj %2 :keywordize-keys true))
                    :options  {:mode     ""
                               :readOnly true
                               :theme    "logger"}
                    :class    ["large-height"]}]]
   [ui/Label (str "line count:")
    [ui/LabelDetail (count log)]]])


(defn logs-viewer
  []
  (let [resource-log   (subscribe [::subs/resource-log])
        id             (subscribe [::subs/id])
        play?          (subscribe [::subs/play?])
        go-live?       (r/atom true)
        scroll-info    (r/atom nil)]
    (fn []
      (let [log (:log @resource-log)
            log-components  (:components @resource-log)
            last-timestamp  (:last-timestamp @resource-log)]
        [:div
         [log-controller go-live? log]
         [:<>
          ^{:key (str "logger" @go-live?)}
          [ui/Segment {:attached    "bottom"
                       :loading     (and (nil? last-timestamp)
                                         @play?)
                       :placeholder true
                       :style       {:padding 0
                                     :z-index 0}}
           (if (and @id last-timestamp)
             (if (empty? log-components)
               (print-logs (:_all-in-one log) scroll-info @go-live?)
               [ui/Tab {:menu     {:tabular false
                                   :pointing true
                                   :attached "top"}
                        :panes    (map
                                    (fn [[component-name component-log]]
                                      {:menuItem {:content (name component-name)
                                                  :key     (name component-name)}
                                       :render (fn []
                                                 (r/as-element
                                                   (print-logs component-log scroll-info @go-live?)))}) log)}])
             [ui/Header {:icon true}
              [ui/Icon {:name "search"}]
              "Get NuvlaBox logs"])]]]))))


(defn TabLogs
  []
  (r/create-class
    {:component-will-unmount #(do
                                (dispatch [::events/delete])
                                (dispatch [::events/set-since (spec/default-since)]))
     :reagent-render         logs-viewer}))
