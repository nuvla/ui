(ns sixsq.nuvla.ui.credentials.components
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.credentials.subs :as subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn CredentialCheckPopup
  [cred-id]
  (let [tr        (subscribe [::i18n-subs/tr])
        error-msg (subscribe [::subs/credential-check-error-msg cred-id])
        loading?  (subscribe [::subs/credential-check-loading? cred-id])
        valid?    (subscribe [::subs/credential-check-status-valid? cred-id])
        invalid?  (subscribe [::subs/credential-check-status-invalid? cred-id])
        status    (subscribe [::subs/credential-check-status cred-id])]
    (when (and @status (or @loading? (not @valid?)))
      [ui/Popup {:trigger  (r/as-element
                             [ui/Icon {:name    (cond
                                                  @loading? "circle notched"
                                                  (not @valid?) (if @invalid? "window close" "warning sign")
                                                  :else "world")
                                       :color   (cond
                                                  @loading? "black"
                                                  (not @valid?) (if @invalid? "red" "yellow")
                                                  :else "green")
                                       :loading @loading?
                                       :size    "large"}])
                 :header   (@tr [:connectivity-check])
                 :content  (cond
                             @loading? (@tr [:connectivity-check-in-progress])
                             (not @valid?) (str/capitalize (or @error-msg ""))
                             :else (@tr [:all-good]))
                 :wide     "very"
                 :position "top center"}])))