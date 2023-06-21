(ns sixsq.nuvla.ui.plugins.step-group
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
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
     (when content content)]))

(s/fdef StepGroup
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::helpers/change-event])))
