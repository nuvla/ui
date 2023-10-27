(ns sixsq.nuvla.ui.ui-demo.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
            [sixsq.nuvla.ui.cimi.views :refer [MenuBar]]
            [sixsq.nuvla.ui.plugins.table :refer [TableColsEditable]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn UiDemo
  []
   (let [collection (subscribe [::cimi-subs/collection])
         icons      (->> (ns-publics 'sixsq.nuvla.ui.utils.icons)
                         (filter #(and
                                    (not (str/starts-with? (name (first %)) "i-"))
                                    (not= (first %) "Icon")))
                         (sort-by first))]
     [ui/Tab
     {:panes [{:menuItem "Table"
               :render #(r/as-element
                          [:<>
                           [MenuBar]
                           [TableColsEditable {:rows (:resources @collection)}]])}
              {:menuItem "Icons"
               :render #(r/as-element
                          [ui/Segment
                           [:div {:style {:display :flex
                                          :flex-wrap :wrap
                                          :gap "50px"
                                          :margin :auto
                                          :margin-top "100px"
                                          :max-width "75%"}}
                            (for [[k v] icons]
                              (when (and
                                      (not (str/starts-with? (name k) "i-"))
                                      (not= (name k) "Icon"))
                                [:div {:style {:width "25%"}}
                                 [v {:size :large}]
                                 [:span (-> v symbol name)]]))]])}]}]))

(comment
  (if (some #{:ka} [:k :b]) true false)


  ;; add as second column
  (dispatch [::add-col
             {:col-key :updated :position 1 :db-path ::table-cols-config}])

  ;; remove column
  (dispatch [::reset-current-cols ::table-cols-config])
  (dispatch [::remove-col :state :sixsq.nuvla.ui.edges.views/table-cols-config])
  (dispatch [::remove-col :created :sixsq.nuvla.ui.edges.views/table-cols-config])

  ;; no position adds as last column
  (dispatch [::add-col {:col-key :updated
                        :db-path ::table-cols-config}])
  ;; adding same column again does not work
  (dispatch [::add-col {:col-key :updated
                        :db-path ::table-cols-config}])

  ;; setting default column
  (dispatch [::set-current-cols default-columns ::table-cols-config])

  (dispatch [::reset-current-cols :sixsq.nuvla.ui.edges.views/table-cols-config]))