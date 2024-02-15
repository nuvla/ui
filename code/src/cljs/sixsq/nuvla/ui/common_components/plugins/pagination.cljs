(ns sixsq.nuvla.ui.common-components.plugins.pagination
  (:require [cljs.spec.alpha :as s]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [re-frame.cofx :refer [inject-cofx]]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(s/def ::items-per-page (s/nilable int?))
(s/def ::default-items-per-page (s/nilable int?))
(s/def ::active-page (s/nilable int?))
(s/def ::change-event (s/nilable coll?))

(defn build-spec
  [& {:keys [default-items-per-page]
      :or   {default-items-per-page 10}}]
  {::items-per-page         default-items-per-page
   ::default-items-per-page default-items-per-page
   ::active-page            1})

(def local-storage-key-prefix "nuvla.ui.pagination.")

(defn- get-local-storage-key [db-path]
  (str local-storage-key-prefix db-path))


(reg-event-fx
  ::init-paginations
  [(inject-cofx :storage/all)]
  (fn [{db :db storage :storage/all}]
    (let [store-entries (filter #(str/starts-with? (first %) local-storage-key-prefix)
                                storage)
          paginations   (reduce (fn [paginations entry]
                                  (merge paginations (edn/read-string (second entry))))
                                {}
                                store-entries)]
      {:db (reduce-kv (fn [db k v] (if (vector? k)
                                     (update-in db k merge v)
                                     (update db k merge v))) db paginations)})))

(reg-event-fx
  ::store-pagination
  (fn [_ [_ db-path items-per-page]]
    {:storage/set {:session? false
                   :name     (get-local-storage-key db-path)
                   :value    {db-path {::items-per-page items-per-page}}}}))

(reg-event-fx
  ::change-page
  (fn [{db :db} [_ db-path page]]
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:db (assoc-in db (conj db-path ::active-page) page)
       :fx [(when change-event [:dispatch change-event])]})))

(defn first-last-params
  [db db-path params]
  (let [page           (get-in db (conj db-path ::active-page))
        items-per-page (get-in db (conj db-path ::items-per-page))]
    (assoc params
      :first (inc (* (dec page) items-per-page))
      :last (* page items-per-page))))

(defn- icon
  [icon-name]
  {:content (r/as-element [ui/Icon {:class icon-name}]) :icon true})

(defn Pagination
  [{:keys [db-path total-items change-event i-per-page-multipliers] :as _opts}]
  (dispatch [::helpers/set db-path ::change-event change-event])
  (let [tr            @(subscribe [::i18n-subs/tr])
        dipp          @(subscribe [::helpers/retrieve db-path ::default-items-per-page])
        per-page-opts (map (fn [i]
                             (let [n-per-page (* dipp i)]
                               {:key     n-per-page
                                :value   n-per-page
                                :content n-per-page
                                :text    (str n-per-page " " (tr [:per-page]))}))
                           (or i-per-page-multipliers (range 1 4)))
        change-page   #(dispatch [::change-page db-path %])
        active-page   @(subscribe [::helpers/retrieve db-path ::active-page])
        per-page      @(subscribe [::helpers/retrieve db-path ::items-per-page])
        total-pages   (or (some-> total-items
                                  (general-utils/total-pages per-page))
                          0)]
    (when (and (> active-page total-pages)
               (not= total-pages 0))
      (change-page 1))
    [:div {:style {:display         :flex
                   :justify-content :space-between
                   :align-items     :baseline
                   :flex-wrap       :wrap-reverse
                   :margin-top      10}
           :class :uix-pagination}
     [:div {:style {:display :flex}
            :class :uix-pagination-control}
      [:div {:style {:display :flex}}
       [:div {:style {:margin-right "0.5rem"}}
        (str (str/capitalize (tr [:total])) ":")]
       [:div (or total-items 0)]]
      [:div {:style {:color "#C10E12" :margin-right "1rem" :margin-left "1rem"}} "| "]
      [ui/Dropdown {:value     per-page
                    :options   per-page-opts
                    :pointing  true
                    :on-change (ui-callback/value
                                 #(do
                                    (dispatch [::helpers/set db-path ::items-per-page %])
                                    (dispatch [::store-pagination db-path %])
                                    (change-page active-page)))}]]
     [ui/Pagination
      {:size          :tiny
       :class         :uix-pagination-navigation
       :total-pages   total-pages
       :first-item    (icon "angle double left")
       :last-item     (icon "angle double right")
       :prev-item     (icon "angle left")
       :next-item     (icon "angle right")
       :ellipsis-item nil
       :active-page   active-page
       :onPageChange  (ui-callback/callback :activePage #(change-page %))}]]))

(s/def ::total-items (s/nilable nat-int?))
(s/def ::i-per-page-multipliers (s/nilable #(and
                                              (vector? %)
                                              (nat-int? (first %))
                                              (apply < %))))

(s/fdef Pagination
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::helpers/change-event
                                            ::total-items]
                                   :opt-un [::i-per-page-multipliers])))
