(ns sixsq.nuvla.ui.db.spec
  (:require
    [sixsq.nuvla.ui.apps-application.spec :as apps-application]
    [sixsq.nuvla.ui.apps-component.spec :as apps-component]
    [sixsq.nuvla.ui.apps-store.spec :as apps-store]
    [sixsq.nuvla.ui.apps.spec :as apps]
    [sixsq.nuvla.ui.cimi-detail.spec :as api-detail]
    [sixsq.nuvla.ui.cimi.spec :as api]
    [sixsq.nuvla.ui.clouds-detail.spec :as infra-service-detail]
    [sixsq.nuvla.ui.clouds.spec :as infra-service]
    [sixsq.nuvla.ui.credentials.spec :as credential]
    [sixsq.nuvla.ui.data-set.spec :as data-set]
    [sixsq.nuvla.ui.data.spec :as data]
    [sixsq.nuvla.ui.deployment-dialog.spec :as deployment-dialog]
    [sixsq.nuvla.ui.deployment-fleets-detail.spec :as deployment-fleets-detail]
    [sixsq.nuvla.ui.deployment-fleets.spec :as deployment-fleets]
    [sixsq.nuvla.ui.deployments.spec :as deployments]
    [sixsq.nuvla.ui.docs.spec :as docs]
    [sixsq.nuvla.ui.edges-detail.spec :as edges-detail]
    [sixsq.nuvla.ui.edges.spec :as edges]
    [sixsq.nuvla.ui.i18n.spec :as i18n]
    [sixsq.nuvla.ui.intercom.spec :as intercom]
    [sixsq.nuvla.ui.job.spec :as job]
    [sixsq.nuvla.ui.main.spec :as main]
    [sixsq.nuvla.ui.messages.spec :as messages]
    [sixsq.nuvla.ui.profile.spec :as profile]
    [sixsq.nuvla.ui.resource-log.spec :as resource-log]
    [sixsq.nuvla.ui.session.spec :as session]))

(def default-db
  (merge apps/defaults
         apps-component/defaults
         apps-application/defaults
         apps-store/defaults
         api/defaults
         api-detail/defaults
         data/defaults
         data-set/defaults
         deployments/defaults
         deployment-dialog/defaults
         credential/defaults
         docs/defaults
         i18n/defaults
         infra-service/defaults
         infra-service-detail/defaults
         intercom/defaults
         main/defaults
         messages/defaults
         edges/defaults
         edges-detail/defaults
         profile/defaults
         session/defaults
         job/defaults
         resource-log/defaults
         deployment-fleets/defaults
         deployment-fleets-detail/defaults
         ))
