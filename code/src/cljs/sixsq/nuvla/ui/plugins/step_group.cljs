(ns sixsq.nuvla.ui.plugins.step-group
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [taoensso.timbre :as log]))

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

(defn- key->content
  [items step-key]
  (or (some #(when (= step-key (:key %)) (:content %)) items)
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

(defn- PreviousButton
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

(defn- NextButton
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

(defn- PreviousNextButtons
  [db-path]
  [:div {:style {:display         :flex
                 :justify-content :flex-end
                 :margin-top      10}}
   [ui/ButtonGroup
    [PreviousButton db-path]
    [NextButton db-path]]])

(defn StepGroup
  [{:keys [db-path items change-event] :as opts}]
  (dispatch [::helpers/set db-path
             ::change-event change-event
             ::items items])
  (let [active-step @(subscribe [::helpers/retrieve db-path ::active-step])
        content     (key->content items active-step)
        items       (map (fn [{step-key :key :as item}]
                           (-> item
                               (assoc :onClick #(dispatch [::change-step
                                                           db-path step-key])
                                      :active (= active-step step-key))
                               (dissoc :content))) items)]
    [:<>
     [ui/StepGroup
      (-> opts
          (dissoc :db-path :change-event)
          (assoc :items items))]
     (when content content)
     [PreviousNextButtons db-path]]))

(s/fdef StepGroup
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::helpers/change-event])))
