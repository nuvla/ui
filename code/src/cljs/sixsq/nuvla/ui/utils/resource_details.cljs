(ns sixsq.nuvla.ui.utils.resource-details
  (:require
    [cljs.pprint :refer [pprint]]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-detail.events :as api-detail-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.forms :as forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.table :as table]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as values]))


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
  [menu-item-label button-confirm-label icon title-text body on-confirm on-cancel & [scrolling? position]]
  (let [show? (r/atom false)]
    (fn [menu-item-label button-confirm-label icon title-text body on-confirm on-cancel & [scrolling? position]]
      (let [action-fn (fn []
                        (reset! show? false)
                        (on-confirm))
            cancel-fn (fn []
                        (reset! show? false)
                        (on-cancel))]

        [ui/Modal
         {:open       (boolean @show?)
          :close-icon true
          :on-close   #(reset! show? false)
          :trigger    (r/as-element
                        [ui/MenuItem (cond-> {:aria-label menu-item-label
                                              :name       menu-item-label
                                              :on-click   #(reset! show? true)}
                                             position (assoc :position position))
                         (when icon
                           [ui/Icon {:name icon}])
                         menu-item-label])}
         [ui/ModalHeader title-text]
         [ui/ModalContent (cond-> {}
                                  scrolling? (assoc :scrolling true)) body]
         [ui/ModalActions
          [action-buttons button-confirm-label "cancel" action-fn cancel-fn]]]))))


(defn edit-button
  "Creates an edit that will bring up an edit dialog and will save the
   modified resource when saved."
  [data action-fn]
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
  [data action-fn]
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


(defn other-button
  "Creates a button that will bring up a dialog to confirm the given action."
  [label data action-fn]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [label data action-fn]
      [action-button-icon
       label
       label
       (case label
         "download" "cloud download"
         "upload" "cloud upload"
         "describe" "info"
         "ready" "check"
         "start" "rocket"
         "stop" "stop"
         "cog")
       (@tr [:execute-action] [label])
       [:p (@tr [:execute-action-msg] [label (:id data)])]
       action-fn
       (constantly nil)])))


;; Explicit keys have been added to the operation buttons to avoid react
;; errors for duplicate keys, which may happen when the data contains :key.
;; It is probably a bad idea to have a first argument that can be a map
;; as this will be confused with reagent options.
(defn operation-button [{:keys [id] :as data} operation]
  (case operation
    "edit" ^{:key "edit"} [edit-button data #(dispatch [::api-detail-events/edit id %])]
    "delete" ^{:key "delete"} [delete-button data #(dispatch [::api-detail-events/delete id])]
    ^{:key operation} [other-button operation data #(dispatch [::api-detail-events/operation id operation])]))


(defn format-operations [refresh-button {:keys [operations] :as data}]
    (let [ops (map #(general-utils/operation-name (:rel %)) operations)]
    (vec (concat [refresh-button] (map (partial operation-button data) ops)))))


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
             parent (conj (metadata-row "parent" parent)))]))


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
  [refresh-button data]
  (vec (concat [ui/Menu {:borderless true}]
               (format-operations nil data)
               [refresh-button])))


(defn resource-detail
  "Provides a generic visualization of a CIMI resource document."
  [refresh-button data]
  [:<>
   (detail-menu refresh-button data)
   (detail-header data)
   (group-table-sui (general-utils/remove-common-attrs data))])
