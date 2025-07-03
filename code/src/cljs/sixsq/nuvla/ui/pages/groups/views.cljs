(ns sixsq.nuvla.ui.pages.groups.views
  (:require ["@stripe/react-stripe-js" :as react-stripe]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.acl.views :as acl-views]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.profile.events :as events]
            [sixsq.nuvla.ui.pages.profile.subs :as subs]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(def group-changed! (r/atom {}))
(defn set-group-changed! [id] (swap! group-changed! assoc id true))
(defn disable-changes-protection!
  [id]
  (swap! group-changed! assoc id false)
  (when-not (some true? (vals @group-changed!))
    (dispatch [::main-events/reset-changes-protection])))

(defn GroupMember
  [id principal members editable?]
  (let [principal-name (subscribe [::session-subs/resolve-principal principal])]
    [ui/ListItem
     [ui/ListContent
      [ui/ListHeader
       [acl-views/PrincipalIcon principal]
       utils-general/nbsp
       @principal-name
       utils-general/nbsp
       (when editable?
         [icons/CloseIcon {:link     true
                           :size     "small"
                           :color    "red"
                           :on-click (fn []
                                       (reset! members (-> @members set (disj principal) vec))
                                       (dispatch [::main-events/changes-protection? true])
                                       (set-group-changed! id))}])]]]))

(defn DropdownPrincipals
  [_add-user _opts _members]
  (let [peers      (subscribe [::session-subs/peers-options])
        peers-opts (r/atom @peers)]
    (fn [add-user
         {:keys [fluid placeholder]
          :or   {fluid       false
                 placeholder ""}}
         members]
      (let [used-principals (set members)]
        [ui/Dropdown {:placeholder     placeholder
                      :fluid           fluid
                      :allow-additions true
                      :on-add-item     (ui-callback/value
                                         #(swap! peers-opts conj {:key %, :value %, :text %}))
                      :search          true
                      :value           @add-user
                      :on-change       (ui-callback/value #(reset! add-user %))
                      :options         (remove
                                         #(used-principals (:key %))
                                         @peers-opts)
                      :selection       true
                      :style           {:width "250px"}
                      :upward          false}]))))

(defn GroupMembers
  [group]
  (let [tr          (subscribe [::i18n-subs/tr])
        editable?   (utils-general/editable? group false)
        users       (:users group)
        members     (r/atom users)
        acl         (r/atom (:acl group))
        changed?    (r/cursor group-changed! [(:id group)])
        show-acl?   (r/atom false)
        invite-user (r/atom nil)
        add-user    (r/atom nil)]
    (fn [group]
      (let [{:keys [id name description]} group]
        [ui/Table {:columns 4}
         [ui/TableHeader {:fullWidth true}
          [ui/TableRow
           [ui/TableHeaderCell
            [ui/HeaderSubheader {:as :h3}
             name " (" id ")"]
            (when description [:p description])]
           (when (and @acl editable?)
             [ui/TableHeaderCell
              [acl-views/AclButtonOnly {:default-value @acl
                                        :read-only     (not editable?)
                                        :active?       show-acl?}]])]
          (when @show-acl?
            [ui/TableRow
             [ui/TableCell {:colSpan 4}
              [acl-views/AclSection {:default-value @acl
                                     :read-only     (not editable?)
                                     :active?       show-acl?
                                     :on-change     #(do
                                                       (reset! acl %)
                                                       (set-group-changed! id)
                                                       (dispatch [::main-events/changes-protection? true]))}]]])]
         [ui/TableBody
          [ui/TableRow
           [ui/TableCell
            (if (empty? @members)
              [uix/MsgNoItemsToShow [uix/TR (if editable? :empty-group-message
                                                          :empty-group-or-no-access-message)]]
              [ui/ListSA
               (for [m @members]
                 ^{:key m}
                 [GroupMember id m members editable?])])]]
          (when editable?
            [ui/TableRow
             [ui/TableCell
              [:div {:style {:display "flex"}}
               [DropdownPrincipals
                add-user
                {:placeholder (@tr [:add-group-members])
                 :fluid       true} @members]
               [:span utils-general/nbsp]
               [uix/Button {:text     (@tr [:add])
                            :icon     "add user"
                            :disabled (str/blank? @add-user)
                            :on-click #(do
                                         (swap! members conj @add-user)
                                         (reset! add-user nil)
                                         (set-group-changed! id)
                                         (dispatch [::main-events/changes-protection? true]))}]
               [:span utils-general/nbsp]
               [:span utils-general/nbsp]
               [ui/Input {:placeholder (@tr [:invite-by-email])
                          :style       {:width "250px"}
                          :value       (or @invite-user "")
                          :on-change   (ui-callback/value #(reset! invite-user %))}]
               [:span utils-general/nbsp]
               [uix/Button {:text     (@tr [:send])
                            :icon     "send"
                            :disabled (str/blank? @invite-user)
                            :on-click #(do
                                         (dispatch [::events/invite-to-group id @invite-user])
                                         (reset! invite-user nil))}]]]
             [ui/TableCell {:textAlign "right"}
              [uix/Button {:primary  true
                           :text     (@tr [:save])
                           :icon     "save"
                           :disabled (not @changed?)
                           :on-click #(do (dispatch [::events/edit-group (assoc group :users @members, :acl @acl)])
                                          (disable-changes-protection! id))}]]])]]))))

(defn GroupMembersSegment
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading? :group])
        group    (subscribe [::subs/group])]
    (dispatch [::events/get-group])
    (fn []
      [ui/Segment {:padded  true
                   :color   "green"
                   :loading @loading?
                   :style   {:height "100%"}}
       [ui/Header {:as :h2 :dividing true} (@tr [:group-members])]
       ^{:key (random-uuid)}
       [GroupMembers @group]])))

