(ns sixsq.nuvla.ui.common-components.plugins.nav-tab-refactor
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn Tab
  [{::keys [!panes attached tabular !active-pane set-active-pane-fn] :as control}]
  (let [clean-panes (fn [item]
                      (let [menuItem (:menuItem item)
                            k        (:key menuItem)
                            icon     (:icon menuItem)
                            clean-i  (if (and (string? icon)
                                              (some #(str/starts-with? icon %) ["fa-" "fal " "fad " "fas "]))
                                       (r/as-element [icons/Icon {:name icon}])
                                       icon)
                            on-click (fn [event]
                                       (.preventDefault event)
                                       (set-active-pane-fn k))]
                        (-> item
                            (update :menuItem merge
                                    {:onClick                  #(when-not (:disabled menuItem)
                                                                  (on-click %))
                                     :data-reitit-handle-click false
                                     :icon                     clean-i}))))]
    (let [clean-panes (map clean-panes (remove nil? @!panes))
          key->index  (zipmap (map (comp :key :menuItem) clean-panes)
                              (range (count clean-panes)))]
      [ui/Tab
       (merge
         (-> {:panes        clean-panes
              :active-index (get key->index @!active-pane 0)}
             (assoc-in [:menu :class] :uix-tab-nav)
             (assoc-in [:menu :tabular] tabular)
             (assoc-in [:menu :attached] attached)))])))

(defn NavTabController
  [{:keys [;; tab panes
           !panes

           ;; Optional
           ;; the active pane key
           !active-pane
           ;; fn to set the active pane
           set-active-pane-fn

           ;; Optional
           ;; Translations
           tr-fn
           ]}]
  (r/with-let [!panes             (or !panes (r/atom []))
               !active-pane       (or !active-pane (r/atom (-> @!panes first :menuItem :key)))
               set-active-pane-fn (or set-active-pane-fn #(reset! !active-pane %))
               tr-fn              (or tr-fn (comp str/capitalize name first))]
    [Tab {::!panes             !panes
          ::!active-pane       !active-pane
          ::set-active-pane-fn set-active-pane-fn
          ::tr-fn              tr-fn}]))
