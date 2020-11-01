(ns sixsq.nuvla.ui.profile.views
  (:require
    ["@stripe/react-stripe-js" :as react-stripe]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.events :as events]
    [sixsq.nuvla.ui.profile.subs :as subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


;;; VALIDATION SPEC
(s/def ::current-password us/nonblank-string)
(s/def ::new-password us/acceptable-password?)
(s/def ::new-password-repeat us/nonblank-string)

(s/def ::credential-change-password
  (s/keys :req-un [::current-password
                   ::new-password
                   ::new-password-repeat]))


(defn password-repeat-check [form name]
  (let [password        (get-in @form [:names->value :new-password])
        password-repeat (get-in @form [:names->value name])]
    (when-not (= password password-repeat)
      [:new-password-repeat :password-not-equal])))


(defn modal-change-password []
  (let [open?     (subscribe [::subs/modal-open? :change-password])
        error     (subscribe [::subs/error-message])
        tr        (subscribe [::i18n-subs/tr])
        form-conf {:names->value      {:current-password    ""
                                       :new-password        ""
                                       :new-password-repeat ""}
                   :form-spec         ::credential-change-password
                   :names->validators {:new-password-repeat [password-repeat-check]}}
        form      (fv/init-form form-conf)
        spec->msg {::current-password   (@tr [:should-not-be-empty])
                   ::new-password       (@tr [:password-constraint])
                   :new-password-repeat (@tr [:passwords-doesnt-match])}]
    (fn []
      [ui/Modal
       {:size      :tiny
        :open      @open?
        :closeIcon true
        :on-close  #(do
                      (dispatch [::events/close-modal])
                      (reset! form (fv/init-form form-conf)))}

       [ui/ModalHeader (@tr [:change-password])]

       [ui/ModalContent

        (when @error
          [ui/Message {:negative  true
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-error-message])}
           [ui/MessageHeader (str/capitalize (@tr [:error]))]
           [:p @error]])

        [ui/Form
         [ui/FormInput
          {:name          :current-password
           :id            "current-password"
           :label         (str/capitalize (@tr [:current-password]))
           :required      true
           :icon          "key"
           :icon-position "left"
           :auto-focus    "on"
           :auto-complete "off"
           :type          "password"
           :on-change     (partial fv/event->names->value! form)
           :on-blur       (partial fv/event->show-message form)
           :error         (fv/?show-message form :current-password spec->msg)}]
         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name          :new-password
                         :icon          "key"
                         :icon-position "left"
                         :required      true
                         :auto-complete "new-password"
                         :label         (str/capitalize (@tr [:new-password]))
                         :type          "password"
                         :on-change     (partial fv/event->names->value! form)
                         :on-blur       (partial fv/event->show-message form)
                         :error         (fv/?show-message form :new-password spec->msg)}]
          [ui/FormInput {:name      :new-password-repeat
                         :required  true
                         :label     (str/capitalize (@tr [:new-password-repeat]))
                         :type      "password"
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :new-password-repeat spec->msg)}]]]]

       [ui/ModalActions
        [uix/Button
         {:text     (str/capitalize (@tr [:change-password]))
          :positive true
          :on-click #(when (fv/validate-form-and-show? form)
                       (dispatch [::events/change-password
                                  (-> @form
                                      :names->value
                                      (dissoc :new-password-repeat))]))}]]])))


