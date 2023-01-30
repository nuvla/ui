(ns sixsq.nuvla.ui.cimi-detail.views
  (:require [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.acl.views :as acl-views]
            [sixsq.nuvla.ui.cimi-detail.events :as events]
            [sixsq.nuvla.ui.cimi-detail.subs :as subs]
            [sixsq.nuvla.ui.cimi.events :as cimi-events]
            [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.utils.collapsible-card :as cc]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.forms :as forms]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.table :as table]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.values :as values]))


(defn path->resource-id
  [path]
  (str/join "/" (rest path)))


(defn action-buttons
  [confirm-label cancel-label on-confirm on-cancel]
  [:div
   [uix/Button
    {:text     cancel-label
     :on-click on-cancel}]
   [uix/Button
    {:text     confirm-label
     :primary  true
     :on-click on-confirm}]])


(defn action-button-icon
  [_menu-item-label _button-confirm-label _icon _title-text _body _on-confirm _on-cancel & [_scrolling?]]
  (let [tr    (subscribe [::i18n-subs/tr])
        show? (r/atom false)]
    (fn [menu-item-label button-confirm-label icon title-text body on-confirm on-cancel & [scrolling?]]
      (let [action-fn (fn []
                        (reset! show? false)
                        (on-confirm))
            cancel-fn (fn []
                        (reset! show? false)
                        (on-cancel))]

        [ui/Modal
         {:open       (boolean @show?)
          :close-icon true
          :on-click   #(.stopPropagation %)
          :on-close   #(reset! show? false)
          :trigger    (r/as-element
                        [ui/MenuItem {:aria-label menu-item-label
                                      :name       menu-item-label
                                      :on-click   #(reset! show? true)}
                         (when icon
                           [ui/Icon {:name icon}])
                         (str/capitalize menu-item-label)])}
         [uix/ModalHeader {:header title-text}]
         [ui/ModalContent {:scrolling (boolean scrolling?)} body]
         [ui/ModalActions
          [action-buttons button-confirm-label (@tr [:cancel]) action-fn cancel-fn]]]))))


(defn edit-button
  "Creates an edit that will bring up an edit dialog and will save the
   modified resource when saved."
  [data _action-fn]
  (let [tr   (subscribe [::i18n-subs/tr])
        text (atom (general-utils/edn->json data))]
    (fn [{:keys [id] :as data} action-fn]
      (reset! text (general-utils/edn->json data))
      [action-button-icon
       (@tr [:raw])
       (@tr [:save])
       "pencil"
       (str (@tr [:editing]) " " id)
       [forms/resource-editor id text]
       (fn []
         (try
           (action-fn (general-utils/json->edn @text))
           (catch :default e
             (action-fn e))))
       #(reset! text (general-utils/edn->json data))
       true])))


(defn delete-button
  "Creates a button that will bring up a delete dialog and will execute the
   delete when confirmed."
  [_data _action-fn]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [data action-fn]
      [action-button-icon
       (@tr [:delete])
       (@tr [:delete])
       "trash"
       (@tr [:delete-resource])
       [:p (@tr [:delete-resource-msg] [(:id data)])]
       action-fn
       (constantly nil)])))


(defmulti other-button
          (fn [{:keys [resource-type] :as _resource} operation]
            [resource-type operation]))


(defmethod other-button :default
  [{:keys [resource-type] :as _resource} operation]
  (let [tr         (subscribe [::i18n-subs/tr])
        params     (subscribe [::cimi-subs/resource-metadata-input-parameters resource-type operation])
        form-data  (atom {})
        op-display (general-utils/name->display-name operation)]
    (fn [{:keys [id] :as _resource} operation]
      [action-button-icon
       op-display
       op-display
       (case operation
         "download" "cloud download"
         "upload" "cloud upload"
         "describe" "info"
         "ready" "check"
         "start" "rocket"
         "stop" "stop"
         "reboot" "power"
         "cog")
       op-display
       (if @params
         [ui/Form
          (for [param @params]
            ^{:key (:name param)}
            [ff/form-field #(swap! form-data assoc %2 %3)
             :operation-form (assoc param
                               :editable true
                               :display-name (general-utils/name->display-name (:name param))
                               :help (:description param))])]
         [:p (@tr [:execute-action-msg] [operation])])
       #(dispatch [::events/operation id operation @form-data])
       (constantly nil)])))


