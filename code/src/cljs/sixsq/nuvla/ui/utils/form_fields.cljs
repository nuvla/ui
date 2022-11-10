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

(defn help-popup [description & {:keys [on] :or {on "hover"}}]
  (when description
    (let [icon [ui/Icon {:name "help circle"}]]
      [ui/Popup
       {:trigger        (reagent/as-element icon)
        :content        description
        :on             on
        :hide-on-scroll true}])))


(defmulti form-field
          (fn [_update-fn _form-id {:keys [type] :as _param}]
            (keyword type)))


(defmethod form-field :default
  [update-fn form-id {:keys [name display-name _help _hidden _sensitive value-scope
                             _required editable] :as _attribute}]
  (let [{:keys [values value default]} value-scope
        label         (or display-name name)
        default-value (or value default "")
        read-only     (not editable)
        on-change-fn  (ui-callback/value #(update-fn form-id name %))]
    (when default-value
      (update-fn form-id name default-value))
    (fn [_update-fn _form-id {:keys [name _display-name help hidden sensitive _value-scope
                                   required _editable] :as _attribute}]
      ^{:key name}
      [ui/FormField {:required required}
       (when-not hidden [:label label nbsp (help-popup help)])
       (if values
         [ui/Dropdown {:selection     true
                       :search        true
                       :clearable     true
                       :disabled      read-only
                       :default-value default-value
                       :on-change     on-change-fn
                       :options       (map (fn [v] {:key v, :value v, :text v}) values)}]
         [ui/Input
          (cond-> {:type          (if sensitive "password" "text")
                   :name          name
                   :default-value default-value
                   :read-only     read-only
                   :on-change     on-change-fn}
                  hidden (assoc :style {:display "none"}))])])))


(defmethod form-field :integer
  [update-fn form-id {:keys [name display-name _help _hidden value-scope
                             _required _editable] :as _attribute}]
  (let [label         (or display-name name)
        default-value (or (:value value-scope) (:default value-scope))]
    (when default-value
      (update-fn form-id name default-value))
    (fn [update-fn form-id {:keys [name _display-name help hidden _value-scope
                                   required editable] :as _attribute}]
      ^{:key name}
      [ui/FormField {:required required}
       (when-not hidden [:label label nbsp (help-popup help)])
       [ui/Input
        (cond-> {:type          "number"
                 :name          name
                 :default-value (or default-value "")
                 :read-only     (not editable)
                 :on-change     (ui-callback/value #(update-fn form-id name (utils/str->int %)))}
                hidden (assoc :style {:display "none"}))]])))

(defn date-time-form
  [_update-fn _form-id {:keys [value-scope] :as _attribute}]
  (let [locale        (subscribe [::i18n-subs/locale])
        {:keys [value default]} value-scope
        default-value (or value default)
        date-atom     (reagent/atom (when default-value
                                      (time/parse-iso8601 default-value)))]
    (fn [update-fn form-id {:keys [name display-name help hidden required editable] :as _attribute}]

      (let [label     (or display-name name)
            read-only (not editable)]
        ^{:key name}
        [ui/FormField {:required required}
         (when-not hidden [:label label nbsp (help-popup help)])
         [ui/DatePicker (cond-> {:custom-input     (reagent/as-element [ui/Input {:style {:width "250px"}}])
                                 :show-time-select true
                                 :read-only        read-only
                                 :locale           @locale
                                 :date-format      "d MMMM yyyy, hh:mm a"
                                 :on-change        (fn [date]
                                                     (reset! date-atom date)
                                                     (update-fn form-id name date))}
                                @date-atom (assoc :selected @date-atom))]]))))


(defmethod form-field :date-time
  [update-fn form-id {:keys [name] :as attribute}]
  ^{:key name}
  [date-time-form update-fn form-id attribute])


(defmethod form-field :boolean
  [update-fn form-id {:keys [name display-name _help _hidden value-scope
                             _required _editable] :as _attribute}]
  (let [label         (or display-name name)
        default-value (or (:value value-scope) (:default value-scope) false)]
    (update-fn form-id name default-value)
    (fn [update-fn form-id {:keys [name _display-name help hidden _value-scope
                                   required editable] :as _attribute}]
      ^{:key name}
      [ui/FormField {:required required}
       (when-not hidden [:label label nbsp (help-popup help)])
       [ui/Checkbox
        (cond-> {:name          name
                 :default-value default-value
                 :read-only     (not editable)
                 :on-change     (ui-callback/checked #(update-fn form-id name %))}
                hidden (assoc :style {:display "none"}))]])))


(defmethod form-field :resource-id
  [update-fn form-id {:keys [name display-name help hidden value-scope
                             required editable] :as _attribute}]
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