(defn Session
  []
  (let [tr      (subscribe [::i18n-subs/tr])
        session (subscribe [::session-subs/session])]
    [ui/Segment {:padded true, :color "teal", :style {:height "100%"}}
     [ui/Header {:as :h2 :dividing true} "Session"]
     (if @session
       [ui/Table {:basic "very"}
        [ui/TableBody
         [ui/TableRow
          [ui/TableCell {:width 5} [:b "Identifier"]]
          [ui/TableCell {:width 11} (:identifier @session)]]
         [ui/TableRow
          [ui/TableCell [:b (str/capitalize (@tr [:session-expires]))]]
          [ui/TableCell (-> @session :expiry time/parse-iso8601 time/ago)]]
         [ui/TableRow
          [ui/TableCell [:b "User id"]]
          [ui/TableCell (values/as-href {:href (:user @session)})]]
         [ui/TableRow
          [ui/TableCell [:b "Roles"]]
          [ui/TableCell (values/format-collection (sort (str/split (:roles @session) #"\s+")))]]]]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true, :text-align "center"}
          [ui/Icon {:className "fad fa-sign-in-alt"}]
          (@tr [:no-session])]]])]))


(def Elements (r/adapt-react-class react-stripe/Elements))
(def CardElement (r/adapt-react-class react-stripe/CardElement))
(def IbanElement (r/adapt-react-class react-stripe/IbanElement))


(defn PaymentMethodInputInternal
  [{:keys [type onChange options] :as props}]
  (case type
    "sepa_debit" [IbanElement
                  {:className "stripe-input"
                   :on-change onChange
                   :options   (clj->js options)}]
    "card" [CardElement {:className "stripe-input"
                         :on-change onChange}]
    [:div]))

;; While not yet hooks support we have to use react components
;; https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#hooks

(defn PaymentMethodInputWrapper
  [props]
  (let [elements     (react-stripe/useElements)
        props        (js->clj props :keywordize-keys true)
        set-elements (:setElements props)]
    (when set-elements (set-elements elements))
    (r/as-element
      [PaymentMethodInputInternal props])))


(def PaymentMethodInputReactClass (r/adapt-react-class PaymentMethodInputWrapper))

(defn PaymentMethodInput
  [props]
  (let [locale (subscribe [::i18n-subs/locale])
        stripe (subscribe [::main-subs/stripe])]
    (fn [props]
      ^{:key (str @locale @stripe)}
      [Elements {:stripe  @stripe
                 :options {:locale @locale}}
       [PaymentMethodInputReactClass props]])))


;; VALIDATION SPEC
(s/def ::fullname us/nonblank-string)
(s/def ::street-address us/nonblank-string)
(s/def ::city us/nonblank-string)
(s/def ::country us/nonblank-string)
(s/def ::postal-code us/nonblank-string)
(s/def ::payment-method (s/nilable map?))
(s/def ::coupon string?)
(s/def ::email us/email?)

(s/def ::customer
  (s/keys :req-un [::fullname
                   ::street-address
                   ::city
                   ::country
                   ::postal-code]
          :opt-un [::payment-method
                   ::coupon]))


(s/def ::customer-with-email
  (s/keys :req-un [::fullname
                   ::street-address
                   ::city
                   ::country
                   ::postal-code
                   ::email]
          :opt-un [::payment-method
                   ::coupon]))


(defn CustomerFormFields
  [form]
  (let [tr        (subscribe [::i18n-subs/tr])
        is-group? (subscribe [::session-subs/active-claim-is-group?])]
    (fn [form]
      (let [should-not-be-empty-msg (@tr [:should-not-be-empty])
            spec->msg               {::fullname       should-not-be-empty-msg
                                     ::street-address should-not-be-empty-msg
                                     ::city           should-not-be-empty-msg
                                     ::country        should-not-be-empty-msg
                                     ::postal-code    should-not-be-empty-msg
                                     ::coupon         should-not-be-empty-msg
                                     ::email          (@tr [:email-invalid-format])}]
        [:<>
         [ui/FormInput {:name      :fullname
                        :label     (@tr [:full-name])
                        :required  true
                        :on-change (partial fv/event->names->value! form)
                        :on-blur   (partial fv/event->show-message form)
                        :error     (fv/?show-message form :fullname spec->msg)}]
         [ui/FormGroup
          [ui/FormInput {:name      :street-address
                         :label     (@tr [:street-address])
                         :required  true
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :street-address spec->msg)
                         :width     10}]
          [ui/FormInput {:name      :postal-code
                         :label     (@tr [:postal-code])
                         :required  true
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :postal-code spec->msg)
                         :width     6}]]
         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name      :city
                         :label     (@tr [:city])
                         :required  true
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :city spec->msg)}]
          [ui/FormDropdown {:name        :country
                            :label       (@tr [:country])
                            :search      true
                            :selection   true
                            :required    true
                            :on-change   (fn [_ data]
                                           (let [name  (keyword (.-name data))
                                                 value (.-value data)]
                                             (swap! form #(assoc-in % [:names->value name] value))
                                             (fv/validate-form form)))
                            :error       (fv/?show-message form :country spec->msg)
                            :options     [{:text "Afghanistan", :flag "af", :key "af", :value "AF"} {:text "Aland Islands", :flag "ax", :key "ax", :value "AX"} {:text "Albania", :flag "al", :key "al", :value "AL"} {:text "Algeria", :flag "dz", :key "dz", :value "DZ"} {:text "American Samoa", :flag "as", :key "as", :value "AS"} {:text "Andorra", :flag "ad", :key "ad", :value "AD"} {:text "Angola", :flag "ao", :key "ao", :value "AO"} {:text "Anguilla", :flag "ai", :key "ai", :value "AI"} {:text "Antigua", :flag "ag", :key "ag", :value "AG"} {:text "Argentina", :flag "ar", :key "ar", :value "AR"} {:text "Armenia", :flag "am", :key "am", :value "AM"} {:text "Aruba", :flag "aw", :key "aw", :value "AW"} {:text "Australia", :flag "au", :key "au", :value "AU"} {:text "Austria", :flag "at", :key "at", :value "AT"} {:text "Azerbaijan", :flag "az", :key "az", :value "AZ"} {:text "Bahamas", :flag "bs", :key "bs", :value "BS"} {:text "Bahrain", :flag "bh", :key "bh", :value "BH"} {:text "Bangladesh", :flag "bd", :key "bd", :value "BD"} {:text "Barbados", :flag "bb", :key "bb", :value "BB"} {:text "Belarus", :flag "by", :key "by", :value "BY"} {:text "Belgium", :flag "be", :key "be", :value "BE"} {:text "Belize", :flag "bz", :key "bz", :value "BZ"} {:text "Benin", :flag "bj", :key "bj", :value "BJ"} {:text "Bermuda", :flag "bm", :key "bm", :value "BM"} {:text "Bhutan", :flag "bt", :key "bt", :value "BT"} {:text "Bolivia", :flag "bo", :key "bo", :value "BO"} {:text "Bosnia", :flag "ba", :key "ba", :value "BA"} {:text "Botswana", :flag "bw", :key "bw", :value "BW"} {:text "Bouvet Island", :flag "bv", :key "bv", :value "BV"} {:text "Brazil", :flag "br", :key "br", :value "BR"} {:text "British Virgin Islands", :flag "vg", :key "vg", :value "VG"} {:text "Brunei", :flag "bn", :key "bn", :value "BN"} {:text "Bulgaria", :flag "bg", :key "bg", :value "BG"} {:text "Burkina Faso", :flag "bf", :key "bf", :value "BF"} {:text "Burma", :flag "mm", :key "mm", :value "MM"} {:text "Burundi", :flag "bi", :key "bi", :value "BI"} {:text "Caicos Islands", :flag "tc", :key "tc", :value "TC"} {:text "Cambodia", :flag "kh", :key "kh", :value "KH"} {:text "Cameroon", :flag "cm", :key "cm", :value "CM"} {:text "Canada", :flag "ca", :key "ca", :value "CA"} {:text "Cape Verde", :flag "cv", :key "cv", :value "CV"} {:text "Cayman Islands", :flag "ky", :key "ky", :value "KY"} {:text "Central African Republic", :flag "cf", :key "cf", :value "CF"} {:text "Chad", :flag "td", :key "td", :value "TD"} {:text "Chile", :flag "cl", :key "cl", :value "CL"} {:text "China", :flag "cn", :key "cn", :value "CN"} {:text "Christmas Island", :flag "cx", :key "cx", :value "CX"} {:text "Cocos Islands", :flag "cc", :key "cc", :value "CC"} {:text "Colombia", :flag "co", :key "co", :value "CO"} {:text "Comoros", :flag "km", :key "km", :value "KM"} {:text "Congo", :flag "cd", :key "cd", :value "CD"} {:text "Congo Brazzaville", :flag "cg", :key "cg", :value "CG"} {:text "Cook Islands", :flag "ck", :key "ck", :value "CK"} {:text "Costa Rica", :flag "cr", :key "cr", :value "CR"} {:text "Cote Divoire", :flag "ci", :key "ci", :value "CI"} {:text "Croatia", :flag "hr", :key "hr", :value "HR"} {:text "Cuba", :flag "cu", :key "cu", :value "CU"} {:text "Cyprus", :flag "cy", :key "cy", :value "CY"} {:text "Czech Republic", :flag "cz", :key "cz", :value "CZ"} {:text "Denmark", :flag "dk", :key "dk", :value "DK"} {:text "Djibouti", :flag "dj", :key "dj", :value "DJ"} {:text "Dominica", :flag "dm", :key "dm", :value "DM"} {:text "Dominican Republic", :flag "do", :key "do", :value "DO"} {:text "Ecuador", :flag "ec", :key "ec", :value "EC"} {:text "Egypt", :flag "eg", :key "eg", :value "EG"} {:text "El Salvador", :flag "sv", :key "sv", :value "SV"} {:text "Equatorial Guinea", :flag "gq", :key "gq", :value "GQ"} {:text "Eritrea", :flag "er", :key "er", :value "ER"} {:text "Estonia", :flag "ee", :key "ee", :value "EE"} {:text "Ethiopia", :flag "et", :key "et", :value "ET"} {:text "Europeanunion", :flag "eu", :key "eu", :value "EU"} {:text "Falkland Islands", :flag "fk", :key "fk", :value "FK"} {:text "Faroe Islands", :flag "fo", :key "fo", :value "FO"} {:text "Fiji", :flag "fj", :key "fj", :value "FJ"} {:text "Finland", :flag "fi", :key "fi", :value "FI"} {:text "France", :flag "fr", :key "fr", :value "FR"} {:text "French Guiana", :flag "gf", :key "gf", :value "GF"} {:text "French Polynesia", :flag "pf", :key "pf", :value "PF"} {:text "French Territories", :flag "tf", :key "tf", :value "TF"} {:text "Gabon", :flag "ga", :key "ga", :value "GA"} {:text "Gambia", :flag "gm", :key "gm", :value "GM"} {:text "Georgia", :flag "ge", :key "ge", :value "GE"} {:text "Germany", :flag "de", :key "de", :value "DE"} {:text "Ghana", :flag "gh", :key "gh", :value "GH"} {:text "Gibraltar", :flag "gi", :key "gi", :value "GI"} {:text "Greece", :flag "gr", :key "gr", :value "GR"} {:text "Greenland", :flag "gl", :key "gl", :value "GL"} {:text "Grenada", :flag "gd", :key "gd", :value "GD"} {:text "Guadeloupe", :flag "gp", :key "gp", :value "GP"} {:text "Guam", :flag "gu", :key "gu", :value "GU"} {:text "Guatemala", :flag "gt", :key "gt", :value "GT"} {:text "Guinea", :flag "gn", :key "gn", :value "GN"} {:text "Guinea-Bissau", :flag "gw", :key "gw", :value "GW"} {:text "Guyana", :flag "gy", :key "gy", :value "GY"} {:text "Haiti", :flag "ht", :key "ht", :value "HT"} {:text "Heard Island", :flag "hm", :key "hm", :value "HM"} {:text "Honduras", :flag "hn", :key "hn", :value "HN"} {:text "Hong Kong", :flag "hk", :key "hk", :value "HK"} {:text "Hungary", :flag "hu", :key "hu", :value "HU"} {:text "Iceland", :flag "is", :key "is", :value "IS"} {:text "India", :flag "in", :key "in", :value "IN"} {:text "Indian Ocean Territory", :flag "io", :key "io", :value "IO"} {:text "Indonesia", :flag "id", :key "id", :value "ID"} {:text "Iran", :flag "ir", :key "ir", :value "IR"} {:text "Iraq", :flag "iq", :key "iq", :value "IQ"} {:text "Ireland", :flag "ie", :key "ie", :value "IE"} {:text "Israel", :flag "il", :key "il", :value "IL"} {:text "Italy", :flag "it", :key "it", :value "IT"} {:text "Jamaica", :flag "jm", :key "jm", :value "JM"} {:text "Jan Mayen", :flag "sj", :key "sj", :value "SJ"} {:text "Japan", :flag "jp", :key "jp", :value "JP"} {:text "Jordan", :flag "jo", :key "jo", :value "JO"} {:text "Kazakhstan", :flag "kz", :key "kz", :value "KZ"} {:text "Kenya", :flag "ke", :key "ke", :value "KE"} {:text "Kiribati", :flag "ki", :key "ki", :value "KI"} {:text "Kuwait", :flag "kw", :key "kw", :value "KW"} {:text "Kyrgyzstan", :flag "kg", :key "kg", :value "KG"} {:text "Laos", :flag "la", :key "la", :value "LA"} {:text "Latvia", :flag "lv", :key "lv", :value "LV"} {:text "Lebanon", :flag "lb", :key "lb", :value "LB"} {:text "Lesotho", :flag "ls", :key "ls", :value "LS"} {:text "Liberia", :flag "lr", :key "lr", :value "LR"} {:text "Libya", :flag "ly", :key "ly", :value "LY"} {:text "Liechtenstein", :flag "li", :key "li", :value "LI"} {:text "Lithuania", :flag "lt", :key "lt", :value "LT"} {:text "Luxembourg", :flag "lu", :key "lu", :value "LU"} {:text "Macau", :flag "mo", :key "mo", :value "MO"} {:text "Macedonia", :flag "mk", :key "mk", :value "MK"} {:text "Madagascar", :flag "mg", :key "mg", :value "MG"} {:text "Malawi", :flag "mw", :key "mw", :value "MW"} {:text "Malaysia", :flag "my", :key "my", :value "MY"} {:text "Maldives", :flag "mv", :key "mv", :value "MV"} {:text "Mali", :flag "ml", :key "ml", :value "ML"} {:text "Malta", :flag "mt", :key "mt", :value "MT"} {:text "Marshall Islands", :flag "mh", :key "mh", :value "MH"} {:text "Martinique", :flag "mq", :key "mq", :value "MQ"} {:text "Mauritania", :flag "mr", :key "mr", :value "MR"} {:text "Mauritius", :flag "mu", :key "mu", :value "MU"} {:text "Mayotte", :flag "yt", :key "yt", :value "YT"} {:text "Mexico", :flag "mx", :key "mx", :value "MX"} {:text "Micronesia", :flag "fm", :key "fm", :value "FM"} {:text "Moldova", :flag "md", :key "md", :value "MD"} {:text "Monaco", :flag "mc", :key "mc", :value "MC"} {:text "Mongolia", :flag "mn", :key "mn", :value "MN"} {:text "Montenegro", :flag "me", :key "me", :value "ME"} {:text "Montserrat", :flag "ms", :key "ms", :value "MS"} {:text "Morocco", :flag "ma", :key "ma", :value "MA"} {:text "Mozambique", :flag "mz", :key "mz", :value "MZ"} {:text "Namibia", :flag "na", :key "na", :value "NA"} {:text "Nauru", :flag "nr", :key "nr", :value "NR"} {:text "Nepal", :flag "np", :key "np", :value "NP"} {:text "Netherlands", :flag "nl", :key "nl", :value "NL"} {:text "Netherlandsantilles", :flag "an", :key "an", :value "AN"} {:text "New Caledonia", :flag "nc", :key "nc", :value "NC"} {:text "New Guinea", :flag "pg", :key "pg", :value "PG"} {:text "New Zealand", :flag "nz", :key "nz", :value "NZ"} {:text "Nicaragua", :flag "ni", :key "ni", :value "NI"} {:text "Niger", :flag "ne", :key "ne", :value "NE"} {:text "Nigeria", :flag "ng", :key "ng", :value "NG"} {:text "Niue", :flag "nu", :key "nu", :value "NU"} {:text "Norfolk Island", :flag "nf", :key "nf", :value "NF"} {:text "North Korea", :flag "kp", :key "kp", :value "KP"} {:text "Northern Mariana Islands", :flag "mp", :key "mp", :value "MP"} {:text "Norway", :flag "no", :key "no", :value "NO"} {:text "Oman", :flag "om", :key "om", :value "OM"} {:text "Pakistan", :flag "pk", :key "pk", :value "PK"} {:text "Palau", :flag "pw", :key "pw", :value "PW"} {:text "Palestine", :flag "ps", :key "ps", :value "PS"} {:text "Panama", :flag "pa", :key "pa", :value "PA"} {:text "Paraguay", :flag "py", :key "py", :value "PY"} {:text "Peru", :flag "pe", :key "pe", :value "PE"} {:text "Philippines", :flag "ph", :key "ph", :value "PH"} {:text "Pitcairn Islands", :flag "pn", :key "pn", :value "PN"} {:text "Poland", :flag "pl", :key "pl", :value "PL"} {:text "Portugal", :flag "pt", :key "pt", :value "PT"} {:text "Puerto Rico", :flag "pr", :key "pr", :value "PR"} {:text "Qatar", :flag "qa", :key "qa", :value "QA"} {:text "Reunion", :flag "re", :key "re", :value "RE"} {:text "Romania", :flag "ro", :key "ro", :value "RO"} {:text "Russia", :flag "ru", :key "ru", :value "RU"} {:text "Rwanda", :flag "rw", :key "rw", :value "RW"} {:text "Saint Helena", :flag "sh", :key "sh", :value "SH"} {:text "Saint Kitts and Nevis", :flag "kn", :key "kn", :value "KN"} {:text "Saint Lucia", :flag "lc", :key "lc", :value "LC"} {:text "Saint Pierre", :flag "pm", :key "pm", :value "PM"} {:text "Saint Vincent", :flag "vc", :key "vc", :value "VC"} {:text "Samoa", :flag "ws", :key "ws", :value "WS"} {:text "San Marino", :flag "sm", :key "sm", :value "SM"} {:text "Sandwich Islands", :flag "gs", :key "gs", :value "GS"} {:text "Sao Tome", :flag "st", :key "st", :value "ST"} {:text "Saudi Arabia", :flag "sa", :key "sa", :value "SA"} {:text "Scotland", :flag "gb sct", :key "gb sct", :value "GB SCT"} {:text "Senegal", :flag "sn", :key "sn", :value "SN"} {:text "Serbia", :flag "cs", :key "cs", :value "CS"} {:text "Serbia", :flag "rs", :key "rs", :value "RS"} {:text "Seychelles", :flag "sc", :key "sc", :value "SC"} {:text "Sierra Leone", :flag "sl", :key "sl", :value "SL"} {:text "Singapore", :flag "sg", :key "sg", :value "SG"} {:text "Slovakia", :flag "sk", :key "sk", :value "SK"} {:text "Slovenia", :flag "si", :key "si", :value "SI"} {:text "Solomon Islands", :flag "sb", :key "sb", :value "SB"} {:text "Somalia", :flag "so", :key "so", :value "SO"} {:text "South Africa", :flag "za", :key "za", :value "ZA"} {:text "South Korea", :flag "kr", :key "kr", :value "KR"} {:text "Spain", :flag "es", :key "es", :value "ES"} {:text "Sri Lanka", :flag "lk", :key "lk", :value "LK"} {:text "Sudan", :flag "sd", :key "sd", :value "SD"} {:text "Suriname", :flag "sr", :key "sr", :value "SR"} {:text "Swaziland", :flag "sz", :key "sz", :value "SZ"} {:text "Sweden", :flag "se", :key "se", :value "SE"} {:text "Switzerland", :flag "ch", :key "ch", :value "CH"} {:text "Syria", :flag "sy", :key "sy", :value "SY"} {:text "Taiwan", :flag "tw", :key "tw", :value "TW"} {:text "Tajikistan", :flag "tj", :key "tj", :value "TJ"} {:text "Tanzania", :flag "tz", :key "tz", :value "TZ"} {:text "Thailand", :flag "th", :key "th", :value "TH"} {:text "Timorleste", :flag "tl", :key "tl", :value "TL"} {:text "Togo", :flag "tg", :key "tg", :value "TG"} {:text "Tokelau", :flag "tk", :key "tk", :value "TK"} {:text "Tonga", :flag "to", :key "to", :value "TO"} {:text "Trinidad", :flag "tt", :key "tt", :value "TT"} {:text "Tunisia", :flag "tn", :key "tn", :value "TN"} {:text "Turkey", :flag "tr", :key "tr", :value "TR"} {:text "Turkmenistan", :flag "tm", :key "tm", :value "TM"} {:text "Tuvalu", :flag "tv", :key "tv", :value "TV"} {:text "U.A.E.", :flag "ae", :key "ae", :value "AE"} {:text "Uganda", :flag "ug", :key "ug", :value "UG"} {:text "Ukraine", :flag "ua", :key "ua", :value "UA"} {:text "United Kingdom", :flag "gb", :key "gb", :value "GB"} {:text "United States", :flag "us", :key "us", :value "US"} {:text "Uruguay", :flag "uy", :key "uy", :value "UY"} {:text "US Minor Islands", :flag "um", :key "um", :value "UM"} {:text "US Virgin Islands", :flag "vi", :key "vi", :value "VI"} {:text "Uzbekistan", :flag "uz", :key "uz", :value "UZ"} {:text "Vanuatu", :flag "vu", :key "vu", :value "VU"} {:text "Vatican City", :flag "va", :key "va", :value "VA"} {:text "Venezuela", :flag "ve", :key "ve", :value "VE"} {:text "Vietnam", :flag "vn", :key "vn", :value "VN"} {:text "Wales", :flag "gb wls", :key "gb wls", :value "GB WLS"} {:text "Wallis and Futuna", :flag "wf", :key "wf", :value "WF"} {:text "Western Sahara", :flag "eh", :key "eh", :value "EH"} {:text "Yemen", :flag "ye", :key "ye", :value "YE"} {:text "Zambia", :flag "zm", :key "zm", :value "ZM"} {:text "Zimbabwe", :flag "zw", :key "zw", :value "ZW"}]
                            :placeholder (@tr [:select-country])}]]

         [ui/FormGroup {:widths 2}
          (when @is-group?
            [ui/FormInput {:name          :email
                           :label         (str/capitalize (@tr [:email]))
                           :required      true
                           :on-change     (partial fv/event->names->value! form)
                           :on-blur       (partial fv/event->show-message form)
                           :error         (fv/?show-message form :email spec->msg)
                           :auto-complete "off"
                           :width         8}])
          [ui/FormInput {:name          :coupon
                         :label         (@tr [:coupon-code])
                         :on-change     (partial fv/event->names->value! form)
                         :on-blur       (partial fv/event->show-message form)
                         :error         (fv/?show-message form :coupon spec->msg)
                         :auto-complete "off"
                         :width         8}]]]))))


