(ns sixsq.nuvla.ui.ui-demo.views
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
            [sixsq.nuvla.ui.cimi.views :refer [MenuBar]]
            [sixsq.nuvla.ui.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [clojure.string :as str]))

(defn TableColsEditable
  [props]
  [Table props])

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
                          [:div
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