(ns sixsq.nuvla.ui.acl.views
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.accordion :as accordion-utils]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn is-advanced-mode?
  [mode]
  (= mode :advanced))


(defn rights-for-mode
  [mode]
  (if (is-advanced-mode? mode)
    utils/all-defined-rights
    utils/subset-defined-rights))


(defn InfoIcon
  [help-kw]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Popup {:trigger (r/as-element
                          [ui/Icon {:name "info circle", :link true}])
               :basic   true
               :content (@tr [help-kw])}]))


(defn AclTableHeaders
  [{:keys [mode] :as _opts}]
  (let [tr                (subscribe [::i18n-subs/tr])
        border-left-style {:style {:border-left "1px solid rgba(34,36,38,.1)"}}]
    (if (is-advanced-mode? @mode)
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell {:row-span 2 :text-align "left"} (str/capitalize (@tr [:rights]))]
        [ui/TableHeaderCell (merge border-left-style {:col-span 3})
         (str/capitalize (@tr [:edit]))
         [InfoIcon :acl-rights-edit]]
        [ui/TableHeaderCell (merge border-left-style {:col-span 3})
         (str/capitalize (@tr [:view]))
         [InfoIcon :acl-rights-view]]
        [ui/TableHeaderCell (merge border-left-style {:row-span 2})
         (str/capitalize (@tr [:manage]))
         [InfoIcon :acl-rights-manage]]
        [ui/TableHeaderCell (merge border-left-style {:row-span 2})
         (str/capitalize (@tr [:delete]))
         [InfoIcon :acl-rights-delete]]
        [ui/TableHeaderCell {:row-span 2}]]
       [ui/TableRow
        [ui/TableHeaderCell border-left-style "Acl" [InfoIcon :acl-rights-edit-acl]]
        [ui/TableHeaderCell "Data" [InfoIcon :acl-rights-edit-data]]
        [ui/TableHeaderCell "Meta" [InfoIcon :acl-rights-edit-meta]]
        [ui/TableHeaderCell border-left-style "Acl" [InfoIcon :acl-rights-view-acl]]
        [ui/TableHeaderCell "Data" [InfoIcon :acl-rights-view-data]]
        [ui/TableHeaderCell "Meta" [InfoIcon :acl-rights-view-meta]]]]
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell {:text-align "left"} (str/capitalize (@tr [:rights]))]
        [ui/TableHeaderCell
         (str/capitalize (@tr [:edit]))
         [InfoIcon :acl-rights-edit]]
        [ui/TableHeaderCell
         (str/capitalize (@tr [:view]))
         [InfoIcon :acl-rights-view]]
        [ui/TableHeaderCell
         (str/capitalize (@tr [:manage]))
         [InfoIcon :acl-rights-manage]]
        [ui/TableHeaderCell
         (str/capitalize (@tr [:delete]))
         [InfoIcon :acl-rights-delete]]
        [ui/TableHeaderCell]]])))


(defn PrincipalIcon
  [principal]
  [ui/Icon {:name (utils/id->icon principal)}])


(defn OwnerItem
  [{:keys [on-change]} ui-acl removable? principal]
  (let [principal-name (subscribe [::session-subs/resolve-principal principal])]
    [ui/ListItem {:style {:vertical-align "middle"}}
     [ui/ListContent
      [ui/ListHeader
       [PrincipalIcon principal]
       ff/nbsp
       @principal-name
       ff/nbsp
       (when removable?
         [ui/Icon {:name     "close"
                   :link     true
                   :size     "small"
                   :color    "red"
                   :on-click #(do
                                (swap! ui-acl utils/acl-remove-owner principal)
                                (on-change (utils/ui-acl-format->acl @ui-acl)))}])]]]))


(defn RightCheckbox
  [{:keys [on-change read-only mode] :as _opts} ui-acl row-number principal rights right-kw]
  (let [checked?       (contains? rights right-kw)
        indeterminate? (and
                         (= @mode :simple)
                         (not checked?)
                         #_:clj-kondo/ignore
                         (not (empty? (set/intersection rights (set (utils/same-base-right right-kw))))))]
    [ui/Checkbox {:checked       checked?
                  :indeterminate indeterminate?
                  :on-change     #(let [new-rights (if checked?
                                                     (->> (set/difference
                                                            rights
                                                            (utils/same-base-right right-kw))
                                                          (map utils/extent-right)
                                                          (apply concat)
                                                          set)
                                                     (apply conj rights (utils/extent-right right-kw)))]
                                    (swap! ui-acl utils/acl-change-rights-for-row row-number principal new-rights)
                                    (on-change (utils/ui-acl-format->acl @ui-acl)))
                  :disabled      read-only}]))