(defn customer-form->customer
  [form]
  (let [{:keys [fullname payment-method coupon email] :as customer} (:names->value @form)]
    (cond-> {:fullname fullname
             :address  (select-keys customer [:street-address
                                              :city
                                              :country
                                              :postal-code])}
            payment-method (assoc :payment-method payment-method)
            email (assoc :email email)
            (not (str/blank? coupon)) (assoc :coupon coupon))))


(defn SubscribeButton
  []
  (let [tr                       (subscribe [::i18n-subs/tr])
        stripe                   (subscribe [::main-subs/stripe])
        loading-customer?        (subscribe [::subs/loading? :customer])
        open?                    (subscribe [::subs/modal-open? :subscribe])
        loading-subscription?    (subscribe [::subs/loading? :subscription])
        session                  (subscribe [::session-subs/session])
        error                    (subscribe [::subs/error-message])
        loading-create-customer? (subscribe [::subs/loading? :create-customer])
        customer                 (subscribe [::subs/customer])
        subscription             (subscribe [::subs/subscription])
        is-group?                (subscribe [::session-subs/active-claim-is-group?])
        form-conf                {:form-spec    (if @is-group?
                                                  ::customer-with-email
                                                  ::customer)
                                  :names->value {:fullname       ""
                                                 :street-address ""
                                                 :city           ""
                                                 :country        ""
                                                 :postal-code    ""}}
        form                     (fv/init-form form-conf)]
    (dispatch [::events/get-pricing-catalogue])
    (fn []
      [:<>
       [ui/Modal
        {:open       @open?
         :size       "small"
         :on-close   #(do
                        (reset! form @(fv/init-form form-conf))
                        (dispatch [::events/close-modal]))
         :close-icon true}
        [ui/ModalHeader (@tr [:subscribe])]
        [ui/ModalContent
         [ui/Form {:error   (boolean @error)
                   :loading @loading-create-customer?}
          [ui/Message {:error   true
                       :header  (@tr [:something-went-wrong])
                       :content @error}]
          (if @customer
            (@tr [:confirm-subscribe-text])
            [CustomerFormFields form])]]
        [ui/ModalActions
         [ui/Button {:animated "vertical"
                     :primary  true
                     :on-click #(if @customer
                                  (dispatch [::events/create-subscription])
                                  (when (fv/validate-form-and-show? form)
                                    (dispatch [::events/create-customer (customer-form->customer
                                                                          form)])))
                     :disabled (or (not @stripe)
                                   @loading-create-customer?
                                   (if @customer
                                     false
                                     (not (fv/form-valid? form))))}
          [ui/ButtonContent {:hidden true} [ui/Icon {:name "shop"}]]
          [ui/ButtonContent {:visible true} (@tr [:subscribe])]]]]
       [ui/Button {:primary  true
                   :circular true
                   :basic    true
                   :loading  @loading-customer?
                   :disabled (or @loading-subscription? (some? (:status @subscription)))
                   :on-click (if @session
                               #(dispatch [::events/open-modal :subscribe])
                               #(dispatch [::history-events/navigate "sign-up"]))}
        (if @customer
          (@tr [:subscribe])
          (@tr [:try-nuvla-for-14-days]))]])))



