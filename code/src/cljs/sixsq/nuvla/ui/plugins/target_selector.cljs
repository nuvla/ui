(ns sixsq.nuvla.ui.plugins.target-selector
  (:require [cljs.spec.alpha :as s]
            [clojure.set :as set]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.plugins.pagination :as pagination]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

; "infrastructure-service-swarm" "infrastructure-service-kubernetes"
;(s/def ::subtype (s/nilable #{"swarm" "kubernetes"}))

(defn build-spec
  [& {:keys [default-items-per-page]
      :or   {default-items-per-page 10}}]
  {::loading?          true
   ::apps              nil
   ::selected          #{}
   ::tab               (nav-tab/build-spec :default-tab :edges)
   ::edges-search      (full-text-search/build-spec :persistent? false)
   ::edges-pagination  (pagination/build-spec
                         :default-items-per-page default-items-per-page)
   ::clouds-search     (full-text-search/build-spec :persistent? false)
   ::clouds-pagination (pagination/build-spec
                         :default-items-per-page default-items-per-page)})

(defn db-selected
  [db db-path]
  (get-in db (conj db-path ::selected)))

(reg-event-fx
  ::restore-selected
  (fn [{:keys [db]} [_ db-path selected-ids]]
    (if (seq selected-ids)
      (let [rebuild-selected (fn [{:keys [resources]}]
                               (let [selected-ids-set (set selected-ids)
                                     result-ids-set   (set (map :id resources))
                                     difference-set   (set/difference selected-ids-set result-ids-set)
                                     resources-set    (set resources)]
                                 (cond-> resources-set
                                         (seq difference-set) (set/union
                                                                (->> difference-set
                                                                     (map #(hash-map :id % :subtype "unknown"))
                                                                     set)))))]
        {:db                  (assoc-in db (conj db-path ::loading?) true)
         ::cimi-api-fx/search [:credential {:select "id, name, description, parent, subtype"
                                        :filter (apply general-utils/join-or
                                                       (map #(str "id='" % "'") selected-ids))
                                        :last   10000}
                               #(dispatch [::helpers/set db-path
                                           ::selected (rebuild-selected %)
                                           ::loading? false])]})
      {:db (assoc-in db (conj db-path ::selected) #{})})))

(reg-event-db
  ::set-credentials
  (fn [db [_ db-path response]]
    (-> db
        (assoc-in (conj db-path ::targets-loading?) false)
        (assoc-in (conj db-path ::credentials) response))))

(reg-event-fx
  ::search-credentials
  (fn [_ [_ db-path filter-str]]
    {::cimi-api-fx/search
     [:credential {:last   10000
                   :select "id, name, description, parent, subtype"
                   :filter filter-str}
      #(dispatch [::set-credentials db-path %])]}))

(reg-event-fx
  ::set-infrastructures
  (fn [{db :db} [_ db-path {:keys [resources] :as response}]]
    (if (seq resources)
      (let [filter-str (->> resources
                            (map #(str "parent='" (:id %) "'"))
                            (apply general-utils/join-or)
                            (general-utils/join-and
                              (general-utils/join-or
                                "subtype='infrastructure-service-swarm'"
                                "subtype='infrastructure-service-kubernetes'"
                                )))]
        {:db (assoc-in db (conj db-path ::infrastructures) response)
         :fx [[:dispatch [::search-credentials db-path filter-str]]]})
      {:db (-> db
               (assoc-in (conj db-path ::targets-loading?) false)
               (assoc-in (conj db-path ::infrastructures) response)
               (assoc-in (conj db-path ::credentials) nil))})))

(reg-event-fx
  ::search-infrastructures
  (fn [_ [_ db-path filter-str]]
    {::cimi-api-fx/search
     [:infrastructure-service
      {:last   10000
       :select "id, name, description, subtype, parent"
       :filter filter-str}
      #(dispatch [::set-infrastructures db-path %])]}))

(reg-event-fx
  ::search-clouds
  (fn [{db :db} [_ db-path]]
    {:db (assoc-in db (conj db-path ::targets-loading?) true)
     ::cimi-api-fx/search
     [:infrastructure-service
      (->> {:select  "id, name, description, subtype, parent"
            :orderby "name:asc,id:asc"
            :filter  (general-utils/join-and
                       (general-utils/join-or
                         "tags!='nuvlabox=True'"
                         "tags!='nuvlaedge=True'")
                       (general-utils/join-or
                         "subtype='swarm'"
                         "subtype='kubernetes'")
                       (full-text-search/filter-text
                         db (conj db-path ::clouds-search)))}
           (pagination/first-last-params db (conj db-path ::clouds-pagination)))
      #(dispatch [::set-infrastructures db-path %])]}))

(reg-event-fx
  ::set-edges
  (fn [{db :db} [_ db-path {:keys [resources] :as response}]]
    (if (seq resources)
      (let [filter-str (->> resources
                            (map #(str "parent='"
                                       (:infrastructure-service-group %)
                                       "'"))
                            (apply general-utils/join-or)
                            (general-utils/join-and
                              (general-utils/join-or
                                "subtype='swarm'"
                                "subtype='kubernetes'")))]
        {:db (assoc-in db (conj db-path ::edges) response)
         :fx [[:dispatch [::search-infrastructures db-path filter-str]]]})
      {:db (-> db
               (assoc-in (conj db-path ::targets-loading?) false)
               (assoc-in (conj db-path ::edges) response)
               (assoc-in (conj db-path ::credentials) nil))})))

(reg-event-fx
  ::search-edges
  (fn [{db :db} [_ db-path]]
    {:db (assoc-in db (conj db-path ::targets-loading?) true)
     ::cimi-api-fx/search
     [:nuvlabox
      (->> {:select  "id, name, description, infrastructure-service-group"
            :orderby "name:asc,id:asc"
            :filter  (general-utils/join-and
                       (full-text-search/filter-text db (conj db-path ::edges-pagination))
                       "state='COMMISSIONED'"
                       "infrastructure-service-group!=null")}
           (pagination/first-last-params db (conj db-path ::edges-pagination)))
      #(dispatch [::set-edges db-path %])]}))

(reg-event-db
  ::toggle-select-target
  (fn [db [_ db-path credential credentials]]
    (let [db-path-selected (conj db-path ::selected)
          selected         (get-in db db-path-selected)
          select?          (nil? (selected credential))
          op               (if select? conj disj)]
      (-> db
          (assoc-in db-path-selected (apply disj selected credentials))
          (update-in db-path-selected op credential)))))

(reg-sub
  ::selected?
  (fn [db [_ db-path credentials]]
    (->> (get-in db (conj db-path ::selected))
         (some (set credentials))
         boolean)))

(reg-sub
  ::infrastructures-with-credentials
  (fn [db [_ db-path]]
    (let [{:keys [resources]} (get-in db (conj db-path ::infrastructures))
          creds-by-parent      (->> (get-in db (conj db-path ::credentials))
                                    :resources
                                    (group-by :parent))
          {edges :resources} (get-in db (conj db-path ::edges))
          edges-by-infra-group (->> edges
                                    (map (juxt :infrastructure-service-group identity))
                                    (into {}))]
      (->> resources
           (map #(let [{:keys [id parent name description]} %
                       {edge-name  :name
                        edge-descr :description} (get edges-by-infra-group parent)]
                   (assoc % :credentials (get creds-by-parent id)
                            :name (or edge-name name)
                            :description (or edge-descr description))))
           (sort-by (juxt :name :id))))))

(defn CredentialItem
  [db-path {:keys [id name description] :as credential} credentials]
  (let [selected? @(subscribe [::selected? db-path [credential]])]
    [ui/ListItem {:style    {:cursor :pointer}
                  :on-click #(dispatch [::toggle-select-target db-path
                                        credential credentials])}
     [ui/ListIcon
      [ui/Icon {:name (if selected?
                        "check square outline"
                        "square outline")}]]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a})
       [ui/Icon {:name "key"}] " "
       (or name id)]
      (when description
        [ui/ListDescription (general-utils/truncate description 100)])]]))

(defn TargetItem
  [db-path {:keys [id name description credentials subtype] :as _infrastructure}]
  (let [selected?      (subscribe [::selected? db-path credentials])
        multiple-cred? (> (count credentials) 1)]
    [ui/ListItem (when-not multiple-cred?
                   {:style    {:cursor :pointer}
                    :disabled (-> credentials count zero?)
                    :on-click #(dispatch [::toggle-select-target db-path
                                          (first credentials)])})
     [ui/ListIcon
      [ui/Icon {:name (if @selected?
                        "check square outline"
                        "square outline")}]]
     [ui/ListContent
      [ui/ListHeader (when (and (not multiple-cred?) @selected?) {:as :a})
       (case subtype
         "swarm" [ui/Icon {:name "docker"}]
         "kubernetes" [ui/Image {:src   (if @selected?
                                          "/ui/images/kubernetes.svg"
                                          "/ui/images/kubernetes-grey.svg")
                                 :style {:width   "1.18em"
                                         :margin  "0 .25rem 0 0"
                                         :display :inline-block}}])
       " " (or name id)]
      (when description
        [ui/ListDescription (general-utils/truncate description 100)])
      (when multiple-cred?
        [ui/ListList
         (for [{:keys [id] :as credential} credentials]
           ^{:key id}
           [CredentialItem db-path credential credentials])])]]))

(defn TargetEdges
  [{:keys [db-path] :as _opts}]
  (dispatch [::search-edges db-path])
  (fn [{:keys [db-path] :as _opts}]
    (let [{:keys [count]} @(subscribe [::helpers/retrieve db-path ::edges])
          infrastructures @(subscribe [::infrastructures-with-credentials db-path])
          loading?        @(subscribe [::helpers/retrieve db-path ::targets-loading?])]
      [ui/TabPane {:loading loading?}
       [full-text-search/FullTextSearch
        {:db-path      (conj db-path ::edges-search)
         :change-event [::search-edges db-path]}]
       [ui/ListSA
        (for [{:keys [id] :as infrastructure} infrastructures]
          ^{:key id}
          [TargetItem db-path infrastructure])]
       [pagination/Pagination {:db-path      (conj db-path ::edges-pagination)
                               :total-items  count
                               :change-event [::search-edges db-path]}]])))

(defn TargetClouds
  [{:keys [db-path] :as _opts}]
  (dispatch [::search-clouds db-path])
  (fn [{:keys [db-path] :as _opts}]
    (let [{:keys [count]} @(subscribe [::helpers/retrieve db-path ::infrastructures])
          infrastructures @(subscribe [::infrastructures-with-credentials db-path])
          loading?        @(subscribe [::helpers/retrieve db-path ::targets-loading?])]
      [ui/TabPane {:loading loading?}
       [full-text-search/FullTextSearch
        {:db-path      (conj db-path ::clouds-search)
         :change-event [::search-clouds db-path]}]
       [ui/ListSA
        (for [{:keys [id] :as infrastructures} infrastructures]
          ^{:key id}
          [TargetItem db-path infrastructures])]
       [pagination/Pagination {:db-path      (conj db-path ::clouds-pagination)
                               :total-items  count
                               :change-event [::search-clouds db-path]}]])))

(defn TargetsSelectorSection
  [{:keys [db-path] :as opts}]
  [nav-tab/Tab
   {:db-path                 db-path
    :panes                   [{:menuItem {:content "Edges"
                                          :key     :edges
                                          :icon    "box"}
                               :render   #(r/as-element [TargetEdges opts])}
                              {:menuItem {:content "Clouds"
                                          :key     :clouds
                                          :icon    "cloud"}
                               :render   #(r/as-element [TargetClouds opts])}]
    :on-change-event         [::pagination/change-page (conj db-path ::pagination) 1]
    :ignore-chng-protection? true}])

(s/fdef TargetsSelectorSection
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::helpers/change-event
                                            ;::subtype
                                            ])))
