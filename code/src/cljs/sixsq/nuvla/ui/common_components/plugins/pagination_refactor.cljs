(ns sixsq.nuvla.ui.common-components.plugins.pagination-refactor
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn !paginated-data-fn
  [{:keys [!pagination !enable-pagination? !data] :as _control}]
  (r/track (fn paginated-data-fn []
             (if @!enable-pagination?
               (let [{:keys [page-index page-size]} @!pagination
                     start (* page-index page-size)
                     end   (min (+ start page-size) (count @!data))]
                 (subvec @!data start end))
               @!data))))

(defn- icon
  [icon-name]
  {:content (r/as-element [ui/Icon {:class icon-name}]) :icon true})

(defn Pagination
  [{:keys [::tr-fn ::!page-sizes ::!pagination ::set-pagination-fn ::total-items] :as _control}]
  (let [{:keys [page-index page-size] :as pagination} @!pagination
        page-count    (cond-> (quot total-items page-size)
                              (pos? (rem total-items page-size)) inc)
        goto-page     #(set-pagination-fn (assoc pagination :page-index (max 0 (min (dec page-count) %))))
        set-page-size #(set-pagination-fn (assoc pagination :page-size % :page-index 0))
        per-page-opts (map (fn [n-per-page] {:key     n-per-page
                                             :value   n-per-page
                                             :content n-per-page
                                             :text    (str n-per-page " per page")})
                           @!page-sizes)]
    [:div {:style {:display         :flex
                   :position        :relative
                   :justify-content :space-between
                   :align-items     :baseline
                   :flex-wrap       :wrap-reverse
                   :margin-top      10}
           :class :uix-pagination}
     [:div {:style {:display :flex}
            :class :uix-pagination-control}
      [:div {:style {:display :flex}}
       [:div {:style {:margin-right "0.5rem"}}
        (str (str/capitalize (tr-fn [:total])) ":")]
       [:div (or total-items 0)]]
      [:div {:style {:color "#C10E12" :margin-right "1rem" :margin-left "1rem"}} "| "]
      [ui/Dropdown {:value     page-size
                    :options   per-page-opts
                    :pointing  true
                    :on-change (ui-callback/value set-page-size)}]]
     [ui/Pagination
      {:size          :tiny
       :class         :uix-pagination-navigation
       :total-pages   page-count
       :first-item    (icon "angle double left")
       :last-item     (icon "angle double right")
       :prev-item     (icon "angle left")
       :next-item     (icon "angle right")
       :ellipsis-item nil
       :active-page   (inc page-index)
       :onPageChange  (ui-callback/callback :activePage #(goto-page (dec %)))}]]))

(defn PaginationController
  [{:keys [;; total number of items
           total-items

           ;; Optional
           ;; Available page sizes
           !page-sizes

           ;; Optional (disabled by default)
           ;; Pagination
           !enable-pagination?
           !pagination
           set-pagination-fn

           ;; Optional
           ;; Translations
           tr-fn
           ]}]
  (r/with-let [!enable-pagination? (or !enable-pagination? (r/atom false))
               !page-sizes         (or !page-sizes (r/atom [10 25 50 100]))
               !pagination         (or !pagination (r/atom {:page-index 0, :page-size 10}))
               set-pagination-fn   (or set-pagination-fn #(reset! !pagination %))
               tr-fn               (or tr-fn (comp str/capitalize name first))]
    [Pagination {::!enable-pagination? !enable-pagination?
                 ::total-items         total-items
                 ::!page-sizes         !page-sizes
                 ::!pagination         !pagination
                 ::set-pagination-fn   set-pagination-fn
                 ::tr-fn               tr-fn}]))
