(ns sixsq.nuvla.ui.db.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.application.spec :as application]
    [sixsq.nuvla.ui.appstore.spec :as appstore]
    [sixsq.nuvla.ui.authn.spec :as authn]
    [sixsq.nuvla.ui.cimi-detail.spec :as api-detail]
    [sixsq.nuvla.ui.cimi.spec :as api]
    [sixsq.nuvla.ui.client.spec :as client]
    [sixsq.nuvla.ui.data.spec :as data]
    [sixsq.nuvla.ui.deployment-dialog.spec :as deployment-dialog]
    [sixsq.nuvla.ui.deployment.spec :as deployment]
    [sixsq.nuvla.ui.docs.spec :as docs]
    [sixsq.nuvla.ui.i18n.spec :as i18n]
    [sixsq.nuvla.ui.main.spec :as main]
    [sixsq.nuvla.ui.messages.spec :as messages]
    [sixsq.nuvla.ui.nuvlabox-detail.spec :as nuvlabox-detail]
    [sixsq.nuvla.ui.nuvlabox.spec :as nuvlabox]))


(s/def ::db (s/merge ::application/db
                     ::appstore/db
                     ::authn/db
                     ::api/db
                     ::api-detail/db
                     ::client/db
                     ::deployment/db
                     ::deployment-dialog/db
                     ::data/db
                     ::docs/db
                     ::i18n/db
                     ::main/db
                     ::messages/db
                     ::nuvlabox/db
                     ::nuvlabox-detail/db))


(def default-db
  (merge application/defaults
         appstore/defaults
         authn/defaults
         api/defaults
         api-detail/defaults
         data/defaults
         deployment/defaults
         deployment-dialog/defaults
         client/defaults
         docs/defaults
         i18n/defaults
         main/defaults
         messages/defaults
         nuvlabox/defaults
         nuvlabox-detail/defaults))
