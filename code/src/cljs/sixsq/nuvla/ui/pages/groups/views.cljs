(ns sixsq.nuvla.ui.pages.groups.views
  (:require ["@stripe/react-stripe-js" :as react-stripe]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.pages.profile.events :as events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.forms :as forms]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn ConfirmActionModal
  [{:keys [on-confirm header Content Icon]}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/ModalDanger
     {:button-text (@tr [:yes])
      :on-confirm  on-confirm
      :trigger     (r/as-element
                     [:span [ui/Popup {:content header
                                       :trigger (r/as-element
                                                  [ui/Button {:icon true :basic true}
                                                   Icon])}]])
      :header      header
      :content     Content}]))

(defn RemoveManagerButton
  [group principal principal-name group-name]
  [ConfirmActionModal {:on-confirm (fn []
                                     (dispatch [::events/edit-group
                                                (-> group
                                                    (utils-general/acl-append-resource :owners "group/nuvla-admin")
                                                    (utils-general/acl-remove-resource :owners principal)
                                                    (utils-general/acl-remove-resource :edit-meta principal)
                                                    (utils-general/acl-remove-resource :edit-data principal)
                                                    (utils-general/acl-remove-resource :edit-acl principal)
                                                    (utils-general/acl-remove-resource :manage principal))]))
                       :header     "Remove manager"
                       :Content    [:span "Do you want to remove " [:b @principal-name] " from manager's of group " [:b group-name] "?"]
                       :Icon       [ui/IconGroup
                                    [icons/Icon {:name "fal fa-crown"}]
                                    [icons/Icon {:name "fal fa-slash"}]]}])

(defn MakeManagerButton
  [group principal principal-name group-name]
  [ConfirmActionModal {:on-confirm #(dispatch [::events/edit-group
                                               (-> group
                                                   (utils-general/acl-append-resource :edit-acl principal)
                                                   (utils-general/acl-append-resource :manage principal))])
                       :header     "Make manager"
                       :Content    [:span "Do you want to make " [:b @principal-name] " a manager of group " [:b group-name] "?"]
                       :Icon       [icons/Icon {:name "fal fa-crown"}]}])