;; Explicit keys have been added to the operation buttons to avoid react
;; errors for duplicate keys, which may happen when the data contains :key.
;; It is probably a bad idea to have a first argument that can be a map
;; as this will be confused with reagent options.
(defn operation-button [{:keys [id resource-type] :as data} operation]
  (let [resource-metadata (subscribe [::cimi-subs/resource-metadata resource-type])]
    (when (and resource-type (nil? @resource-metadata))
      (dispatch [::cimi-events/get-resource-metadata resource-type]))
    (case operation
      "edit" ^{:key "edit"} [edit-button data #(dispatch [::events/edit id %])]
      "delete" ^{:key "delete"} [delete-button data #(dispatch [::events/delete id])]
      ^{:key operation} [other-button data operation])))


(defn format-operations [{:keys [id operations] :as data} & [remove-fn]]
  (let [ops (map #(general-utils/operation-name (:rel %)) operations)]
    (for [op (cond->> ops
                      remove-fn (remove remove-fn))]
      ^{:key (str id "_" op)}
      [operation-button data op])))


(defn metadata-row
  [k v]
  (let [value (cond
                (vector? v) v
                (map? v) (with-out-str (pprint v))
                :else (str v))]
    [ui/TableRow
     [ui/TableCell {:collapsing true} (str k)]
     [ui/TableCell value]]))


(defn detail-header
  [{:keys [id created updated name description properties acl parent subtype method state] :as data}]
  (when data
    [cc/metadata
     {:title       (or name id)
      :subtitle    (when name id)
      :description description
      :icon        "file"
      :updated     updated
      :acl         acl
      :properties  properties}
     (cond-> []
             id (conj (metadata-row "id" id))
             name (conj (metadata-row "name" name))
             description (conj (metadata-row "description" description))
             created (conj (metadata-row "created" (time/time-value created)))
             updated (conj (metadata-row "updated" (time/time-value updated)))
             subtype (conj (metadata-row "subtype" subtype))
             method (conj (metadata-row "method" method))
             state (conj (metadata-row "state" state))
             parent (conj (metadata-row "parent" (values/as-link parent))))]))


(defn strip-attr-ns
  "Strips the attribute namespace from the given key."
  [k]
  (last (re-matches #"(?:([^:]*):)?(.*)" (name k))))


(defn tuple-to-row
  [[key value]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (name key)]
   [ui/TableCell {:style {:max-width     "80ex"             ;; FIXME: need to get this from parent container
                          :text-overflow "ellipsis"
                          :overflow      "hidden"}} (if (vector? value)
                                                      (values/format-collection value)
                                                      (values/format-value value))]])


(defn group-table-sui
  [group-data]
  (let [data (sort-by first group-data)]
    (table/definition-table (map tuple-to-row data))))


(defn detail-menu
  [RefreshButton data]
  (let [MenuItems (format-operations data)]
    [components/ResponsiveMenuBar
     MenuItems
     RefreshButton]))


(defn resource-detail
  "Provides a generic visualization of a CIMI resource document."
  [refresh-button data]
  [:<>
   (detail-menu refresh-button data)
   (detail-header data)
   (group-table-sui (general-utils/remove-common-attrs data))])



(defn cimi-detail
  []
  (let [path               (subscribe [::route-subs/nav-path])
        loading?           (subscribe [::subs/loading?])
        cached-resource-id (subscribe [::subs/resource-id])
        resource           (subscribe [::subs/resource])]
    (fn []
      (let [resource-id       (path->resource-id @path)
            correct-resource? (= resource-id @cached-resource-id)
            {:keys [updated acl] :as resource-value} @resource]

        ;; forces a refresh when the correct resource isn't cached
        (when-not correct-resource?
          (dispatch [::events/get (path->resource-id @path)]))

        ;; render the (possibly empty) detail
        [:<>
         (when acl
           ^{:key (str resource-id "-" updated)}
           [:div {:style {:min-height "30px"}}
            [acl-views/AclButton {:default-value acl
                                  :read-only     (not (general-utils/can-edit? resource-value))
                                  :on-change     #(dispatch [::events/edit
                                                             resource-id
                                                             (assoc resource-value :acl %)])
                                  :margin-override {:margin-top 0}}]])
         [resource-detail
          [components/RefreshMenu
           {:on-refresh #(dispatch [::events/get resource-id])
            :loading?   @loading?}]
          (when (and (not @loading?) correct-resource?) @resource)]]))))
