(ns sixsq.nuvla.ui.pages.docs.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.docs-detail.views :as docs-details-view]
            [sixsq.nuvla.ui.pages.docs.events :as events]
            [sixsq.nuvla.ui.pages.docs.subs :as subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.style :as style]))


(defn row-fn [{:keys [id] :as entry}]
  [ui/TableRow {:on-click #(dispatch [::routing-events/navigate routes/documentation-sub-page {:sub-path (general-utils/id->uuid id)}])}
   [ui/CopyToClipboard {:text (:name entry)} [ui/TableCell {:collapsing true} (:name entry)]]
   [ui/TableCell {:style {:max-width     "150px"
                          :overflow      "hidden"
                          :text-overflow "ellipsis"}} (:description entry)]])


(defn documents-table
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        documents (subscribe [::subs/documents])]
    [components/LoadingPage {}
     [ui/Segment (merge style/basic
                        {:class-name "nuvla-ui-x-autoscroll"})

      (when @documents
        [:div
         [ui/Table
          {:style       {:cursor "pointer"}
           :compact     "very"
           :single-line true
           :padded      false
           :unstackable true
           :selectable  true}
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell (@tr [:name])]
            [ui/TableHeaderCell (@tr [:description])]]]
          (vec (concat [ui/TableBody]
                       (map row-fn (sort-by :name (vals @documents)))))]])]]))


(defn documents-view
  []
  (dispatch [::events/get-documents])
  (fn []
    [ui/Container {:fluid true}
     [documents-table]]))


(defn documentation
  [{:keys [path]}]
  (let [loading? (subscribe [::subs/loading?])
        n        (count path)
        children (case n
                   1 [documents-view]
                   2 [docs-details-view/docs-detail (str "resource-metadata/" (second path))]
                   [documents-view])]
    [ui/Segment (merge style/basic {:loading @loading?}) children]))