(def selected-group (r/atom nil))

(defn Group
  []
  (let [collapsed (r/atom true)]
    (fn [{:keys [id name children] :as _group}]
      [ui/ListItem {:active true
                    :on-click #(do
                                 (reset! selected-group id)
                                 (.stopPropagation %))
                    :style    {:cursor :pointer}}
       [ui/ListIcon {:style    (cond-> {:padding   5
                                        :min-width "17px"}
                                       (seq children) (assoc :cursor :pointer))
                     :on-click #(do (swap! collapsed not)
                                    (.stopPropagation %))
                     :name     (if (seq children)
                                 (if @collapsed "angle right" "angle down")
                                 "")}]
       [ui/ListContent {:className "nuvla-group-item"
                        :style     (cond-> {:padding       5
                                            :border-radius 5}
                                           (= @selected-group id) (assoc :background-color "lightgray"))}
        [ui/ListHeader (when (not= @selected-group id) {:style {:font-weight 400}})
         (or name id)]
        (when (and (not @collapsed) (seq children))
          [ui/ListList
           (for [child (sort-by (juxt :id :name) children)]
             ^{:key (:id child)}
             [Group child])])]])))

(defn GroupHierarchySegment
  []
  (let [groups-hierarchy @(subscribe [::session-subs/groups-hierarchies])]
    [ui/Segment {:raised true :style {:min-height "100%"}}
     [ui/Header {:as :h3} "Groups"]
     [full-text-search-plugin/FullTextSearch
      {:db-path      [::deployments-search]
       :change-event [:a]
       :style        {:width "100%"}}]
     [ui/ListSA
      (for [group-hierarchy (sort-by (juxt :id :name) groups-hierarchy)]
        ^{:key (:id group-hierarchy)}
        [Group group-hierarchy])]]))

(defn GroupsViewPage
  []
  [ui/Grid {:columns 2}
   [ui/GridColumn {:width 4 :stretched true :style {:background-color "light-gray"
                                                    :padding-right    0}}
    [GroupHierarchySegment]]
   [ui/GridColumn {:width 12 :stretched true :style {:background-color "light-gray"
                                                     :padding-right    0}}
    [ui/Segment {:style {:min-height "100%"}}
     (if @selected-group
       [GroupMembers @(subscribe [::session-subs/group @selected-group])]
       [:i "Select a group"])]]])
