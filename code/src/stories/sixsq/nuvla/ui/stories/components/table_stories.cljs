(ns sixsq.nuvla.ui.stories.components.table-stories
  (:require [sixsq.nuvla.ui.stories.helper :as helper]
            [sixsq.nuvla.ui.common-components.plugins.table :refer [Table]]
            [reagent.core :as reagent]))

(defn TableWrapper
  [n-cols _on-click]
  [Table {:columns
          (for [i (range (or n-cols 4))
                :let [k (keyword (str "col" i))]]
            ^{:key k}
            {:field-key      k
             :header-content (constantly (str "Column " i))
             :cell           (fn [{{:keys [info]} :row-data}]
                               [:p info])})
          :rows [^{:key "row1"} {:info "Info 1"}
                 ^{:key "row2"} {:info "Info 2"}]}])


(defn ^:export table [args]
  (let [params   (-> args helper/->params)
        n-cols   (:numberOfColumns params)
        on-click (:on-click params)]
    (reagent/as-element
      [TableWrapper n-cols on-click])))

