(ns sixsq.nuvla.ui.utils.form-fields
  (:require
    [re-frame.core :refer [subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(def nbsp "\u00a0")


(defn help-popup [description]
  (when description
    (let [icon [ui/Icon {:name "help circle"}]]
      [ui/Popup
       {:trigger        (reagent/as-element icon)
        :content        description
        :on             "hover"
        :hide-on-scroll true}])))


(defmulti form-field
          (fn [update-fn form-id {:keys [type] :as param}]
            (keyword type)))


(defmethod form-field :default
  [update-fn form-id {:keys [name display-name help hidden sensitive value-scope
                             required editable] :as attribute}]
  (let [{:keys [values value default]} value-scope
        label         (or display-name name)
        default-value (or value default "")
        read-only     (not editable)
        on-change-fn  (ui-callback/value #(update-fn form-id name %))]
    ^{:key name}
    [ui/FormField {:required required}
     (when-not hidden [:label label nbsp (help-popup help)])
     (cond
       values [ui/Dropdown {:selection     true
                            :search        true
                            :clearable     true
                            :disabled      read-only
                            :default-value default-value
                            :on-change     on-change-fn
                            :options       (map (fn [v] {:key v, :value v, :text v}) values)}]
       :else [ui/Input
              (cond-> {:type          (if sensitive "password" "text")
                       :name          name
                       :default-value default-value
                       :read-only     read-only
                       :on-change     on-change-fn}
                      hidden (assoc :style {:display "none"}))])]))


(defmethod form-field :integer
  [update-fn form-id {:keys [name display-name help hidden value-scope
                             required editable] :as attribute}]
  (let [label (or display-name name)]
    ^{:key name}
    [ui/FormField {:required required}
     (when-not hidden [:label label nbsp (help-popup help)])
     [ui/Input
      (cond-> {:type          "number"
               :name          name
               :default-value (or (:value value-scope) (:default value-scope) "")
               :read-only     (not editable)
               :on-change     (ui-callback/value #(update-fn form-id name (utils/str->int %)))}
              hidden (assoc :style {:display "none"}))]]))

(defn date-time-form
  [update-fn form-id {:keys [value-scope] :as attribute}]
  (let [locale        (subscribe [::i18n-subs/locale])
        {:keys [value default]} value-scope
        default-value (or value default)
        date-atom     (reagent/atom (when default-value (time/parse-iso8601 default-value)))]
    (fn [update-fn form-id {:keys [name display-name help hidden required editable] :as attribute}]
      (let [label     (or display-name name)
            read-only (not editable)]
        ^{:key name}
        [ui/FormField {:required required}
         (when-not hidden [:label label nbsp (help-popup help)])
         [ui/DatePicker (cond-> {:custom-input     (reagent/as-element [ui/Input {:style {:width "250px"}}])
                                 :show-time-select true
                                 :read-only        read-only
                                 :locale           @locale
                                 :date-format      "d MMMM YYYY, hh:mm a"
                                 :on-change        (fn [date]
                                                     (reset! date-atom date)
                                                     (update-fn form-id name date))}
                                @date-atom (assoc :selected @date-atom))]]))))


(defmethod form-field :date-time
  [update-fn form-id {:keys [name] :as attribute}]
  ^{:key name}
  [date-time-form update-fn form-id attribute])


(defmethod form-field :boolean
  [update-fn form-id {:keys [name display-name help hidden value-scope
                             required editable] :as attribute}]
  (let [label (or display-name name)]
    ^{:key name}
    [ui/FormField {:required required}
     (when-not hidden [:label label nbsp (help-popup help)])
     [ui/Checkbox
      (cond-> {:name          name
               :default-value (or (:value value-scope) (:default value-scope) false)
               :read-only     (not editable)
               :on-change     (ui-callback/checked #(update-fn form-id name %))}
              hidden (assoc :style {:display "none"}))]]))


(defmethod form-field :resource-id
  [update-fn form-id {:keys [name display-name help hidden value-scope
                             required editable] :as attribute}]
  (let [label (or display-name name)]
    ^{:key name}
    [ui/FormField {:required required}
     (when-not hidden [:label label nbsp (help-popup help)])
     [ui/Checkbox
      (cond-> {:name          name
               :default-value (or (:value value-scope) (:default value-scope) "")
               :read-only     (not editable)
               :on-change     (ui-callback/value #(update-fn form-id name {:href %}))}
              hidden (assoc :style {:display "none"}))]]))
