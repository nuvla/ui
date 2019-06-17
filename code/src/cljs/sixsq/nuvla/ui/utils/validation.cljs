(ns sixsq.nuvla.ui.utils.validation
  (:require [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn validation-error-message
  [form-valid-spec]
  (let [tr          (subscribe [::i18n-subs/tr])
        form-valid? (subscribe [form-valid-spec])]
    [ui/Message {:hidden (boolean @form-valid?)
                 :error  true}
     [ui/MessageHeader (@tr [:validation-error])]
     [ui/MessageContent (@tr [:validation-error-message])]]))