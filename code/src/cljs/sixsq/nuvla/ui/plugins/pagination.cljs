(ns sixsq.nuvla.ui.plugins.pagination
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
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

(reg-event-fx
  ::change-page
  (fn [{db :db} [_ db-path page]]
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:db (assoc-in db (conj db-path ::active-page) page)
       :fx [[:dispatch change-event]]})))

(defn first-last-params
  [db db-path params]
  (let [page           (get-in db (conj db-path ::active-page))
        items-per-page (get-in db (conj db-path ::items-per-page))]
    (assoc params
      :first (inc (* (dec page) items-per-page))
      :last (* page items-per-page))))

(defn- icon
  [icon-name]
  {:content (r/as-element [ui/Icon {:name icon-name}]) :icon true})

(defn Pagination
  [{:keys [db-path total-items change-event] :as _opts}]
  (dispatch [::helpers/set db-path ::change-event change-event])
  (let [dipp          @(subscribe [::helpers/retrieve db-path
                                   ::default-items-per-page])
        per-page-opts (map (fn [i]
                             {:key   (* dipp i)
                              :value (* dipp i)
                              :text  (* dipp i)})
                           (range 1 4))
        change-page   #(dispatch [::change-page db-path %])
        tr            @(subscribe [::i18n-subs/tr])
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
                   :margin-top      10}}
     [ui/Label {:size :medium}
      (str (str/capitalize (tr [:total])) " : " (or total-items 0))
      [:div {:style {:display :inline-block}}
       ff/nbsp
       "| "
       [ui/Dropdown {:value     per-page
                     :trigger   per-page
                     :options   per-page-opts
                     :on-change (ui-callback/value
                                  #(do
                                     (dispatch [::helpers/set db-path
                                                ::items-per-page %])
                                     (change-page active-page)))}]
       " " (tr [:per-page])]]
     [ui/Pagination
      {:size          :tiny
       :total-pages   total-pages
       :first-item    (icon "angle double left")
       :last-item     (icon "angle double right")
       :prev-item     (icon "angle left")
       :next-item     (icon "angle right")
       :ellipsis-item nil
       :active-page   active-page
       :onPageChange  (ui-callback/callback :activePage #(change-page %))}]]))

(s/def ::total-items (s/nilable nat-int?))

(s/fdef Pagination
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::helpers/change-event
                                            ::total-items])))

