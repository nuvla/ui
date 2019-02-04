(ns sixsq.nuvla.webui.db.spec
  (:require-macros [sixsq.nuvla.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.webui.application.spec :as application]
    [sixsq.nuvla.webui.appstore.spec :as appstore]
    [sixsq.nuvla.webui.authn.spec :as authn]
    [sixsq.nuvla.webui.cimi-detail.spec :as api-detail]
    [sixsq.nuvla.webui.cimi.spec :as api]
    [sixsq.nuvla.webui.client.spec :as client]
    [sixsq.nuvla.webui.data.spec :as data]
    [sixsq.nuvla.webui.deployment-dialog.spec :as deployment-dialog]
    [sixsq.nuvla.webui.deployment.spec :as deployment]
    [sixsq.nuvla.webui.docs.spec :as docs]
    [sixsq.nuvla.webui.i18n.spec :as i18n]
    [sixsq.nuvla.webui.main.spec :as main]
    [sixsq.nuvla.webui.messages.spec :as messages]
    [sixsq.nuvla.webui.metrics.spec :as metrics]
    [sixsq.nuvla.webui.nuvlabox-detail.spec :as nuvlabox-detail]
    [sixsq.nuvla.webui.nuvlabox.spec :as nuvlabox]
    [sixsq.nuvla.webui.quota.spec :as quota]
    [sixsq.nuvla.webui.usage.spec :as usage]))


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
                     ::metrics/db
                     ::messages/db
                     ::nuvlabox/db
                     ::nuvlabox-detail/db
                     ::usage/db))


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
         metrics/defaults
         messages/defaults
         nuvlabox/defaults
         nuvlabox-detail/defaults
         usage/defaults
         quota/defaults))