(defn RightRow
  [{:keys [on-change read-only mode] :as opts} ui-acl row-number principal rights]
  (let [principal-name (subscribe [::session-subs/resolve-principal principal])]
    [ui/TableRow

     [ui/TableCell {:text-align "left"}
      [PrincipalIcon principal]
      ff/nbsp
      (if @principal-name
        [ui/Popup {:content  principal
                   :position "right center"
                   :trigger  (r/as-element [:span @principal-name])}]
        [:span @principal-name])]

     (for [right-kw (rights-for-mode @mode)]
       ^{:key (str principal right-kw)}
       [ui/TableCell [RightCheckbox opts ui-acl row-number principal rights right-kw]])

     [ui/TableCell
      (when-not read-only
        [ui/Icon {:link     true
                  :name     "trash"
                  :color    "red"
                  :on-click #(do
                               (swap! ui-acl utils/acl-remove-principle-from-rights principal)
                               (on-change (utils/ui-acl-format->acl @ui-acl)))}])]]))


(defn DropdownPrincipals
  [_opts _ui-acl]
  (let [tr          (subscribe [::i18n-subs/tr])
        add-item    (r/atom nil)
        on-add-item (ui-callback/value
                      #(reset! add-item {:key   %
                                         :value %
                                         :text  %
                                         :icon  (utils/id->icon %)}))
        search      (fn [opts query-search]
                      (->> opts
                           (filter #(let [pattern (re-pattern
                                                    (str "(?i).*" (general-utils/regex-escape query-search) ".*"))]
                                      (or (re-matches pattern (.-text %))
                                          (re-matches pattern (.-value %)))))
                           clj->js))]
    (fn [{:keys [on-change fluid]
          :or   {on-change #()
                 fluid     false}}
         ui-acl]
      (let [used-principals (utils/acl-get-all-principals-set @ui-acl)
            options         (subscribe [::session-subs/peers-groups-options used-principals])]
        [ui/Dropdown {:fluid           fluid
                      :selection       true
                      :style           {:width "350px"}
                      :upward          false
                      :options         (cond-> @options @add-item (conj @add-item))
                      :search          search
                      :allow-additions true
                      :addition-label  (@tr [:add-by-user-group-id])
                      :on-add-item     on-add-item
                      :on-change       (ui-callback/value on-change)}]))))

(defn AddRight
  [{:keys [on-change _mode] :as _opts} ui-acl]
  (let [empty-permission {:principal nil
                          :right     nil}
        new-permission   (r/atom empty-permission)
        update-acl       #(let [{:keys [principal right]} @new-permission]
                            (when (and (some? right)
                                       (some? principal))
                              (swap! ui-acl utils/acl-add-principal-with-right principal right)
                              (on-change (utils/ui-acl-format->acl @ui-acl))
                              (reset! new-permission empty-permission)))]
    (fn [{:keys [_on-change mode] :as _opts} ui-acl]

      [ui/TableRow

       [ui/TableCell
        [DropdownPrincipals {:value     (:principal @new-permission)
                             :on-change #(do
                                           (swap! new-permission assoc :principal %)
                                           (update-acl))
                             :fluid     true} ui-acl]]

       (doall
         (for [right-kw (rights-for-mode @mode)]
           ^{:key right-kw}
           [ui/TableCell
            [ui/Checkbox
             {:checked   (= (:right @new-permission) right-kw)
              :on-change #(do
                            (swap! new-permission assoc :right right-kw)
                            (update-acl))}]]))
       [ui/TableCell]])))


