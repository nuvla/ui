(ns sixsq.nuvla.ui.plugins.step-group
  (:require
    [re-frame.core :refer [dispatch subscribe reg-sub reg-event-db]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.plugins.helpers :as helpers]
    [taoensso.timbre :as log]))

(defn add-spec
  [location-kw default-active-step]
  {location-kw {::active-step default-active-step}})

(defn change-step
  [db-location step-key]
  (dispatch [::helpers/set db-location ::active-step step-key]))

(defn- key->render
  [items step-key]
  (or (some #(when (= step-key (:key %)) (:render %)) items)
      (log/error "step-key not found: " step-key items)))

(defn- on-step-change
  [db-location on-change step-key]
  (change-step db-location step-key)
  (when on-change
    (on-change step-key)))

(defn- NextPreviousButton
  [{:keys [db-location get-step icon content label-position]}]
  (let [active-step @(subscribe [::helpers/retrieve db-location ::active-step])
        {:keys [items on-change]} @(subscribe [::helpers/retrieve
                                               db-location ::opts])
        step        (get-step items active-step)
        disabled?   (or (nil? step) (:disabled step))
        on-click    (partial on-step-change db-location on-change)]
    [ui/Button {:icon           icon
                :content        content
                :disabled       disabled?
                :on-click       #(on-click (:key step))
                :label-position label-position}]))

(defn PreviousButton
  [db-location]
  [NextPreviousButton
   {:db-location    db-location
    :get-step       (fn [items active-step]
                      (->> items
                           (take-while #(not= active-step (:key %)))
                           last))
    :icon           "left chevron"
    :content        "Previous"
    :label-position :left}])

(defn NextButton
  [db-location]
  [NextPreviousButton
   {:db-location    db-location
    :get-step       (fn [items active-step]
                      (->> items
                           (drop-while #(not= active-step (:key %)))
                           second))
    :icon           "right chevron"
    :content        "Next"
    :label-position :right}])

(defn PreviousNextButtons
  [db-location]
  [ui/ButtonGroup {:floated :right}
   [PreviousButton db-location]
   [NextButton db-location]])

(defn StepPane
  [{:keys [db-location items] :as _opts}]
  (let [active-step (subscribe [::helpers/retrieve db-location ::active-step])
        render      (key->render items @active-step)]
    (when render
      [render])))

(defn StepGroup
  [{:keys [db-location items on-change] :as opts}]
  (dispatch [::helpers/set db-location ::opts opts])
  (let [on-click    (partial on-step-change db-location on-change)
        active-step (subscribe [::helpers/retrieve db-location ::active-step])
        items       (map (fn [{step-key :key :as item}]
                           (-> item
                               (assoc :onClick #(on-click step-key)
                                      :active (= @active-step step-key))
                               (dissoc :render))) items)
        opts        (-> opts
                        (dissoc :db-location :on-change)
                        (assoc :items items))]
    [ui/StepGroup opts]))