(defn Subscription
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        locale       (subscribe [::i18n-subs/locale])
        subscription (subscribe [::subs/subscription])
        loading?     (subscribe [::subs/loading? :subscription])]
    (fn []
      (let [{:keys [status start-date trial-start trial-end
                    current-period-start current-period-end]} @subscription]
        [ui/Segment {:padded  true
                     :color   "red"
                     :loading @loading?
                     :style   {:height "100%"}}
         [ui/Header {:as :h2 :dividing true} (@tr [:subscription])]
         (if status
           [ui/Table {:basic "very"}
            [ui/TableBody
             [ui/TableRow
              [ui/TableCell {:width 5} [:b (str/capitalize (@tr [:status]))]]
              [ui/TableCell {:width 11} (str/capitalize status)]]
             [ui/TableRow
              [ui/TableCell [:b (@tr [:start-date])]]
              [ui/TableCell (some-> start-date (time/time->format "LLL" @locale))]]
             (when (= status "trialing")
               [:<>
                [ui/TableRow
                 [ui/TableCell [:b (@tr [:trial-start-date])]]
                 [ui/TableCell (some-> trial-start
                                       (time/time->format "LLL" @locale))]]
                [ui/TableRow
                 [ui/TableCell [:b (@tr [:trial-end-date])]]
                 [ui/TableCell (some-> trial-end (time/time->format "LLL" @locale))]]])
             [ui/TableRow
              [ui/TableCell [:b (@tr [:current-period-start])]]
              [ui/TableCell (some-> current-period-start (time/time->format "LLL" @locale))]]
             [ui/TableRow
              [ui/TableCell [:b (@tr [:current-period-end])]]
              [ui/TableCell (some-> current-period-end (time/time->format "LLL" @locale))]]]]

           [ui/Grid {:text-align     "center"
                     :vertical-align "middle"
                     :style          {:height "100%"}}
            [ui/GridColumn
             [ui/Header {:as :h3, :icon true, :disabled true}
              [ui/Icon {:className "fad fa-money-check-edit"}]
              (@tr [:not-subscribed-yet])]
             [:br]
             [SubscribeButton]]
            ])]))))


