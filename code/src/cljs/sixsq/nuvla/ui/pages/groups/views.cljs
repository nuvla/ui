(ns sixsq.nuvla.ui.pages.groups.views
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.groups.events :as events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.pages.groups.subs :as subs]
            [sixsq.nuvla.ui.utils.forms :as forms]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.spec :as us]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(s/def ::group-name us/nonblank-string)
(s/def ::group-description us/nonblank-string)

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

(defn RevokeInvitationButton
  [group invited-email]
  (let [group-name (or (:name group) (:id group))]
    [ConfirmActionModal {:on-confirm #(dispatch [::events/revoke group invited-email])
                         :header     "Revoke invitation"
                         :Content    [:span "Do you want to revoke the invitation of " [:b invited-email] " from " [:b group-name] " group?"]
                         :Icon       [icons/TrashIcon]}]))

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
        reset-key   (r/atom (random-uuid))
        invite-user (r/atom "")
        invite-fn   #(do
                       (when-not (str/blank? @invite-user)
                         (dispatch [::events/invite-to-group id @invite-user])
                         (reset! reset-key (random-uuid))
                         (reset! invite-user "")))]
    (fn [group]
      (when (utils-general/can-operation? "invite" group)
        ^{:key @reset-key}
        [ui/Input {:placeholder   (@tr [:invite-by-email])
                   :type          :email
                   :icon          (r/as-element
                                    [icons/PaperPlaneIcon {:style    {:font-size "unset"}
                                                           :link     (not (str/blank? @invite-user))
                                                           :color    (when (not (str/blank? @invite-user)) "blue")
                                                           :circular true
                                                           :onClick  invite-fn}])
                   :style         {:width "280px" :cursor :pointer}
                   :on-key-press  (partial forms/on-return-key invite-fn)
                   :default-value @invite-user
                   :on-change     (ui-callback/value #(reset! invite-user %))}]))))

(defn sanitize-name [name]
  (when name
    (str/lower-case
      (str/replace
        (str/trim
          (str/join "" (re-seq #"[a-zA-Z0-9-_\ ]" name)))
        " " "-"))))

(defn AddGroupButton
  [_opts]
  (let [tr         (subscribe [::i18n-subs/tr])
        show?      (r/atom false)
        group-name (r/atom "")
        group-desc (r/atom "")
        validate?  (r/atom false)
        loading?   (r/atom false)
        close-fn   #(reset! show? false)]
    (fn [{:keys [parent-group header]}]
      (let [group-identifier (sanitize-name @group-name)
            form-valid?      (and (s/valid? ::group-name @group-name)
                                  (s/valid? ::group-description @group-desc))]
        [ui/Modal
         {:open       @show?
          :close-icon true
          :on-close   close-fn
          :trigger    (r/as-element
                        [ui/Button {:secondary true
                                    :size      "small"
                                    :icon      true
                                    :on-click  #(reset! show? true)}
                         [icons/PlusSquareIcon]
                         header])}
         [uix/ModalHeader {:header header}]
         [ui/ModalContent
          [ui/Message {:hidden (not (and @validate? (not form-valid?)))
                       :error  true}
           [ui/MessageHeader (@tr [:validation-error])]
           [ui/MessageContent (@tr [:validation-error-message])]]
          (when-not (str/blank? group-identifier)
            [:i {:style {:padding-left "1ch"
                         :color        :grey}}
             [:b "id : "]
             (str "group/" group-identifier)])
          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField (@tr [:name]), :required? true, :default-value @group-name,
             :validate-form? @validate?, :spec ::group-name,
             :on-change #(reset! group-name %)]
            [uix/TableRowField (@tr [:description]), :required? true,
             :spec ::group-description, :validate-form? @validate?,
             :default-value @group-desc, :on-change #(reset! group-desc %)]]]]
         [ui/ModalActions
          [uix/Button
           {:text     (str/capitalize (@tr [:add]))
            :primary  true
            :disabled (and @validate? (not form-valid?))
            :loading  @loading?
            :on-click #(if (not form-valid?)
                         (reset! validate? true)
                         (do
                           (reset! show? false)
                           (dispatch
                             [::events/add-group {:parent-group     parent-group
                                                  :group-identifier group-identifier
                                                  :group-name       @group-name
                                                  :group-desc       @group-desc
                                                  :loading?         loading?}])))}]]]))))

(defn EditGroupButton
  [{:keys [name description] :as group}]
  (let [tr         (subscribe [::i18n-subs/tr])
        show?      (r/atom false)
        group-name (r/atom name)
        group-desc (r/atom description)
        validate?  (r/atom false)
        close-fn   #(reset! show? false)]
    (fn [_group]
      (let [form-valid? (and (s/valid? ::group-name @group-name)
                             (s/valid? ::group-description @group-desc))]
        [ui/Modal
         {:open       @show?
          :close-icon true
          :on-close   close-fn
          :trigger    (r/as-element
                        [icons/PencilIcon {:on-click #(reset! show? true)
                                           :style    {:cursor :pointer}}])}
         [uix/ModalHeader {:header "Edit group"}]
         [ui/ModalContent
          [ui/Message {:hidden (not (and @validate? (not form-valid?)))
                       :error  true}
           [ui/MessageHeader (@tr [:validation-error])]
           [ui/MessageContent (@tr [:validation-error-message])]]
          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField (@tr [:name]), :required? true, :default-value @group-name,
             :validate-form? @validate?, :spec ::group-name,
             :on-change #(reset! group-name %)]
            [uix/TableRowField (@tr [:description]), :required? true,
             :spec ::group-description, :validate-form? @validate?,
             :default-value @group-desc, :on-change #(reset! group-desc %)]]]]
         [ui/ModalActions
          [uix/Button
           {:text     (str/capitalize (@tr [:save]))
            :primary  true
            :disabled (and @validate? (not form-valid?))
            :on-click #(if (not form-valid?)
                         (reset! validate? true)
                         (do
                           (reset! show? false)
                           (dispatch
                             [::events/edit-group (assoc group
                                                    :name @group-name
                                                    :description @group-desc)])))}]]]))))

(defn GroupMembers
  [group]
  (let [editable? (utils-general/editable? group false)]
    (fn [{:keys [id name description users] :as group}]
      (let [group-name (or name id)]
        [:<>
         [:div {:style {:display         :flex
                        :align-items     :flex-start
                        :justify-content :space-between
                        :flex-wrap       :wrap
                        :padding-bottom  "1em"}}
          [ui/Header {:as :h3}
           [icons/UserGroupIcon]
           [ui/HeaderContent
            group-name " " (when editable?
                             [EditGroupButton group])
            [ui/HeaderSubheader description " (" id ")"]]]
          (when (utils-general/can-operation? "add-subgroup" group)
            [AddGroupButton {:header       "Add Subgroup"
                             :parent-group group}])]
         [ui/Header {:as :h3} "Members"]
         (if (empty? users)
           [uix/MsgNoItemsToShow [uix/TR (if editable? :empty-group-message
                                                       :empty-group-or-no-access-message)]]

           [ui/ListSA {:divided true :vertical-align "middle"}
            (for [m users]
              ^{:key m}
              [GroupMember id group-name m editable? group])])
         [InviteInput group]]))))

(defn GroupPendingInvitations
  [group]
  (let [pending-invitations (subscribe [::subs/pending-invitations (:id group)])]
    (dispatch [::events/get-pending-invitations (:id group) pending-invitations])
    (fn [group]
      [:<>
       [ui/Header {:as :h3} "Pending invitations"]
       (if (empty? @pending-invitations)
         [uix/MsgNoItemsToShow "No pending invitations"]
         [ui/ListSA {:divided true :vertical-align "middle"}
          (for [pi @pending-invitations]
            ^{:key (:invited-email pi)}
            [ui/ListItem
             [ui/ListContent {:floated :right}
              (when (utils-general/can-operation? "revoke-invitation" group)
                [RevokeInvitationButton group (:invited-email pi)])]
             [ui/ListContent {:style {:display :flex :align-items :flex-end}}
              [ui/IconGroup
               [ui/Icon {:className icons/i-user :size "large"}]]
              (:invited-email pi)]
             ])])])))

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
  (let [tr     (subscribe [::i18n-subs/tr])
        search (r/atom "")]
    (fn [selected-group]
      (let [filtered-groups-hierarch @(subscribe [::subs/filter-groups-hierarchy @search])]
        [ui/Segment {:raised true :style {:overflow-x :auto
                                          :min-height "100%"}}

         [:div {:style {:display         :flex
                        :align-items     :baseline
                        :justify-content :space-between
                        :flex-wrap       :wrap
                        :padding-bottom  "1em"}}
          [ui/Header {:as :h3} "Groups"]
          [AddGroupButton {:header (@tr [:add-group])}]]
         [ui/Input
          {:style         {:width "100%"}
           :placeholder   (str (@tr [:search]) "...")
           :icon          "search"
           :default-value @search
           :on-change     (ui-callback/input-callback #(reset! search %))}]
         (if (seq filtered-groups-hierarch)
           [ui/ListSA {:selection true}
            (for [group-hierarchy (sort-by (juxt :id :name) filtered-groups-hierarch)]
              ^{:key (:id group-hierarchy)}
              [Group group-hierarchy selected-group])]
           [uix/MsgNoItemsToShow [uix/TR "No groups found"]])]))))

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
         [:<>
          [GroupMembers selected-group]
          (when (utils-general/can-operation? "get-pending-invitations" selected-group)
            ^{:key (:id selected-group)}
            [GroupPendingInvitations selected-group])]
         [uix/MsgNoItemsToShow [uix/TR "Select a Group"]])]]]))
