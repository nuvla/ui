(ns sixsq.slipstream.webui.db.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.slipstream.webui.application.spec :as application]
    [sixsq.slipstream.webui.appstore.spec :as appstore]
    [sixsq.slipstream.webui.authn.spec :as authn]
    [sixsq.slipstream.webui.cimi-detail.spec :as api-detail]
    [sixsq.slipstream.webui.cimi.spec :as api]
    [sixsq.slipstream.webui.client.spec :as client]
    [sixsq.slipstream.webui.dashboard.spec :as dashboard]
    [sixsq.slipstream.webui.data.spec :as data]
    [sixsq.slipstream.webui.deployment-dialog.spec :as deployment-dialog]
    [sixsq.slipstream.webui.deployment.spec :as deployment]
    [sixsq.slipstream.webui.docs.spec :as docs]
    [sixsq.slipstream.webui.i18n.spec :as i18n]
    [sixsq.slipstream.webui.legacy-application.spec :as legacy-application]
    [sixsq.slipstream.webui.main.spec :as main]
    [sixsq.slipstream.webui.messages.spec :as messages]
    [sixsq.slipstream.webui.metrics.spec :as metrics]
    [sixsq.slipstream.webui.nuvlabox-detail.spec :as nuvlabox-detail]
    [sixsq.slipstream.webui.nuvlabox.spec :as nuvlabox]
    [sixsq.slipstream.webui.quota.spec :as quota]
    [sixsq.slipstream.webui.usage.spec :as usage]))


(s/def ::db (s/merge ::application/db
                     ::appstore/db
                     ::authn/db
                     ::api/db
                     ::api-detail/db
                     ::client/db
                     ::dashboard/db
                     ::deployment/db
                     ::deployment-dialog/db
                     ::data/db
                     ::docs/db
                     ::i18n/db
                     ::legacy-application/db
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
         dashboard/defaults
         docs/defaults
         i18n/defaults
         legacy-application/defaults
         main/defaults
         metrics/defaults
         messages/defaults
         nuvlabox/defaults
         nuvlabox-detail/defaults
         usage/defaults
         quota/defaults))
