(ns sixsq.nuvla.ui.docs.views
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.docs-detail.views :as docs-details-view]
    [sixsq.nuvla.ui.docs.events :as events]
    [sixsq.nuvla.ui.docs.subs :as subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]))


(defn row-fn [{:keys [id] :as entry}]
  [ui/TableRow {:on-click #(dispatch [::history-events/navigate
                                      (str "documentation/" (general-utils/resource-id->uuid id))])}
   [ui/CopyToClipboard {:text (:name entry)} [ui/TableCell {:collapsing true} (:name entry)]]
   [ui/TableCell {:style {:max-width     "150px"
                          :overflow      "hidden"
                          :text-overflow "ellipsis"}} (:description entry)]])


(defn documents-table
  []
  (let [tr (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])
        documents (subscribe [::subs/documents])]
    [ui/Segment (merge style/basic
                       {:class-name "nuvla-ui-x-autoscroll"
                        :loading    (or @loading? (nil? @documents))})

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
                      (map row-fn (sort-by :name (vals @documents)))))]])]))


(defn documents-view
  []
  [ui/Container {:fluid true}
   [documents-table]])


(defmethod panel/render :documentation
  [_]
  [documents-view])


(defn documentation-resource
  []
  (let [path (subscribe [::main-subs/nav-path])
        documents (subscribe [::subs/documents])]
    (dispatch [::events/get-documents])
    (fn []
      (let [n (count @path)
            children (case n
                       1 [[documents-view]]
                       2 [[docs-details-view/docs-detail (str "resource-metadata/" (second @path))]]
                       [[documents-view]])]
        (vec (concat [ui/Segment style/basic] children))))))


(defmethod panel/render :documentation
  [path]
  [documentation-resource])