(defn RemoveMemberButton
  [group principal principal-name group-name]
  [ConfirmActionModal {:on-confirm (fn []
                                     (dispatch [::events/edit-group
                                                (-> group
                                                    (update :users (partial remove #{principal}))
                                                    (utils-general/acl-append-resource :owners "group/nuvla-admin")
                                                    (utils-general/acl-remove-resource :edit-acl principal)
                                                    (utils-general/acl-remove-resource :edit-data principal)
                                                    (utils-general/acl-remove-resource :edit-meta principal)
                                                    (utils-general/acl-remove-resource :view-acl principal)
                                                    (utils-general/acl-remove-resource :view-data principal)
                                                    (utils-general/acl-remove-resource :view-meta principal)
                                                    (utils-general/acl-remove-resource :manage principal))]))
                       :header     "Remove member"
                       :Content    [:span "Do you want to remove " [:b @principal-name] " from " [:b group-name] " group?"]
                       :Icon       [icons/TrashIcon]}])

(defn LimitMemberViewButton
  [group principal]
  [ConfirmActionModal {:on-confirm (fn []
                                     (dispatch [::events/edit-group
                                                (-> group
                                                    (utils-general/acl-remove-resource :edit-meta principal)
                                                    (utils-general/acl-remove-resource :edit-data principal)
                                                    (utils-general/acl-remove-resource :edit-acl principal)
                                                    (utils-general/acl-remove-resource :view-acl principal)
                                                    (utils-general/acl-remove-resource :view-data principal)
                                                    )]))
                       :header     "Limit member’s view"
                       :Content    "Limit member’s view to only the group name and description"
                       :Icon       [icons/Icon {:className "far fa-eye-slash"}]}])

(defn ExtendMemberViewButton
  [group principal]
  [ConfirmActionModal {:on-confirm (fn []
                                     (dispatch [::events/edit-group
                                                (utils-general/acl-append-resource group :view-acl principal)]))
                       :header     "Extend user view"
                       :Content    "Extend user view to member's list"
                       :Icon       [icons/Icon {:className "far fa-eye"}]}])

(defn GroupMember
  [id group-name principal editable? {{:keys [owners manage view-data view-acl] :as acl} :acl :as group}]
  (let [tr                (subscribe [::i18n-subs/tr])
        principal-name    (subscribe [::session-subs/resolve-principal principal])
        manager?          (boolean ((set (concat owners manage)) principal))
        can-view-members? (boolean ((set (concat owners view-data view-acl)) principal))]
    [ui/ListItem
     (when editable?
       [ui/ListContent {:floated :right}
        (if manager?
          [RemoveManagerButton group principal principal-name group-name]
          [:<>
           (if can-view-members?
             [LimitMemberViewButton group principal]
             [ExtendMemberViewButton group principal])
           [MakeManagerButton group principal principal-name group-name]])
        [RemoveMemberButton group principal principal-name group-name]])


     [ui/ListContent {:style {:display :flex :align-items :flex-end}}
      [ui/IconGroup
       [ui/Icon {:className icons/i-user :size "large"}]
       (when manager? [ui/Icon {:className "fa-solid fa-crown" :corner true}])]
      @principal-name]
     ]))

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

(defn InviteInput
  [{:keys [id] :as _group}]
  (let [tr          (subscribe [::i18n-subs/tr])
        invite-user (r/atom nil)
        invite-fn   #(do
                       (when-not (str/blank? @invite-user)
                         (dispatch [::events/invite-to-group id @invite-user])
                         (reset! invite-user nil)))]
    (fn [group]
      (when (utils-general/can-operation? "invite" group)
        [ui/Input {:placeholder  (@tr [:invite-by-email])
                   :type         :email
                   :icon         (r/as-element
                                   [icons/PaperPlaneIcon {:style    {:font-size "unset"}
                                                          :link     (not (str/blank? @invite-user))
                                                          :color    (when (not (str/blank? @invite-user)) "blue")
                                                          :circular true
                                                          :onClick  invite-fn}])
                   :style        {:width "280px" :cursor :pointer}
                   :on-key-press (partial forms/on-return-key invite-fn)
                   :value        (or @invite-user "")
                   :on-change    (ui-callback/value #(reset! invite-user %))}]))))

(defn GroupMembers
  [group]
  (let [editable? (utils-general/editable? group false)]
    (fn [{:keys [id name description users] :as group}]
      (let [group-name (or name id)]
        [:<>
         [ui/Grid {:columns 2 :stackable true}
          [ui/GridColumn {:floated :left :width 13}
           [ui/Header {:as :h3}
            [icons/UserGroupIcon]
            [ui/HeaderContent
             group-name
             [ui/HeaderSubheader description " (" id ")"]]]]
          [ui/GridColumn {:floated :right :width 3}
           [ui/Button {:basic true :floated :right} [:b "Add Subgroup"]]]]
         [ui/Header {:as :h3 :dividing true} "Members"]
         (if (empty? users)
           [uix/MsgNoItemsToShow [uix/TR (if editable? :empty-group-message
                                                       :empty-group-or-no-access-message)]]

           [ui/ListSA {:divided true :vertical-align "middle"}
            (for [m users]
              ^{:key m}
              [GroupMember id group-name m editable? group])])
         [InviteInput group]

         ]))))

(defn Group
  [{:keys [id] :as _group} {:keys [parents] :as _selected-group}]
  (let [collapsed (r/atom (not ((set parents) id)))]
    (fn [{:keys [id name children] :as _group} selected-group]
      (let [selected? (= (:id selected-group) id)
            children? (boolean (seq children))]
        [ui/ListItem {:on-click #(do
                                   (dispatch [::routing-events/navigate routes/groups-details {:uuid (utils-general/id->uuid id)}])
                                   (.stopPropagation %))}
         [ui/ListIcon {:style    {:padding   5
                                  :min-width "17px"}
                       :on-click #(when children?
                                    (swap! collapsed not)
                                    (.stopPropagation %))
                       :name     (if (seq children)
                                   (if @collapsed "angle right" "angle down")
                                   "")}]
         [ui/ListContent
          [ui/ListHeader
           {:className "nuvla-group-item"
            :style     (cond-> {:padding       5
                                :border-radius 5}
                               selected? (assoc :background-color "lightgray")
                               (not selected?) (assoc :font-weight 400))}
           (or name id)]
          (when (and (not @collapsed) (seq children))
            [ui/ListList
             (for [child (sort-by (juxt :id :name) children)]
               ^{:key (:id child)}
               [Group child selected-group])])]]))))

(defn GroupHierarchySegment
  [selected-group]
  (let [groups-hierarchy @(subscribe [::session-subs/groups-hierarchies])]
    [ui/Segment {:raised true :style {:overflow-x :auto
                                      :min-height "100%"}}
     [ui/Header {:as :h3} "Groups"]
     [full-text-search-plugin/FullTextSearch
      {:db-path      [::deployments-search]
       :change-event [:a]
       :style        {:width "100%"}}]
     [ui/ListSA {:selection true}
      (for [group-hierarchy (sort-by (juxt :id :name) groups-hierarchy)]
        ^{:key (:id group-hierarchy)}
        [Group group-hierarchy selected-group])]]))

(defn GroupsViewPage
  [{path :path}]
  (let [[_ uuid] path
        selected-group (when uuid
                @(subscribe [::session-subs/group (str "group/" uuid)]))]
    [ui/Grid {:stackable false}
     [ui/GridColumn {:stretched true
                     :computer  4
                     :tablet    6
                     :mobile    8
                     :style     {:background-color "light-gray"
                                 :padding-right    0}}
      [GroupHierarchySegment selected-group]]
     [ui/GridColumn {:stretched true
                     :tablet    10
                     :computer  12
                     :mobile    8
                     :style     {:background-color "light-gray"
                                 :padding-right    0}}
      [ui/Segment {:style {:min-height "100%"
                           :overflow-x :auto}}
       (if selected-group
         ^{:key selected-group}
         [GroupMembers selected-group]
         [uix/MsgNoItemsToShow [uix/TR "Select a Group"]])]]]))