(defn AddPaymentMethodButton
  [open?]
  (let [tr                        (subscribe [::i18n-subs/tr])
        stripe                    (subscribe [::main-subs/stripe])
        loading-setup-intent?     (subscribe [::subs/loading? :create-setup-intent])
        loading-confirm-setup?    (subscribe [::subs/loading? :confirm-setup-intent])
        disabled?                 (subscribe [::subs/cannot-create-setup-intent?])
        error                     (subscribe [::subs/error-message])
        payment-form              (r/atom "card")
        elements                  (r/atom nil)
        card-validation-error-msg (r/atom nil)
        card-info-completed?      (r/atom false)]
    (fn [open?]
      [:<>
       [ui/Modal
        {:open       open?
         :size       "small"
         :on-close   #(dispatch [::events/close-modal])
         :close-icon true}
        [ui/ModalHeader (@tr [:add-payment-method])]
        [ui/ModalContent
         [ui/Form {:error   (boolean @error)
                   :loading @loading-confirm-setup?}
          [ui/Message {:error   true
                       :header  (@tr [:something-went-wrong])
                       :content @error}]
          [ui/FormGroup {:inline true}
           [:label (@tr [:payment-method])]
           [ui/FormRadio {:label     (@tr [:credit-card])
                          :checked   (= @payment-form "card")
                          :on-change (ui-callback/value #(reset! payment-form "card"))}]
           #_[ui/FormRadio {:label     (@tr [:bank-account])
                            :checked   (= @payment-form "sepa_debit")
                            :on-change (ui-callback/value #(reset! payment-form "sepa_debit"))}]]
          [ui/FormField {:width 9}
           [PaymentMethodInput
            (cond-> {:type         @payment-form
                     :on-change    (fn [event]
                                     (reset! card-validation-error-msg
                                             (some-> event .-error .-message))
                                     (reset! card-info-completed? (.-complete event)))
                     :set-elements #(reset! elements %)}
                    (= @payment-form "sepa_debit") (assoc :options {:supportedCountries ["SEPA"]
                                                                    :placeholderCountry "CH"}))]
           (when @card-validation-error-msg
             [ui/Label {:basic true, :color "red", :pointing true} @card-validation-error-msg])]]]
        [ui/ModalActions
         [ui/Button {:primary  true
                     :on-click #(dispatch [::events/confirm-card-setup @payment-form @elements])
                     :disabled (or
                                 (not @elements)
                                 (not @stripe)
                                 (not @card-info-completed?)
                                 @card-validation-error-msg
                                 @loading-setup-intent?)}
          (str/capitalize (@tr [:add]))]]]
       [ui/Button {:primary  true
                   :circular true
                   :basic    true
                   :size     "small"
                   :disabled @disabled?
                   :on-click #(do
                                (dispatch [::events/create-setup-intent])
                                (dispatch [::events/open-modal :add-payment-method]))}
        [ui/Icon {:name "plus square outline"}]
        (str/capitalize (@tr [:add]))]])))


(defn PaymentMethods
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        loading?               (subscribe [::subs/loading? :payment-methods])
        open?                  (subscribe [::subs/modal-open? :add-payment-method])
        customer               (subscribe [::subs/customer])
        cards-bank-accounts    (subscribe [::subs/cards-bank-accounts])
        default-payment-method (subscribe [::subs/default-payment-method])]
    (dispatch [::events/list-payment-methods])
    (fn []
      (let [default            @default-payment-method
            set-as-default-str (@tr [:set-as-default])
            delete-str         (str/capitalize (@tr [:delete]))]
        [ui/Segment {:padded  true
                     :color   "purple"
                     :loading @loading?
                     :style   {:height "100%"}}
         [ui/Header {:as :h2 :dividing true} (@tr [:payment-methods])]
         (if @cards-bank-accounts
           [ui/Table {:basic "very"}
            [ui/TableBody
             (for [{:keys [last4 brand payment-method exp-month exp-year]} @cards-bank-accounts]
               (let [is-default? (= default payment-method)]
                 ^{:key (str payment-method)}
                 [ui/TableRow
                  [ui/TableCell
                   [ui/Icon {:name (case brand
                                     "visa" "cc visa"
                                     "mastercard" "cc mastercard"
                                     "amex" "cc amex"
                                     "iban" "building"
                                     "payment")
                             :size "large"}]
                   (str/upper-case brand)]
                  [ui/TableCell "•••• " last4 " "
                   (when is-default?
                     [ui/Label {:size :tiny :circular true :color "blue"} "default"])]
                  [ui/TableCell {:style {:color "grey"}}
                   (when (and exp-month exp-year)
                     (str (general-utils/format "%02d" exp-month) "/" exp-year))]
                  [ui/TableCell
                   [ui/ButtonGroup {:basic true :size "small" :icon true :floated "right"}
                    (when-not is-default?
                      [ui/Popup
                       {:position "top center"
                        :content  set-as-default-str
                        :trigger  (r/as-element
                                    [ui/Button
                                     {:on-click #(dispatch [::events/set-default-payment-method
                                                            payment-method])}
                                     [ui/Icon {:name "pin"}]])}])
                    [ui/Popup
                     {:position "top center"
                      :content  delete-str
                      :trigger  (r/as-element [ui/Button
                                               {:on-click #(dispatch
                                                             [::events/detach-payment-method
                                                              payment-method])}
                                               [ui/Icon {:name "trash", :color "red"}]])}]]]]))
             [ui/TableRow
              [ui/TableCell {:col-span 4}
               ^{:key (random-uuid)}
               [AddPaymentMethodButton @open?]]]]]

           [ui/Grid {:text-align     "center"
                     :vertical-align "middle"
                     :style          {:height "100%"}}
            [ui/GridColumn
             [ui/Header {:as :h3, :icon true, :disabled true}
              [ui/Icon {:className "fad fa-credit-card"}]
              (@tr [:payment-method])]
             (when customer
               [:<>
                [:br]
                [AddPaymentMethodButton @open?]])]])]))))


