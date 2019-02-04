(ns sixsq.nuvla.webui.docs.views
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.webui.docs-detail.views :as docs-details-view]
    [sixsq.nuvla.webui.docs.events :as events]
    [sixsq.nuvla.webui.docs.subs :as subs]
    [sixsq.nuvla.webui.history.events :as history-events]
    [sixsq.nuvla.webui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.webui.main.subs :as main-subs]
    [sixsq.nuvla.webui.panel :as panel]
    [sixsq.nuvla.webui.utils.general :as general-utils]
    [sixsq.nuvla.webui.utils.semantic-ui :as ui]
    [sixsq.nuvla.webui.utils.style :as style]))


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
                       {:class-name "webui-x-autoscroll"
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
