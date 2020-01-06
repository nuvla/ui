(ns sixsq.nuvla.ui.profile.views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.events :as events]
    [sixsq.nuvla.ui.profile.subs :as subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as values]))


(defn tuple-to-row [[v1 v2]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (str v1)]
   [ui/TableCell v2]])


(def data-to-tuple
  (juxt (comp name first) (comp values/format-value second)))


(defn format-roles
  [{:keys [roles] :as m}]
  (assoc m :roles (values/format-collection (sort (str/split roles #"\s+")))))


(defn user-as-link
  [{:keys [user] :as m}]
  (assoc m :user (values/as-href {:href user})))


(def session-keys #{:user :roles :clientIP})


(def session-keys-order {:user 1, :clientIP 2, :expiry 3, :roles 4})


(defn add-index
  [[k _ :as entry]]
  (-> k
      (session-keys-order 5)
      (cons entry)))


(defn process-session-data
  [{:keys [expiry] :as data}]
  (let [locale (subscribe [::i18n-subs/locale])]
    (->> (select-keys data session-keys)
         (cons [:expiry (time/remaining expiry @locale)])
         (map add-index)
         (sort-by first)
         (map rest))))


;;; VALIDATION SPEC
(s/def ::current-password us/nonblank-string)
(s/def ::password us/acceptable-password?)
(s/def ::password-repeat us/nonblank-string)

(s/def ::credential-change-password
  (s/keys :req-un [::current-password
                   ::password
                   ::password-repeat]))


(defn password-repeat-check [form name]
  (let [password        (get-in @form [:names->value :password])
        password-repeat (get-in @form [:names->value name])]
    (when-not (= password password-repeat)
      [:password-repeat :password-not-equal])))


(defn modal-change-password []
  (let [open-modal    (subscribe [::subs/open-modal])
        error-message (subscribe [::subs/error-message])
        tr            (subscribe [::i18n-subs/tr])
        form-conf     {:names->value      {:current-password ""
                                           :password         ""
                                           :password-repeat  ""}
                       :form-spec         ::credential-change-password
                       :names->validators {:password-repeat [password-repeat-check]}}
        form          (fv/init-form form-conf)
        spec->msg     {::current-password (@tr [:should-not-be-empty])
                       ::password         (@tr [:password-constraint])
                       :password-repeat   (@tr [:passwords-doesnt-match])}]
    (fn []
      [ui/Modal
       {:size      :tiny
        :open      (= @open-modal :change-password)
        :closeIcon true
        :on-close  #(do
                      (dispatch [::events/close-modal])
                      (reset! form (fv/init-form form-conf)))}

       [ui/ModalHeader (@tr [:change-password])]

       [ui/ModalContent

        (when @error-message
          [ui/Message {:negative  true
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-error-message])}
           [ui/MessageHeader (str/capitalize (@tr [:error]))]
           [:p @error-message]])

        [ui/Form
         [ui/FormInput
          {:name          :current-password
           :label         (str/capitalize (@tr [:current-password]))
           :required      true
           :icon          "key"
           :icon-position "left"
           :auto-focus    "on"
           :auto-complete "off"
           :on-change     (partial fv/event->names->value! form)
           :on-blur       (partial fv/event->show-message form)
           :error         (fv/?show-message form :current-password spec->msg)}]
         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name          :password
                         :icon          "key"
                         :icon-position "left"
                         :required      true
                         :auto-complete "new-password"
                         :label         (str/capitalize (@tr [:new-password]))
                         :type          "password"
                         :on-change     (partial fv/event->names->value! form)
                         :on-blur       (partial fv/event->show-message form)
                         :error         (fv/?show-message form :password spec->msg)}]
          [ui/FormInput {:name      :password-repeat
                         :required  true
                         :label     (str/capitalize (@tr [:new-password-repeat]))
                         :type      "password"
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :password-repeat spec->msg)}]]]]

       [ui/ModalActions
        [uix/Button
         {:text     (str/capitalize (@tr [:change-password]))
          :positive true
          :on-click #(when (fv/validate-form-and-show? form)
                       (js/console.log "valide form")
                       (dispatch [::events/change-password (-> @form
                                                               :names->value
                                                               (dissoc :password-repeat))]))}]]])))


(defn session-info
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        session             (subscribe [::authn-subs/session])
        credential-password (subscribe [::subs/credential-password])]
    (fn []
      [ui/Segment style/basic
       (when @session
         (when-not @credential-password
           (dispatch [::events/get-user]))
         [cc/metadata
          {:title       (:identifier @session)
           :icon        "user"
           :description (str (@tr [:session-expires]) " " (-> @session :expiry time/parse-iso8601 time/ago))}
          (->> @session
               general-utils/remove-common-attrs
               user-as-link
               format-roles
               process-session-data
               (map data-to-tuple)
               (map tuple-to-row))])
       (when @credential-password
         [ui/Button {:primary  true
                     :on-click #(dispatch [::events/open-modal :change-password])}
          (str/capitalize (@tr [:change-password]))])
       (when-not @session
         [:p (@tr [:no-session])])])))


(defmethod panel/render :profile
  [path]
  [:div
   [session-info]
   [modal-change-password]])
