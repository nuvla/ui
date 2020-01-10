(ns sixsq.nuvla.ui.db.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.acl.spec :as acl]
    [sixsq.nuvla.ui.apps-application.spec :as apps-application]
    [sixsq.nuvla.ui.apps-component.spec :as apps-component]
    [sixsq.nuvla.ui.apps-store.spec :as apps-store]
    [sixsq.nuvla.ui.apps.spec :as apps]
    [sixsq.nuvla.ui.cimi-detail.spec :as api-detail]
    [sixsq.nuvla.ui.cimi.spec :as api]
    [sixsq.nuvla.ui.credentials.spec :as credential]
    [sixsq.nuvla.ui.dashboard.spec :as dashboard]
    [sixsq.nuvla.ui.data.spec :as data]
    [sixsq.nuvla.ui.deployment-dialog.spec :as deployment-dialog]
    [sixsq.nuvla.ui.docs.spec :as docs]
    [sixsq.nuvla.ui.edge-detail.spec :as edge-detail]
    [sixsq.nuvla.ui.edge.spec :as edge]
    [sixsq.nuvla.ui.i18n.spec :as i18n]
    [sixsq.nuvla.ui.infrastructures-detail.spec :as infra-service-detail]
    [sixsq.nuvla.ui.infrastructures.spec :as infra-service]
    [sixsq.nuvla.ui.main.spec :as main]
    [sixsq.nuvla.ui.messages.spec :as messages]
    [sixsq.nuvla.ui.ocre.spec :as ocre]
    [sixsq.nuvla.ui.profile.spec :as profile]
    [sixsq.nuvla.ui.session.spec :as session]))


(s/def ::db (s/merge ::acl/db
                     ::apps/db
                     ::apps-store/db
                     ::api/db
                     ::api-detail/db
                     ::credential/db
                     ::dashboard/db
                     ::deployment-dialog/db
                     ::data/db
                     ::docs/db
                     ::i18n/db
                     ::infra-service/db
                     ::infra-service-detail/db
                     ::main/db
                     ::messages/db
                     ::edge/db
                     ::edge-detail/db
                     ::profile/db
                     ::ocre/db
                     ::session/db))


(def default-db
  (merge acl/defaults
         apps/defaults
         apps-component/defaults
         apps-application/defaults
         apps-store/defaults
         api/defaults
         api-detail/defaults
         data/defaults
         dashboard/defaults
         deployment-dialog/defaults
         credential/defaults
         docs/defaults
         i18n/defaults
         infra-service/defaults
         infra-service-detail/defaults
         main/defaults
         messages/defaults
         edge/defaults
         edge-detail/defaults
         profile/defaults
         ocre/defaults
         session/defaults))