(defn AclOwners
  [_opts _ui-acl]
  (let [mobile? (subscribe [::main-subs/is-mobile-device?])
        tr      (subscribe [::i18n-subs/tr])]
    (fn [{:keys [read-only on-change mode] :as opts} ui-acl]
      (let [is-advanced? (is-advanced-mode? @mode)
            owners       (utils/acl-get-owners-set @ui-acl)]
        [ui/Table {:unstackable true
                   :attached    "top"}
         [ui/TableHeader
          [ui/TableRow
           [ui/TableHeaderCell
            (str/capitalize (@tr [:owners]))
            [InfoIcon :acl-owners]

            [ui/Icon {:name     (if is-advanced? "compress" "expand")
                      :style    {:float "right"}
                      :link     true
                      :on-click #(reset! mode (if is-advanced? :simple :advanced))}]]]]

         [ui/TableBody
          [ui/TableRow
           [ui/TableCell
            [ui/ListSA {:horizontal (not @mobile?)
                        :relaxed    true}

             (for [owner owners]
               ^{:key owner}
               [OwnerItem opts ui-acl (and (not read-only) (>= (count owners) 2)) owner])

             (when-not read-only
               [ui/ListItem {:style {:vertical-align "middle"}}
                [ui/ListContent
                 [ui/ListHeader
                  [DropdownPrincipals
                   {:on-change (fn [new-owner]
                                 (when-not (contains? owners new-owner)
                                   (swap! ui-acl utils/acl-add-owner new-owner)
                                   (on-change (utils/ui-acl-format->acl @ui-acl))))
                    :fluid     false} ui-acl]]]])]]]]]))))


(defn AclRights
  [{:keys [read-only] :as opts} ui-acl]
  (let [principals (:principals @ui-acl)]
    [ui/Table {:unstackable    true
               :attached       "bottom"
               :text-align     "center"
               :vertical-align "middle"}

     [AclTableHeaders opts]

     [ui/TableBody

      (for [[row-number [principal rights]] (map-indexed vector principals)]
        ^{:key principal}
        [RightRow opts ui-acl row-number principal rights])

      (when-not read-only
        [AddRight opts ui-acl])]]))


(defn ->acl
  [default-value can-edit?]
  (or default-value
      (when-let [user-id (and can-edit?
                              @(subscribe [::session-subs/active-claim]))]
        {:owners [user-id]})))


(defn ->ui-acl
  [default-value can-edit?]
  (let [acl (->acl default-value can-edit?)]
    (when acl (r/atom (utils/acl->ui-acl-format acl)))))


(defn AclWidget
  [{:keys [default-value read-only mode] :as _opts} & [ui-acl]]
  (let [mode       (r/atom (or mode :simple))
        ui-acl     (or ui-acl
                       (->ui-acl default-value (not read-only)))
        is-mobile? (subscribe [::main-subs/is-mobile-device?])]
    (fn [{:keys [on-change read-only] :as opts}]
      (let [opts (assoc opts :mode mode
                             :read-only (or (nil? read-only) read-only)
                             :on-change (or on-change #()))]

        [:div
         {:style (cond-> {:margin-bottom "10px"
                          :margin-top    "10px"}
                         @is-mobile? (assoc :overflow-x "auto"))}
         [AclOwners opts ui-acl]
         (when (or (not read-only)
                   (not (utils/acl-rights-empty? @ui-acl)))
           [AclRights opts ui-acl])]))))


(defn TabAcls
  [e can-edit? edit-event]
  (let [tr            (subscribe [::i18n-subs/tr])
        default-value (:acl @e)
        ui-acl        (->ui-acl default-value can-edit?)]
    {:menuItem {:content "Share"
                :key     :share
                :icon    "users"}
     :render   (fn []
                 (r/as-element
                   (when default-value
                     ^{:key (:updated @e)}
                     [AclWidget {:default-value default-value
                                 :read-only     (not can-edit?)
                                 :on-change     #(dispatch
                                                   [edit-event
                                                    (:id @e) (assoc @e :acl %)
                                                    (@tr [:acl-updated])])}
                      ui-acl])))}))


(defn AclButtonOnly
  [{:keys [default-value read-only active?] :as _opts}]
  (let [tr     (subscribe [::i18n-subs/tr])
        ui-acl (->ui-acl default-value (not read-only))]
    (fn [_opts]
      (when ui-acl
        (let [owners          (utils/acl-get-owners-set @ui-acl)
              principals-set  (utils/acl-get-all-principals-set @ui-acl)
              some-groups?    (some #(str/starts-with? % "group/") principals-set)

              icon-principals (cond
                                (and owners (= (count owners) 1) (empty? principals-set)) "lock"
                                (contains? principals-set "group/nuvla-anon") "world"
                                some-groups? "users"
                                (not some-groups?) "user"
                                :else nil)
              rights-keys     (utils/acl-get-all-used-rights-set @ui-acl)
              icon-right      (cond
                                (some #(str/starts-with? (name %) "edit") rights-keys) "pencil"
                                (some #(str/starts-with? (name %) "view") rights-keys) "eye"
                                :else nil)]
          [:<>
           [ui/Button {:floated  "right"
                       :style    {:margin-bottom "5px"}
                       :basic    true
                       :on-click #(accordion-utils/toggle active?)}
            [ui/Popup {:trigger  (r/as-element [ui/Icon {:name icon-principals}])
                       :position "bottom center"
                       :content  (@tr [:access-rights])}]
            (when icon-right
              [ui/Popup {:trigger  (r/as-element [ui/Icon {:name icon-right}])
                         :position "bottom center"
                         :content  (@tr [:access-rights])}])
            [ui/Icon {:name (if @active? "caret down" "caret left")}]]])))))


(defn AclSection
  [{:keys [default-value read-only active?] :as _opts}]
  (let [ui-acl (->ui-acl default-value (not read-only))]
    (fn [opts]
      (when (and ui-acl @active?)
        [AclWidget opts ui-acl]))))


(defn AclButton
  [{:keys [default-value read-only default-active?] :as _opts}]
  (let [active? (r/atom default-active?)
        ui-acl  (->ui-acl default-value (not read-only))]
    (fn [opts]
      (when ui-acl
        [:<>
         [AclButtonOnly (assoc opts :active? active?)]
         (when @active?
           [AclWidget opts ui-acl])]))))
