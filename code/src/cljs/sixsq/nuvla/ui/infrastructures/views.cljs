(ns sixsq.nuvla.ui.infrastructures.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.infrastructures.events :as events]
    [sixsq.nuvla.ui.infrastructures.spec :as spec]
    [sixsq.nuvla.ui.infrastructures.subs :as subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [taoensso.timbre :as timbre]
    [taoensso.timbre :as log]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :position  "right"
        :on-click  #(dispatch [::events/get-services])}])))


(defn toggle [v]
  (swap! v not))


;(defn services-search []
;  (let [tr (subscribe [::i18n-subs/tr])]
;    [ui/Input {:placeholder (@tr [:search])
;               :icon        "search"
;               :on-change   (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))}]))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [ui/MenuMenu {:position "left"}
      ;[services-search]
      [uix/MenuItemWithIcon
       {:name      (@tr [:add])
        :icon-name "plus"
        :position  "right"
        :on-click  #(do
                      (dispatch-sync [::events/reset-service-group])
                      (dispatch-sync [::events/reset-service])
                      (dispatch [::events/open-add-service-modal]))}]]
     [ui/MenuMenu {:position "right"}
      [refresh-button]]]))


(def service-icons
  {:swarm :docker
   :s3    :aws})


(defn service-card
  [{:keys [id name description path type logo-url] :as service}]
  ^{:key id}
  [ui/Card {:on-click #(dispatch [::events/open-service-modal service false])}
   (when logo-url
     [ui/Image {:src   logo-url
                :style {:width      "auto"
                        :height     "100px"
                        :object-fit "contain"}}])
   [ui/CardContent
    [ui/CardHeader {:style {:word-wrap "break-word"}}
     [ui/Icon {:name ((keyword type) service-icons)}]
     (or name id)]
    [ui/CardMeta {:style {:word-wrap "break-word"}} path]
    [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description]]])


(defn service-group-card
  [group services]
  ^{:key group}
  [ui/Card
   (when (< (count services) 2)
     [ui/Label {:corner   true
                :style    {:z-index 0
                           :cursor  :pointer}
                :size     "mini"
                :on-click #(do
                             (dispatch-sync [::events/set-service-group group services])
                             (dispatch-sync [::events/reset-service])
                             (dispatch-sync [::events/update-service :parent group])
                             (dispatch [::events/open-add-service-modal]))}
      [ui/Icon {:name  "plus"
                :style {:cursor :pointer}}]                 ; use content to work around bug in icon in label for cursor
      ])
   [ui/CardContent
    [ui/CardHeader {:style {:word-wrap "break-word"}}]
    (for [service services] (service-card service))]])


(defn service-groups
  [groups]
  [ui/Segment style/basic
   (vec (concat [ui/CardGroup {:centered true}]
                (for [[group services] groups]
                  [service-group-card group services])))])


(defn infra-services
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        services          (subscribe [::subs/services])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (fn []
      (let [groups         (:groups @services)
            total-services (get @services :count 0)
            total-pages    (general-utils/total-pages total-services @elements-per-page)]
        [ui/Container {:fluid true}
         [:h2
          [ui/Icon {:name "cloud"}]
          " "
          (@tr [:infra-services])]
         [control-bar]

         (when (pos-int? total-services)
           [ui/Segment
            [service-groups groups]
            (when (> total-pages 1)
              [:div {:style {:padding-bottom 30}}
               [uix/Pagination
                {:totalitems   total-services
                 :totalPages   total-pages
                 :activePage   @page
                 :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}]])])]))))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


(defn row-with-label
  [key name-kw value editable? mandatory? value-spec type validation-event]
  (let [tr              (subscribe [::i18n-subs/tr])
        active-input    (subscribe [::subs/active-input])
        local-validate? (reagent/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (fn [key name-kw value editable? mandatory? value-spec type validation-event]
      (let [name-str      (name name-kw)
            name-label    (if (and editable? mandatory?) (utils-general/mandatory-name name-str) name-str)
            input-active? (= name-str @active-input)
            validate?     (or @local-validate? @validate-form?)
            valid?        (s/valid? value-spec value)]
        ;        (s/explain value-spec value)
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          name-label]
         [ui/TableCell
          (if editable?
            ^{:key key}
            (if (in? [:input :password] type)
              [ui/Input {:default-value value
                         :placeholder   (@tr [name-kw])
                         :disabled      (not editable?)
                         :error         (when (and validate? (not valid?)) true)
                         :fluid         true
                         :type          (if (= type :input) :text type)
                         :icon          (when input-active? :pencil)
                         :onMouseEnter  #(dispatch [::events/active-input name-str])
                         :onMouseLeave  #(dispatch [::events/active-input nil])
                         :on-change     (ui-callback/input-callback
                                          #(do
                                             (reset! local-validate? true)
                                             (dispatch [validation-event])
                                             (dispatch [::events/update-service name-kw %])))}]
              ; Semantic UI's textarea styling requires to be wrapped in a form
              [ui/Form
               [ui/FormField {:class (when (and validate? (not valid?)) :error)}
                [ui/TextArea {:default-value value
                              :placeholder   (@tr [name-kw])
                              :disabled      (not editable?)
                              :icon          (when input-active? :pencil)
                              :onMouseEnter  #(dispatch [::events/active-input name-str])
                              :onMouseLeave  #(dispatch [::events/active-input nil])
                              :on-change     (ui-callback/input-callback
                                               #(do
                                                  (reset! local-validate? true)
                                                  (dispatch [validation-event])
                                                  (dispatch [::events/update-service name-kw %])))}]]])
            [:span value])]]))))


