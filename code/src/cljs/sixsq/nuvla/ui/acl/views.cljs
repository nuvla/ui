(ns sixsq.nuvla.ui.acl.views
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.events :as events]
    [sixsq.nuvla.ui.acl.subs :as subs]
    [sixsq.nuvla.ui.acl.utils :as utils]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


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
    [ui/Popup {:trigger  (r/as-element
                           [ui/Icon {:name  "info circle"
                                     :style {:cursor      "pointer"
                                             :margin-left "4px"}}])
               :content  (@tr [help-kw])
               :on       "click"
               :position "top left"}]))


(defn AclTableHeaders
  [{:keys [mode] :as opts}]
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
  [ui/Icon {:name (if (str/starts-with? principal "user/")
                    "user"
                    "users")}])


(defn OwnerItem
  [{:keys [acl on-change]} removable? principal]
  (let [principal-name @(subscribe [::subs/principal-name principal])]
    [ui/ListItem
     [ui/ListContent
      [ui/ListHeader
       [PrincipalIcon principal]
       ff/nbsp
       (or principal-name principal)
       ff/nbsp
       (when removable?
         [ui/Icon {:name     "close"
                   :link     true
                   :size     "small"
                   :color    "red"
                   :on-click #(on-change (utils/remove-principal acl [:owners] principal))}])]]]))


(defn RightCheckbox
  [{:keys [acl on-change read-only mode] :as opts} principal right-kw]
  (let [checked?       (utils/some-principal? principal (right-kw acl))
        indeterminate? (and
                         (= @mode :simple)
                         (not checked?)
                         (utils/some-principal? principal
                                                (->> (utils/same-base-right right-kw)
                                                     (select-keys acl)
                                                     (mapcat (fn [[_ principals]] principals)))))]
    [ui/Checkbox {:checked       checked?
                  :indeterminate indeterminate?
                  :on-change     #(if checked?
                                    (on-change (utils/remove-principal acl (utils/same-base-right right-kw) principal))
                                    (on-change (utils/add-principal acl [right-kw] principal)))
                  :disabled      read-only}]))


(defn RightRow
  [{:keys [acl on-change read-only mode] :as opts} principal]
  (let [principal-name @(subscribe [::subs/principal-name principal])]
    [ui/TableRow

     [ui/TableCell {:text-align "left"}
      [PrincipalIcon principal]
      ff/nbsp
      (if principal-name
        [ui/Popup {:content  principal
                   :position "right center"
                   :trigger  (r/as-element [:span (or principal-name principal)])}]
        [:span (or principal-name principal)])]

     (for [right-kw (rights-for-mode @mode)]
       ^{:key (str principal right-kw)}
       [ui/TableCell [RightCheckbox opts principal right-kw]])

     [ui/TableCell
      (when-not read-only
        [ui/Icon {:link     true
                  :name     "trash"
                  :color    "red"
                  :on-click #(on-change (utils/remove-principal
                                          acl
                                          utils/all-defined-rights
                                          principal))}])]]))


(defn DropdownPrincipals
  [opts]
  (let [open   (r/atom false)
        users  (subscribe [::subs/users-options])
        groups (subscribe [::subs/groups-options])
        tr     (subscribe [::i18n-subs/tr])]
    (dispatch [::events/search-groups])
    (dispatch [::events/search-users ""])
    (fn [{:keys [on-change fluid value]
          :or   {on-change #()
                 fluid     false
                 value     nil}}]
      [ui/Dropdown {:text      (or (when value @(subscribe [::subs/principal-name value]))
                                   value)
                    :fluid     fluid
                    :style     {:width "250px"}
                    :on-open   #(reset! open true)
                    :open      @open
                    :upward    false
                    :className "selection"
                    :on-blur   #(reset! open false)
                    :on-close  #()}

       [ui/DropdownMenu {:style {:overflow-x "auto"
                                 :min-height "250px"}}

        [ui/DropdownHeader {:icon "user", :content (str/capitalize (@tr [:users]))}]

        [ui/Input {:icon          "search"
                   :icon-position "left"
                   :name          "search"
                   :auto-complete "off"
                   :on-click      #(reset! open true)
                   :on-change     (ui-callback/input ::events/search-users)}]

        (doall
          (for [{user-id :id user-name :name} @users]
            ^{:key user-id}
            [ui/DropdownItem {:text     (or user-name user-id)
                              :on-click (fn []
                                          (on-change user-id)
                                          (reset! open false))}]))

        [ui/DropdownDivider]

        [ui/DropdownHeader {:icon "users", :content (str/capitalize (@tr [:groups]))}]

        (doall
          (for [{group-id :id group-name :name} @groups]
            ^{:key group-id}
            [ui/DropdownItem {:text     (or group-name group-id)
                              :on-click (fn []
                                          (on-change group-id)
                                          (reset! open false))}]))]])))


(defn AddRight
  [opts]
  (let [empty-permission {:principal nil
                          :rights    #{}}
        new-permission   (r/atom empty-permission)]
    (fn [{:keys [acl on-change mode] :as opts}]
      [ui/TableRow

       [ui/TableCell
        [DropdownPrincipals {:value     (:principal @new-permission)
                             :on-change #(reset! new-permission (assoc @new-permission :principal %))
                             :fluid     true}]]

       (doall
         (for [right-kw (rights-for-mode @mode)]
           ^{:key right-kw}
           [ui/TableCell
            [ui/Checkbox
             {:checked   (contains? (:rights @new-permission) right-kw)
              :on-change (ui-callback/checked
                           (fn [checked]
                             (if checked
                               (reset! new-permission
                                       (update @new-permission :rights
                                               set/union (utils/extent-right right-kw)))
                               (reset! new-permission
                                       (update @new-permission :rights
                                               set/difference (utils/same-base-right right-kw))))))}]]))

       [ui/TableCell
        (let [{:keys [principal rights]} @new-permission]
          (when (and principal (not-empty rights))
            [ui/Icon {:name     "plus"
                      :link     true
                      :color    "green"
                      :on-click #(do
                                   (on-change (utils/add-principal acl rights principal))
                                   (reset! new-permission empty-permission))}]))]])))


(defn AclOwners
  [{:keys [acl read-only on-change] :as opts}]
  (let [owners  (:owners acl)
        mobile? (subscribe [::main-subs/is-device? :mobile])
        tr      (subscribe [::i18n-subs/tr])]
    [ui/Table {:unstackable true
               :attached    "top"}

     [ui/TableHeader
      [ui/TableRow
       [ui/TableHeaderCell
        (str/capitalize (@tr [:owners]))
        [InfoIcon :acl-owners]
        ]]]

     [ui/TableBody
      [ui/TableRow
       [ui/TableCell
        [ui/ListSA {:horizontal (not @mobile?)
                    :relaxed    true}

         (for [owner owners]
           ^{:key owner}
           [OwnerItem opts (>= (count owners) 2) owner])

         (when-not read-only
           [ui/ListItem
            [ui/ListContent
             [ui/ListHeader
              [DropdownPrincipals
               {:on-change #(on-change (utils/add-principal acl [:owners] %))
                :fluid     false}]]]])]]]]]))


(defn AclRights
  [{:keys [acl read-only] :as opts}]
  (let [rights-principals (-> acl (dissoc :owners) utils/get-principals sort)]
    [ui/Table {:unstackable    true
               :attached       "bottom"
               :text-align     "center"
               :vertical-align "middle"}

     [AclTableHeaders opts]

     [ui/TableBody

      (for [principal rights-principals]
        ^{:key principal}
        [RightRow opts principal])

      (when-not read-only
        [AddRight opts])]]))


(defn AclWidget
  [{:keys [acl read-only on-change mode]
    :or {acl {:owners [@(subscribe [::authn-subs/user-id])]}
         read-only true
         mode (r/atom :simple)
         on-change #()} :as opts}]
  (fn [opts]
    (let [opts         (assoc opts :mode mode
                                   :read-only read-only
                                   :on-change on-change)
          acl          (or (:acl opts) acl)
          is-advanced? (is-advanced-mode? @mode)]
      [:div (when @(subscribe [::main-subs/is-device? :mobile])
              {:style {:overflow-x "auto"}})
       [ui/Icon {:link     true
                 :name     (if is-advanced? "compress" "expand")
                 :style    {:float    "right"
                            :top      "28px"
                            :right    "10px"
                            :position "relative"}
                 :on-click #(reset! mode (if is-advanced? :simple :advanced))}]
       [AclOwners opts]
       (when-not (and read-only
                      (< (count acl) 2))
         [AclRights opts])])))


(defn AclButton
  [{:keys [acl on-click]
    :or {acl {:owners [@(subscribe [::authn-subs/user-id])]}
         on-click #()} :as opts}]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [opts]
      (let [opts            (assoc opts :on-click on-click)
            acl             (or (:acl opts) acl)
            owners          (:owners acl)
            principals-set  (utils/get-principals acl)
            some-groups?    (some #(str/starts-with? % "group/") principals-set)

            icon-principals (cond
                              (and owners (= (count owners) 1) (= (count acl) 1)) "lock"
                              (contains? principals-set "group/nuvla-anon") "world"
                              some-groups? "users"
                              (not some-groups?) "user"
                              :else nil)
            rights-keys     (keys acl)
            icon-right      (cond
                              (some #(str/starts-with? (name %) "edit") rights-keys) "pencil"
                              (some #(str/starts-with? (name %) "view") rights-keys) "eye"
                              :else nil)]
        [ui/Button {:floated  "right",
                    :basic    true
                    :on-click on-click}
         [ui/Popup {:trigger  (r/as-element [ui/Icon {:name icon-principals}])
                    :position "bottom center"
                    :content  (@tr [:principals-icon])}]
         (when icon-right
           [ui/Popup {:trigger  (r/as-element [ui/Icon {:name icon-right}])
                      :position "bottom center"
                      :content  (@tr [:rights-icon])}])
         [ui/Icon {:name "caret down"}]]))))