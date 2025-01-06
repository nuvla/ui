(ns sixsq.nuvla.ui.components.nav-tab-scenes
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab-refactor :refer [NavTabController]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(def panes
  (map (fn [idx] {:menuItem {:key    (str "tab" idx)
                             :name   (str "Tab " idx)}
                  :render (fn []
                            (r/as-element
                              [ui/TabPane {:loading false}
                               (str "Tab " idx " content")]))})
       (range 10)))

(defscene basic-nav-tab
  (r/with-let [!panes (r/atom panes)]
    [NavTabController {:!panes !panes}]))