(defn service-swarm
  []
  (let [is-new? (subscribe [::subs/is-new?])
        service (subscribe [::subs/service])]
    (fn []
      (let [editable?             (utils-general/editable? @service @is-new?)
            {:keys [name description endpoint]} @service
            form-validation-event ::events/validate-swarm-service-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody
          [row-with-label "name" :name name editable? true
           ::spec/name :input form-validation-event]
          [row-with-label "description" :description description editable? true
           ::spec/description :input form-validation-event]
          [row-with-label "swarm-endpoint" :endpoint endpoint editable? true
           ::spec/endpoint :input form-validation-event]]]))))


(defn service-minio
  []
  (let [is-new? (subscribe [::subs/is-new?])
        service (subscribe [::subs/service])]
    (fn []
      (let [editable?             (utils-general/editable? @service @is-new?)
            {:keys [name description endpoint]} @service
            form-validation-event ::events/validate-minio-service-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody
          [row-with-label "name" :name name editable? true
           ::spec/name :input form-validation-event]
          [row-with-label "description" :description description editable? true
           ::spec/description :input form-validation-event]
          [row-with-label "endpoint" :endpoint endpoint editable? true
           ::spec/endpoint :input form-validation-event]]]))))


(def infrastructure-service-validation-map
  {"swarm" {:validation-event ::events/validate-swarm-service-form
            :modal-content    service-swarm}
   "s3"    {:validation-event ::events/validate-minio-service-form
            :modal-content    service-minio}})


(defn save-callback
  [form-validation-event]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [form-validation-event])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-service])))))


(defn service-modal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/service-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        service     (subscribe [::subs/service])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [type             (:type @service "")
            name             (:name @service type)
            header           (str (if (true? @is-new?) (@tr [:new]) (@tr [:update])) " " name)
            validation-item  (get infrastructure-service-validation-map type)
            validation-event (:validation-event validation-item)
            modal-content    (:modal-content validation-item)]

        (if (empty? type)
          [:div]
          [ui/Modal {:open       @visible?
                     :close-icon true
                     :on-close   #(dispatch [::events/close-service-modal])}

           [ui/ModalHeader header]

           [ui/ModalContent {:scrolling false}
            [utils-validation/validation-error-message ::subs/form-valid?]
            [modal-content]]
           [ui/ModalActions
            [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:update]))
                         :positive true
                         :disabled (when-not @form-valid? true)
                         :active   true
                         :on-click #(save-callback validation-event)}]]])))))


(defn add-service-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-service-modal-visible?])
        group    (subscribe [::subs/service-group])
        service  (subscribe [::subs/service])]
    (fn []
      (let [services (:services @group)]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-service-modal])}

         [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

         [ui/ModalContent {:scrolling false}

          (when (not= 1 (count services))
            [:div
             [:div {:style {:padding-bottom 20}}
              (@tr [:register-swarm-note])]
             [ui/CardGroup {:centered true}

              [ui/Card {:on-click #(do
                                     (dispatch [::events/set-validate-form? false])
                                     (dispatch [::events/form-valid])
                                     (dispatch [::events/close-add-service-modal])
                                     (dispatch [::events/open-service-modal (assoc @service :type "swarm") true]))}

               [ui/CardContent {:text-align :center}
                [ui/Header "Swarm"]
                [ui/Icon {:name "docker"
                          :size :massive}]
                [ui/Header (@tr [:register])]]]

              [ui/Card {:on-click nil}
               [ui/CardContent {:text-align :center}
                [ui/Header "Swarm"]
                [ui/Icon {:name  "docker"
                          :size  :massive
                          :color :grey}]
                [ui/Header (@tr [:deploy]) (str " (" (@tr [:soon]) ")")]]]]])
          [:div {:style {:padding-top    20
                         :padding-bottom 20}}
           (@tr [:register-s3-note])]
          [ui/CardGroup {:centered true}

           [ui/Card {:on-click #(do
                                  (dispatch [::events/set-validate-form? false])
                                  (dispatch [::events/form-valid])
                                  (dispatch [::events/close-add-service-modal])
                                  (dispatch [::events/open-service-modal (assoc @service :type "s3") true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "MinIO"]
             [ui/Image {:src  "/ui/images/minio.png"
                        :size :tiny}]
             [ui/Header (@tr [:register])]]]]]
         [ui/ModalActions]]))))


(defmethod panel/render :infrastructures
  [path]
  (timbre/set-level! :info)
  (dispatch [::events/get-services])
  [:div
   [infra-services]
   [service-modal]
   [add-service-modal]])
