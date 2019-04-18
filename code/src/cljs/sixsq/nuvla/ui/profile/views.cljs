(ns sixsq.nuvla.ui.profile.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.cimi-api.utils :as cimi-api-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.events :as events]
    [sixsq.nuvla.ui.profile.subs :as subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
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


(defn format-ssh-keys
  [{:keys [sshPublicKey] :as data}]
  (let [keys (str/split sshPublicKey #"[\n\r]+")]
    (assoc data :sshPublicKey (values/format-collection keys))))


(defn process-session-data
  [{:keys [expiry] :as data}]
  (let [locale (subscribe [::i18n-subs/locale])]
    (->> (select-keys data session-keys)
         (cons [:expiry (time/remaining expiry @locale)])
         (map add-index)
         (sort-by first)
         (map rest))))

(defn modal-change-password []
  (let [open-modal (subscribe [::subs/open-modal])
        error-message (subscribe [::subs/error-message])
        tr (subscribe [::i18n-subs/tr])
        fields-in-errors (subscribe [::subs/fields-in-errors])
        form-error? (subscribe [::subs/form-error?])]
    (fn []
      [ui/Modal
       {:size      :tiny
        :open      (= @open-modal :change-password)
        :closeIcon true
        :on-close  (fn []
                     (dispatch [::events/close-modal]))}

       [ui/ModalHeader (@tr [:change-password])]

       [ui/ModalContent

        (when @error-message
          [ui/Message {:negative  true
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-error-message])}
           [ui/MessageHeader (str/capitalize (@tr [:error]))]
           [:p @error-message]])

        [ui/Form
         [ui/FormInput {:name          "current password"
                        :type          "password"
                        :placeholder   (str/capitalize (@tr [:current-password]))
                        :icon          "key"
                        :fluid         false
                        :icon-position "left"
                        :required      true
                        :auto-focus    true
                        :auto-complete "off"
                        :on-change     (ui-callback/value
                                         #(dispatch [::events/update-form-data :current-password %]))}]

         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name          "password"
                         :type          "password"
                         :placeholder   (str/capitalize (@tr [:new-password]))
                         :icon          "key"
                         :icon-position "left"
                         :required      true
                         :error         (contains? @fields-in-errors "password")
                         :auto-complete "off"
                         :on-change     (ui-callback/value
                                          #(dispatch [::events/update-form-data :new-password %]))}]

          [ui/FormInput {:name          "password"
                         :type          "password"
                         :placeholder   (str/capitalize (@tr [:new-password-repeat]))
                         :required      true
                         :error         (contains? @fields-in-errors "password")
                         :auto-complete "off"
                         :on-change     (ui-callback/value
                                          #(dispatch [::events/update-form-data :repeat-new-password %]))}]]]]

       [ui/ModalActions
        [uix/Button
         {:text     (str/capitalize (@tr [:change-password]))
          :positive true
          :disabled @form-error?
          :on-click #(dispatch [::events/change-password])}]]])))


(defn session-info
  []
  (let [tr (subscribe [::i18n-subs/tr])
        session (subscribe [::authn-subs/session])
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
               cimi-api-utils/remove-common-attrs
               user-as-link
               format-roles
               process-session-data
               (map data-to-tuple)
               (map tuple-to-row))])
       (when @credential-password
         [ui/Button {:primary true
                     :on-click #(dispatch [::events/open-modal :change-password])}
          (str/capitalize (@tr [:change-password]))])
       (when-not @session
         [:p (@tr [:no-session])])])))


(defmethod panel/render :profile
  [path]
  [:div
   [session-info]
   [modal-change-password]])