(defn format-currency
  [currency amount]
  (str (if (= currency "eur") "€" currency)
       " " (general-utils/format "%.2f" amount)))


(defn CurrentConsumption
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        loading?         (subscribe [::subs/loading? :upcoming-invoice])
        upcoming-invoice (subscribe [::subs/upcoming-invoice])
        upcoming-lines   (subscribe [::subs/upcoming-invoice-lines])]
    (dispatch [::events/upcoming-invoice])
    (fn []
      (let [locale @(subscribe [::i18n-subs/locale])
            {upcoming-total    :total
             upcoming-subtotal :subtotal
             upcoming-currency :currency
             {:keys [coupon]}  :discount} @upcoming-invoice]
        [ui/Segment {:padded  true
                     :color   "brown"
                     :loading @loading?
                     :style   {:height "100%"}}
         [ui/Header {:as :h2 :dividing true} (@tr [:current-consumption])]
         (if upcoming-total
           [ui/Table
            [ui/TableHeader
             [ui/TableRow
              [ui/TableHeaderCell (str/capitalize (@tr [:description]))]
              [ui/TableHeaderCell {:text-align "right"} (@tr [:amount])]]]
            [ui/TableBody
             (for [[period lines] @upcoming-lines]
               ^{:key (str period)}
               [:<>
                [ui/TableRow
                 [ui/TableCell {:col-span 2, :style {:color "grey"}}
                  (str (some-> period :start (time/time->format "LL" locale))
                       " - "
                       (some-> period :end (time/time->format "LL" locale)))]]
                (for [{:keys [description amount currency]} lines]
                  ^{:key (str period description)}
                  [ui/TableRow
                   [ui/TableCell description]
                   [ui/TableCell {:text-align "right"}
                    (format-currency currency amount)]])])

             [ui/TableRow {:active true}
              [ui/TableCell [:i [:b (@tr [:subtotal])]]]
              [ui/TableCell {:text-align "right"}
               [:b [:i (format-currency upcoming-currency upcoming-subtotal)]]]]
             (when coupon
               (let [{:keys [percent-off amount-off currency]} coupon]
                 [ui/TableRow
                  [ui/TableCell
                   [:i (str (:name coupon) " ("
                            (when percent-off
                              (str percent-off "%"))
                            (when amount-off
                              (format-currency currency amount-off))
                            " "
                            (@tr [:reduction-off])
                            ")")]]
                  [ui/TableCell {:text-align "right"}
                   [:i (format-currency upcoming-currency
                                        (- upcoming-total upcoming-subtotal))]]]))
             [ui/TableRow {:active true}
              [ui/TableCell [:b (str/capitalize (@tr [:total]))]]
              [ui/TableCell {:text-align "right"}
               [:b (format-currency upcoming-currency upcoming-total)]]]]]
           [ui/Grid {:text-align     "center"
                     :vertical-align "middle"
                     :style          {:height "100%"}}
            [ui/GridColumn
             [ui/Header {:as :h3, :icon true, :disabled true}
              [ui/Icon {:className "fad fa-file-invoice"}]
              (@tr [:not-any])]]])]))))


