(ns sixsq.nuvla.ui.acl.views
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.acl.events :as events]
    [sixsq.nuvla.ui.acl.subs :as subs]
    [sixsq.nuvla.ui.acl.utils :as utils]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]
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


(defn AclTableHeaders
  [{:keys [mode] :as opts}]
  (if (is-advanced-mode? @mode)
    [ui/TableHeader
     [ui/TableRow
      [ui/TableHeaderCell {:row-span 2 :text-align "left"} "Rights"]
      [ui/TableHeaderCell {:col-span 3} "Edit"]
      [ui/TableHeaderCell {:col-span 3} "View"]
      [ui/TableHeaderCell {:row-span 2} "Manage"]
      [ui/TableHeaderCell {:row-span 2} "Delete"]]
     [ui/TableRow
      [ui/TableHeaderCell "acl"]
      [ui/TableHeaderCell "data"]
      [ui/TableHeaderCell "meta"]
      [ui/TableHeaderCell "acl"]
      [ui/TableHeaderCell "data"]
      [ui/TableHeaderCell "meta"]
      [ui/TableHeaderCell]]]
    [ui/TableHeader
     [ui/TableRow
      [ui/TableHeaderCell {:text-align "left"} "Rights"]
      [ui/TableHeaderCell "Edit"]
      [ui/TableHeaderCell "View"]
      [ui/TableHeaderCell "Manage"]
      [ui/TableHeaderCell "Delete"]
      [ui/TableHeaderCell]]]))


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
  [{:keys [acl on-change read-only] :as opts} principal right-kw]
  (let [checked? (boolean (some #(= % principal) (right-kw acl)))]
    [ui/Checkbox {:checked   checked?
                  :on-change #(if checked?
                                (on-change (utils/remove-principal acl (utils/extent-right right-kw) principal))
                                (on-change (utils/add-principal acl (utils/extent-right right-kw) principal)))
                  :disabled  read-only}]))


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
                   :trigger  (reagent/as-element [:span (or principal-name principal)])}]
        [:span (or principal-name principal)])]

     [:<>
      (for [right-kw (rights-for-mode @mode)]
        ^{:key (str principal right-kw)}
        [ui/TableCell [RightCheckbox opts principal right-kw]])]

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
  (let [open   (reagent/atom false)
        users  (subscribe [::subs/users-options])
        groups (subscribe [::subs/groups-options])]
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

        [ui/DropdownHeader {:icon "user" :content "Users"}]

        [ui/Input {:icon          "search"
                   :icon-position "left"
                   :name          "search"
                   :auto-complete "off"
                   :on-click      #(reset! open true)
                   :on-change     (ui-callback/input ::events/search-users)}]

        [:<>
         (doall
           (for [{user-id :id user-name :name} @users]
             ^{:key user-id}
             [ui/DropdownItem {:text     (or user-name user-id)
                               :on-click (fn []
                                           (on-change user-id)
                                           (reset! open false))}]))]

        [ui/DropdownDivider]

        [ui/DropdownHeader {:icon "users" :content "Groups"}]

        [:<>
         (doall
           (for [{group-id :id group-name :name} @groups]
             ^{:key group-id}
             [ui/DropdownItem {:text     (or group-name group-id)
                               :on-click (fn []
                                           (on-change group-id)
                                           (reset! open false))}]))]]])))


(defn AddRight
  [opts]
  (let [empty-permission {:principal nil
                          :rights    #{}}
        new-permission   (reagent/atom empty-permission)]
    (fn [{:keys [acl on-change mode] :as opts}]
      [ui/TableRow

       [ui/TableCell
        [DropdownPrincipals {:value     (:principal @new-permission)
                             :on-change #(reset! new-permission (assoc @new-permission :principal %))
                             :fluid     true}]]

       [:<>
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
                                        (update @new-permission :rights set/union (utils/extent-right right-kw)))
                                (reset! new-permission
                                        (update @new-permission :rights disj right-kw)))))}]]))]

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
        mobile? (subscribe [::main-subs/is-device? :mobile])]
    [ui/Table {:unstackable true
               :attached    "top"
               :basic       true}

     [ui/TableHeader
      [ui/TableRow
       [ui/TableHeaderCell "Owners"]]]

     [ui/TableBody
      [ui/TableRow
       [ui/TableCell
        [ui/ListSA {:horizontal (not @mobile?)
                    :relaxed    true}

         [:<>
          (for [owner owners]
            ^{:key owner}
            [OwnerItem opts (>= (count owners) 2) owner])]

         (when-not read-only
           [ui/ListItem
            [ui/ListContent
             [ui/ListHeader
              [DropdownPrincipals
               {:on-change #(on-change (utils/add-principal acl [:owners] %))
                :fluid     false}]]]])]]]]]))


(defn AclRights
  [{:keys [acl read-only] :as opts}]
  (let [principals (->> (dissoc acl :owners)
                        (mapcat (fn [[right principal]] principal))
                        (set)
                        (sort))]

    [ui/Table {:unstackable    true
               :attached       "bottom"
               :basic          true
               :text-align     "center"
               :vertical-align "middle"}

     [AclTableHeaders opts]

     [ui/TableBody

      [:<>
       (for [principal principals]
         ^{:key principal}
         [RightRow opts principal])]

      (when-not read-only
        [AddRight opts])]]))


(defn AclWidget
  [{:keys [acl read-only on-change mode]
    :or {acl {:owners [@(subscribe [::authn-subs/user-id])]}
         read-only false
         mode (reagent/atom :simple)
         on-change #()} :as opts}]
  (fn [opts]
    (let [opts         (assoc opts :mode mode)
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
       [AclRights opts]])))
