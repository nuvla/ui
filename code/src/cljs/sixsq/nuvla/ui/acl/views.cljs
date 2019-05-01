(ns sixsq.nuvla.ui.acl.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.acl.events :as events]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.acl.utils :as utils]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.acl.subs :as subs]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [clojure.set :as set]))


(defn acl-table-headers
  []
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
    [ui/TableHeaderCell]]])


(defn principal-icon
  [principal]
  [ui/Icon {:name (if (str/starts-with? principal "user/")
                    "user"
                    "users")}])


(defn owner-item
  [acl on-change removable? principal]
  ^{:key (str "owner_" principal)}
  (let [principal-name @(subscribe [::subs/principal-name principal])]
    [ui/ListItem
     [ui/ListContent
      [ui/ListHeader
       [principal-icon principal]
       ff/nbsp
       (or principal-name principal)
       ff/nbsp
       (when removable?
         [ui/Icon {:name     "close"
                   :link     true
                   :size     "small"
                   :color    "red"
                   :on-click #(on-change (utils/remove-principal acl [:owners] principal))}])]]]))


(defn right-checkbox
  [acl on-change read-only principal right-kw]
  (let [checked? (boolean (some #(= % principal) (right-kw acl)))]
    [ui/Checkbox {:checked   checked?
                  :on-change #(if checked?
                                (on-change (utils/remove-principal acl [right-kw] principal))
                                (on-change (utils/add-principal acl (utils/extent-right right-kw) principal)))
                  :disabled  read-only}]))


(defn principal-row
  [acl on-change read-only principal]
  ^{:key (str "right_" principal)}
  (let [principal-name @(subscribe [::subs/principal-name principal])]
    [ui/TableRow
     [ui/TableCell {:text-align "left"}
      [principal-icon principal]
      ff/nbsp
      (if principal-name
        [ui/Popup {:content  principal
                   :position "right center"
                   :trigger  (reagent/as-element [:span (or principal-name principal)])}]
        [:span (or principal-name principal)])]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :edit-acl]]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :edit-data]]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :edit-meta]]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :view-acl]]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :view-data]]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :view-meta]]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :manage]]
     [ui/TableCell
      [right-checkbox acl on-change read-only principal :delete]]
     [ui/TableCell
      (when-not read-only
        [ui/Icon {:link     true
                  :name     "trash"
                  :color    "red"
                  :on-click #(on-change (utils/remove-principal
                                          acl
                                          utils/all-defined-rights
                                          principal))}])]]))


(defn dropdown-principals
  [opts]
  (let [open (reagent/atom false)
        selected (reagent/atom nil)
        users (subscribe [::subs/users-options])
        groups (subscribe [::subs/groups-options])]
    (dispatch [::events/search-groups])
    (dispatch [::events/search-users ""])
    (fn [{:keys [on-change fluid]
          :or   {on-change #()
                 fluid     false}}]
      ^{:key (str "additional-right-for-" @selected)}
      [ui/Dropdown {:text      @selected
                    :fluid     fluid
                    :on-open   #(reset! open true)
                    :open      @open
                    :className "selection"
                    :on-blur   #(reset! open false)
                    :on-close  #()}
       (vec (concat
              [ui/DropdownMenu]

              [[ui/DropdownHeader {:icon "user" :content "Users"}]
               [ui/Input {:icon          "search"
                          :icon-position "left"
                          :name          "search"
                          :auto-complete "off"
                          :on-change     (ui-callback/input ::events/search-users)}]]

              (map
                (fn [{user-id :id user-name :name}]
                  [ui/DropdownItem {:text     (or user-name user-id)
                                    :on-click (fn []
                                                (on-change user-id)
                                                (reset! selected (or user-name user-id))
                                                (reset! open false))}]) @users)

              [[ui/DropdownDivider]
               [ui/DropdownHeader {:icon "users" :content "Groups"}]]

              (map
                (fn [{user-id :id user-name :name}]
                  [ui/DropdownItem {:text     (or user-name user-id)
                                    :on-click (fn []
                                                (on-change user-id)
                                                (reset! selected (or user-name user-id))
                                                (reset! open false))}]) @groups)

              ))
       ])))


(defn additional-right
  [acl on-change]
  (let [new-permission (reagent/atom {:principal nil
                                      :rights    #{}})]
    (fn [acl on-change]
      (vec (concat
             [ui/TableRow]

             [[ui/TableCell
               [dropdown-principals {:on-change #(reset! new-permission (assoc @new-permission :principal %))
                                     :fluid     true}]]]

             (map
               (fn [right-kw]
                 [ui/TableCell
                  [ui/Checkbox
                   {:checked   (contains? (:rights @new-permission) right-kw)
                    :on-change (ui-callback/checked
                                 (fn [checked]
                                   (if checked
                                     (reset! new-permission
                                             (update @new-permission :rights set/union (utils/extent-right right-kw)))
                                     (reset! new-permission
                                             (update @new-permission :rights disj right-kw)))

                                   ))}]])
               utils/all-defined-rights)

             [[ui/TableCell
               (let [{:keys [principal rights]} @new-permission]
                 (when (and principal (not-empty rights))
                   [ui/Icon {:name     "plus"
                             :link     true
                             :color    "green"
                             :on-click #(on-change (utils/add-principal acl rights principal))
                             }]))]])))))



(defn acl-table
  [{:keys [acl] :as opt}]
  (fn [{:keys [acl read-only on-change]
        :or   {acl       {:owners [@(subscribe [::authn-subs/user-id])]}
               read-only false
               on-change #()}}]
    (let [principals (->> (dissoc acl :owners)
                          (mapcat (fn [[right principal]] principal))
                          (set)
                          (sort))
          owners (:owners acl)]
      [:div
       [ui/Header {:as "h5" :attached "top"} "Owners"]
       [ui/Segment {:attached true}
        (vec (concat [ui/ListSA {:horizontal true}]
                     (map (partial owner-item acl on-change (pos-int? (dec (count owners)))) owners)
                     (when-not read-only
                       [[ui/ListItem
                         [ui/ListContent
                          [ui/ListHeader
                           [dropdown-principals
                            {:on-change #(on-change (utils/add-principal acl [:owners] %))
                             :fluid     false}]
                           ]]]])))]
       [ui/Segment (merge {:attached true} style/autoscroll-x)
        [ui/Segment {:basic true}
         [ui/Table {:unstackable true :basic "very" :text-align "center" :vertical-align "middle"}
          [acl-table-headers]

          (vec (concat [ui/TableBody]
                       (map (partial principal-row acl on-change read-only) principals)

                       (when-not read-only
                         [[additional-right acl on-change]])))]]]])))
