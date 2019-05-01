(ns sixsq.nuvla.ui.db.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.acl.spec :as acl]
    [sixsq.nuvla.ui.apps-component.spec :as apps-component]
    [sixsq.nuvla.ui.apps-store.spec :as apps-store]
    [sixsq.nuvla.ui.apps.spec :as apps]
    [sixsq.nuvla.ui.authn.spec :as authn]
    [sixsq.nuvla.ui.cimi-detail.spec :as api-detail]
    [sixsq.nuvla.ui.cimi.spec :as api]
    [sixsq.nuvla.ui.client.spec :as client]
    [sixsq.nuvla.ui.credential.spec :as credential]
    [sixsq.nuvla.ui.data.spec :as data]
    [sixsq.nuvla.ui.deployment-dialog.spec :as deployment-dialog]
    [sixsq.nuvla.ui.deployment.spec :as deployment]
    [sixsq.nuvla.ui.docs.spec :as docs]
    [sixsq.nuvla.ui.i18n.spec :as i18n]
    [sixsq.nuvla.ui.infra-service.spec :as infra-service]
    [sixsq.nuvla.ui.main.spec :as main]
    [sixsq.nuvla.ui.messages.spec :as messages]
    [sixsq.nuvla.ui.nuvlabox-detail.spec :as nuvlabox-detail]
    [sixsq.nuvla.ui.nuvlabox.spec :as nuvlabox]
    [sixsq.nuvla.ui.profile.spec :as profile]))


(s/def ::db (s/merge ::acl/db
                     ::apps/db
                     ::apps-component/db
                     ::apps-store/db
                     ::authn/db
                     ::api/db
                     ::api-detail/db
                     ::client/db
                     ::credential/db
                     ::deployment/db
                     ::deployment-dialog/db
                     ::data/db
                     ::docs/db
                     ::i18n/db
                     ::infra-service/db
                     ::main/db
                     ::messages/db
                     ::nuvlabox/db
                     ::nuvlabox-detail/db
                     ::profile/db))


(def default-db
  (merge acl/defaults
         apps/defaults
         apps-component/defaults
         apps-store/defaults
         authn/defaults
         api/defaults
         api-detail/defaults
         data/defaults
         deployment/defaults
         deployment-dialog/defaults
         client/defaults
         credential/defaults
         docs/defaults
         i18n/defaults
         infra-service/defaults
         main/defaults
         messages/defaults
         nuvlabox/defaults
         nuvlabox-detail/defaults
         profile/defaults))