(defn Invoices
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading? :invoices])
        invoices (subscribe [::subs/invoices])]
    (dispatch [::events/list-invoices])
    (fn []
      (let [locale @(subscribe [::i18n-subs/locale])]
        [ui/Segment {:padded  true
                     :color   "yellow"
                     :loading @loading?
                     :style   {:height "100%"}}
         [ui/Header {:as :h2 :dividing true} (@tr [:invoices])]
         (if @invoices
           [ui/Table
            [ui/TableHeader
             [ui/TableRow
              [ui/TableHeaderCell (str/capitalize (@tr [:created]))]
              [ui/TableHeaderCell (str/capitalize (@tr [:status]))]
              [ui/TableHeaderCell (@tr [:due-date])]
              [ui/TableHeaderCell (str/capitalize (@tr [:total]))]
              [ui/TableHeaderCell "PDF"]]]
            [ui/TableBody
             (for [{:keys [number created status due-date invoice-pdf currency total]} @invoices]
               ^{:key (str number)}
               [ui/TableRow
                [ui/TableCell (some-> created (time/time->format "LL" locale))]
                [ui/TableCell (str/capitalize status)]
                [ui/TableCell (if due-date (some-> due-date (time/time->format "LL" locale)) "-")]
                [ui/TableCell (format-currency currency total)]
                [ui/TableCell
                 (when invoice-pdf
                   [ui/Button {:basic true
                               :icon  "download"
                               :href  invoice-pdf}])]])]]
           [ui/Grid {:text-align     "center"
                     :vertical-align "middle"
                     :style          {:height "100%"}}
            [ui/GridColumn
             [ui/Header {:as :h3, :icon true, :disabled true}
              [ui/Icon {:className "fad fa-file-invoice-dollar"}]
              (@tr [:not-any])]]])]))))


