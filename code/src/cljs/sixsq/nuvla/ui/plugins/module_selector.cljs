(ns sixsq.nuvla.ui.plugins.module-selector
  (:require [cljs.spec.alpha :as s]
            [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.plugins.pagination :as pagination]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.icons :as icons]))


(s/def ::subtypes (s/nilable set?))

(defn build-spec
  [& {:keys [default-items-per-page]
      :or   {default-items-per-page 10}}]
  {::loading?   true
   ::apps       nil
   ::selected   #{}
   ::tab        (nav-tab/build-spec :default-tab :app-store)
   ::search     (full-text-search/build-spec :persistent? false)
   ::pagination (pagination/build-spec
                  :default-items-per-page default-items-per-page)})

(defn db-selected
  [db db-path]
  (get-in db (conj db-path ::selected)))

(reg-event-fx
  ::load-apps
  (fn [{{:keys [::session-spec/session] :as db} :db} [_ db-path & {:keys [loading? subtypes]}]]
    (let [subtypes-path (conj db-path ::subtypes)
          subtypes      (or subtypes (get-in db subtypes-path))
          active-tab    (nav-tab/get-active-tab db (conj db-path ::tab))
          params        (pagination/first-last-params
                          db (conj db-path ::pagination)
                          {:select  "id, name, description, parent-path, subtype"
                           :orderby "path:asc"
                           :filter  (general-utils/join-and
                                      (full-text-search/filter-text db (conj db-path ::search))
                                      (case active-tab
                                        :my-apps (str "acl/owners='" (or (:active-claim session)
                                                                         (:user session)) "'")
                                        :app-store "published=true"
                                        nil)
                                      (apply general-utils/join-or (map #(str "subtype='" % "'") subtypes))
                                      "subtype!='project'")})]
      {:db                  (cond-> db
                                    loading? (assoc-in
                                               (conj db-path ::loading?) true)
                                    (seq subtypes) (assoc-in subtypes-path subtypes))
       ::cimi-api-fx/search [:module params
                             #(dispatch [::helpers/set db-path
                                         ::apps %
                                         ::loading? false])]})))

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
         ::cimi-api-fx/search [:module {:select "id, name, description, parent-path, subtype"
                                        :filter (apply general-utils/join-or
                                                       (map #(str "id='" % "'") selected-ids))
                                        :last   10000}
                               #(dispatch [::helpers/set db-path
                                           ::selected (rebuild-selected %)
                                           ::loading? false])]})
      {:db (assoc-in db (conj db-path ::selected) #{})})))

(reg-event-db
  ::toggle-app
  (fn [db [_ db-path module]]
    (let [path-selected (conj db-path ::selected)
          selected      (get-in db path-selected)
          select?       (nil? (selected module))
          op            (if select? conj disj)]
      (update-in db path-selected op module))))

(defn transform
  [tree {:keys [parent-path] :as app}]
  (let [paths (if (str/blank? parent-path)
                [:applications]
                (-> parent-path
                    (str/split "/")
                    (conj :applications)))]
    (update-in tree paths conj app)))

(reg-sub
  ::apps-tree
  (fn [[_ db-path]]
    (subscribe [::helpers/retrieve db-path ::apps]))
  (fn [{:keys [resources]}]
    (reduce transform {} resources)))

(reg-sub
  ::selected
  (fn [db [_ db-path]]
    (get-in db (conj db-path ::selected))))

(reg-sub
  ::selected?
  (fn [db [_ db-path id]]
    (->> (get-in db (conj db-path ::selected))
         (some (comp #{id} :id))
         boolean)))

(defn Application
  [db-path {:keys [id name subtype description] :as module}]
  (let [selected? @(subscribe [::selected? db-path id])]
    [ui/ListItem {:on-click #(dispatch [::toggle-app db-path module])
                  :style    {:cursor :pointer}}
     [ui/ListIcon {:name (if selected?
                           "check square outline"
                           "square outline")}]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a})
       [apps-utils/SubtypeIconInfra subtype selected?]
       " "
       (or name id)]
      [ui/ListDescription
       (general-utils/truncate description 100)]]]))

(defn Applications
  [db-path applications]
  [:<>
   (for [{:keys [id] :as child} applications]
     ^{:key id}
     [Application db-path child])])

(declare Node)

(defn Project
  [db-path path {:keys [applications] :as content}]
  [ui/ListItem
   [ui/ListIcon {:name icons/i-folder-full}]
   [ui/ListContent
    [ui/ListHeader path]
    [ui/ListList
     [Node db-path (dissoc content :applications) applications]]]])

(defn Projects
  [db-path projects]
  [:<>
   (for [[path content] projects]
     ^{:key path}
     [Project db-path path content])])

(defn Node
  [db-path projects applications]
  [:<>
   [Projects db-path (sort-by first projects)]
   [Applications db-path (sort-by (juxt :name :id) applications)]])

(defn AppsSelectorSection
  [{:keys [db-path subtypes] :as _opts}]
  (dispatch [::load-apps db-path :subtypes subtypes])
  (fn [{:keys [db-path] :as _opts}]
    (let [{:keys [count]} @(subscribe [::helpers/retrieve db-path ::apps])
          apps     @(subscribe [::apps-tree db-path])
          loading? @(subscribe [::helpers/retrieve db-path ::loading?])
          tr       @(subscribe [::i18n-subs/tr])
          render   (fn []
                     (r/as-element
                       [ui/TabPane {:loading loading?}
                        [full-text-search/FullTextSearch
                         {:db-path      (conj db-path ::search)
                          :change-event [::load-apps db-path]}]
                        [ui/ListSA
                         [Node db-path (dissoc apps :applications) (:applications apps)]]
                        [pagination/Pagination
                         {:db-path      (conj db-path ::pagination)
                          :total-items  count
                          :change-event [::load-apps db-path]}]]))]
      [nav-tab/Tab
       {:db-path                 (conj db-path ::tab)
        :panes                   [{:menuItem {:content (general-utils/capitalize-words (tr [:appstore]))
                                              :key     :app-store
                                              :icon    (r/as-element [ui/Icon {:className "fas fa-store"}])}
                                   :render   render}
                                  {:menuItem {:content (general-utils/capitalize-words (tr [:all-apps]))
                                              :key     :all-apps
                                              :icon    icons/i-grid-layout}
                                   :render   render}
                                  {:menuItem {:content (general-utils/capitalize-words (tr [:my-apps]))
                                              :key     :my-apps
                                              :icon    "user"}
                                   :render   render}]
        :change-event            [::pagination/change-page (conj db-path ::pagination) 1]
        :ignore-chng-protection? true}])))

(s/fdef AppsSelectorSection
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path]
                                   :opt-un [::helpers/change-event
                                            ::subtypes])))
