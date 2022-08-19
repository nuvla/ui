(ns sixsq.nuvla.ui.plugins.step-group
  (:require
    [re-frame.core :refer [dispatch subscribe reg-event-fx]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.plugins.helpers :as helpers]
    [taoensso.timbre :as log]
    [cljs.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [clojure.string :as str]
    [reagent.core :as r]))

(s/def ::active-step keyword?)
(s/def ::items (s/nilable coll?))
(s/def ::change-event (s/nilable coll?))

(defn build-spec
  [& {:keys [active-step]}]
  {::active-step active-step})

(defn change-step
  [db-path step-key]
  (dispatch [::helpers/set db-path ::active-step step-key]))

(reg-event-fx
  ::change-step
  (fn [{db :db} [_ db-path step-key]]
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:db (assoc-in db (conj db-path ::active-step) step-key)
       :fx [(when change-event
              [:dispatch change-event])]})))

(defn- key->render
  [items step-key]
  (or (some #(when (= step-key (:key %)) (:render %)) items)
      (log/error "step-key not found: " step-key items)))

(defn- NextPreviousButton
  [{:keys [db-path get-step icon content label-position]}]
  (let [active-step @(subscribe [::helpers/retrieve db-path ::active-step])
        items       @(subscribe [::helpers/retrieve db-path ::items])
        step        (get-step items active-step)
        disabled?   (or (nil? step) (:disabled step))]
    [ui/Button {:icon           icon
                :content        (r/as-element [uix/TR content str/capitalize])
                :disabled       disabled?
                :on-click       #(dispatch [::change-step db-path (:key step)])
                :label-position label-position}]))

(defn PreviousButton
  [db-path]
  [NextPreviousButton
   {:db-path        db-path
    :get-step       (fn [items active-step]
                      (->> items
                           (take-while #(not= active-step (:key %)))
                           last))
    :icon           "left chevron"
    :content        :previous
    :label-position :left}])

(defn NextButton
  [db-path]
  [NextPreviousButton
   {:db-path        db-path
    :get-step       (fn [items active-step]
                      (->> items
                           (drop-while #(not= active-step (:key %)))
                           second))
    :icon           "right chevron"
    :content        :next
    :label-position :right}])

(defn PreviousNextButtons
  [db-path]
  [ui/ButtonGroup {:floated :right}
   [PreviousButton db-path]
   [NextButton db-path]])

(defn StepPane
  [{:keys [db-path items] :as _opts}]
  (let [active-step (subscribe [::helpers/retrieve db-path ::active-step])
        render      (key->render items @active-step)]
    (when render
      [render])))

(defn StepGroup
  [{:keys [db-path items change-event] :as opts}]
  (dispatch [::helpers/set db-path
             ::change-event change-event
             ::items items])
  (let [active-step (subscribe [::helpers/retrieve db-path ::active-step])
        items       (map (fn [{step-key :key :as item}]
                           (-> item
                               (assoc :onClick #(dispatch [::change-step
                                                           db-path step-key])
                                      :active (= @active-step step-key))
                               (dissoc :render))) items)
        opts        (-> opts
                        (dissoc :db-path :change-event)
                        (assoc :items items))]
    [ui/StepGroup opts]))