(defn AddCouponButton
  [open?]
  (let [tr          (subscribe [::i18n-subs/tr])
        loading?    (subscribe [::subs/loading? :add-coupon])
        error       (subscribe [::subs/error-message])
        coupon-code (r/atom nil)]
    (fn [open?]
      [:<>
       [ui/Modal
        {:open       open?
         :size       "small"
         :on-close   #(dispatch [::events/close-modal])
         :close-icon true}
        [ui/ModalHeader (@tr [:add-coupon])]
        [ui/ModalContent
         [ui/Form {:error   (boolean @error)
                   :loading @loading?}
          [ui/Message {:error   true
                       :header  (@tr [:something-went-wrong])
                       :content @error}]

          [ui/FormInput {:label      (@tr [:coupon-code])
                         :on-change  (ui-callback/value #(reset! coupon-code %))
                         :auto-focus true
                         :focus      true}]]]
        [ui/ModalActions
         [ui/Button {:primary  true
                     :on-click #(dispatch [::events/add-coupon @coupon-code])
                     :disabled (or @loading?
                                   (str/blank? coupon-code))}
          (str/capitalize (@tr [:add]))]]]
       [ui/Button {:primary  true
                   :circular true
                   :basic    true
                   :size     "small"
                   :on-click #(dispatch [::events/open-modal :add-coupon])}
        [ui/Icon {:name "plus square outline"}]
        (str/capitalize (@tr [:add]))]])))


(defn Coupon
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        loading?      (subscribe [::subs/loading? :customer-info])
        customer-info (subscribe [::subs/customer-info])
        open?         (subscribe [::subs/modal-open? :add-coupon])]
    (dispatch [::events/customer-info])
    (fn []
      (let [{:keys [name percent-off currency amount-off
                    duration duration-in-month] :as coupon} (:coupon @customer-info)]
        [ui/Segment {:padded  true
                     :color   "green"
                     :loading @loading?
                     :style   {:height "100%"}}
         [ui/Header {:as :h2 :dividing true} (@tr [:coupon])]
         (if coupon
           [ui/Table {:basic "very"}
            [ui/TableBody
             [ui/TableRow
              [ui/TableCell {:width 5} [:b (str/capitalize (@tr [:name]))]]
              [ui/TableCell {:width 11} name]]
             (when percent-off
               [ui/TableRow
                [ui/TableCell [:b (@tr [:percent-off])]]
                [ui/TableCell (str percent-off "%")]])
             (when amount-off
               [ui/TableRow
                [ui/TableCell [:b (@tr [:amount])]]
                [ui/TableCell (format-currency currency amount-off)]])
             (when duration-in-month
               [ui/TableRow
                [ui/TableCell [:b (@tr [:duration-in-month])]]
                [ui/TableCell duration-in-month]])
             (when duration
               [ui/TableRow
                [ui/TableCell [:b (@tr [:duration])]]
                [ui/TableCell (str/capitalize duration)]])
             [ui/TableRow
              [ui/TableCell {:col-span 2}
               [ui/Popup
                {:position "top center"
                 :content  (str/capitalize (@tr [:delete]))
                 :trigger  (r/as-element [ui/Button {:basic    true
                                                     :size     "small"
                                                     :icon     true
                                                     :on-click #(dispatch [::events/remove-coupon])}
                                          [ui/Icon {:name "trash", :color "red"}]])}]]]]]
           [ui/Grid {:text-align     "center"
                     :vertical-align "middle"
                     :style          {:height "100%"}}
            [ui/GridColumn
             [ui/Header {:as :h3, :icon true, :disabled true}
              [ui/Icon {:className "fad fa-ticket"}]
              (@tr [:not-any])]
             (when @customer-info
               [:<>
                [:br]
                ^{:key (random-uuid)}
                [AddCouponButton @open?]])]])]))))




(defn DashboradVendor
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        vendor (subscribe [::subs/vendor])]
    (when (general-utils/can-operation? "dashboard" @vendor)
      [ui/Form {:action (str @cimi-fx/NUVLA_URL "/api/" (:id @vendor) "/dashboard")
                :method "post"
                :style  {:color "grey"}}
       [ui/Button {:type "submit", :primary true} (@tr [:sales-dashboard])]])))


(defn StripeConnect
  []
  [ui/Form {:action (str @cimi-fx/NUVLA_URL "/api/vendor")
            :method "post"
            :style  {:color "grey"}}
   [:input {:hidden        true
            :name          "redirect-url"
            :default-value (str @config/path-prefix "/profile")}]
   [:input {:type "image"
            :src  "/ui/images/stripe-connect.png"
            :alt  "Stripe connect"}]])



(defn Vendor
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading? :vendor])
        vendor   (subscribe [::subs/vendor])]
    (dispatch [::events/search-existing-vendor])
    (fn []
      [ui/Segment {:padded  true
                   :color   "blue"
                   :loading @loading?
                   :style   {:height "100%"}}
       [ui/Header {:as :h2 :dividing true} (@tr [:vendor])]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true}
          [ui/Icon {:className "fad fa-envelope-open-dollar"}]
          (@tr [:vendor-getting-paid])]
         [:br]
         (if @vendor
           [DashboradVendor]
           [StripeConnect])
         ]]]
      )))


(defn BillingContact
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        loading?      (subscribe [::subs/loading? :customer-info])
        customer-info (subscribe [::subs/customer-info])]
    (dispatch [::events/customer-info])
    (fn []
      (let [{:keys [street-address city country postal-code]} (:address @customer-info)
            fullname (:fullname @customer-info)]
        [ui/Segment {:padded  true
                     :color   "grey"
                     :loading @loading?
                     :style   {:height "100%"}}
         [ui/Header {:as :h2 :dividing true} (@tr [:billing-contact])]
         [ui/Table {:basic "very"}
          [ui/TableBody
           [ui/TableRow
            [ui/TableCell {:width 5} [:b (str/capitalize (@tr [:name]))]]
            [ui/TableCell {:width 11} fullname]]
           [ui/TableRow
            [ui/TableCell [:b (@tr [:street-address])]]
            [ui/TableCell street-address [:br] postal-code " - " city [:br] country]]
           ]]
         ]))))


(defn Content
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        cred-pass (subscribe [::subs/credential-password])
        stripe    (subscribe [::main-subs/stripe])
        session   (subscribe [::session-subs/session])
        is-admin? (subscribe [::session-subs/is-admin?])
        customer  (subscribe [::subs/customer])]
    (dispatch [::events/init])
    (fn []
      (let [show-subscription      (and @stripe @session (not @is-admin?))
            show-customer-sections (and show-subscription @customer)]
        [:<>
         [uix/PageHeader "user" (str/capitalize (@tr [:profile]))]
         [ui/Menu {:borderless true}
          [ui/MenuItem {:disabled (nil? @cred-pass)
                        :content  (str/capitalize (@tr [:change-password]))
                        :on-click #(dispatch [::events/open-modal :change-password])}]]
         [ui/Grid {:stackable true}

          [ui/GridRow {:columns 2}
           [ui/GridColumn
            [Session]]
           [ui/GridColumn
            (when show-subscription
              [Subscription])]]
          (when show-customer-sections
            [:<>
             [ui/GridRow {:columns 2}
              [ui/GridColumn
               [CurrentConsumption]]
              [ui/GridColumn
               [Invoices]]]
             [ui/GridRow {:columns 2}
              [ui/GridColumn
               [Coupon]]
              [ui/GridColumn
               [BillingContact]]]])
          (when show-subscription
            [ui/GridRow {:columns 2}
             [ui/GridColumn
              [Vendor]]
             (when show-customer-sections
               [ui/GridColumn
              [PaymentMethods]])])]]))))

(defmethod panel/render :profile
  [path]
  [:div
   [Content]
   [modal-change-password]])
