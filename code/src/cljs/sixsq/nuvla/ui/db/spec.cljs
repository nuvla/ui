(ns sixsq.nuvla.ui.db.spec
  (:require [sixsq.nuvla.ui.pages.about.spec :as about]
            [sixsq.nuvla.ui.pages.apps.apps-application.spec :as apps-application]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.spec :as apps-applications-sets]
            [sixsq.nuvla.ui.pages.apps.apps-component.spec :as apps-component]
            [sixsq.nuvla.ui.pages.apps.apps-store.spec :as apps-store]
            [sixsq.nuvla.ui.pages.apps.spec :as apps]
            [sixsq.nuvla.ui.pages.cimi-detail.spec :as api-detail]
            [sixsq.nuvla.ui.pages.cimi.spec :as api]
            [sixsq.nuvla.ui.pages.clouds-detail.spec :as infra-service-detail]
            [sixsq.nuvla.ui.pages.clouds.spec :as infra-service]
            [sixsq.nuvla.ui.pages.credentials.spec :as credential]
            [sixsq.nuvla.ui.pages.data-set.spec :as data-set]
            [sixsq.nuvla.ui.pages.data.spec :as data]
            [sixsq.nuvla.ui.common-components.deployment-dialog.spec :as deployment-dialog]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.spec :as deployment-sets-detail]
            [sixsq.nuvla.ui.pages.deployment-sets.spec :as deployment-sets]
            [sixsq.nuvla.ui.pages.deployments.spec :as deployments]
            [sixsq.nuvla.ui.pages.docs.spec :as docs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as edges-detail]
            [sixsq.nuvla.ui.pages.edges.spec :as edges]
            [sixsq.nuvla.ui.common-components.i18n.spec :as i18n]
            [sixsq.nuvla.ui.main.intercom.spec :as intercom]
            [sixsq.nuvla.ui.common-components.job.spec :as job]
            [sixsq.nuvla.ui.common-components.notifications.spec :as notifications]
            [sixsq.nuvla.ui.main.spec :as main]
            [sixsq.nuvla.ui.common-components.messages.spec :as messages]
            [sixsq.nuvla.ui.pages.profile.spec :as profile]
            [sixsq.nuvla.ui.common-components.resource-log.spec :as resource-log]
            [sixsq.nuvla.ui.routing.router :refer [router]]
            [sixsq.nuvla.ui.session.spec :as session]))

(def default-db
  (merge about/defaults
         apps/defaults
         apps-component/defaults
         apps-application/defaults
         apps-applications-sets/defaults
         apps-application/deployments-pagination
         apps-store/defaults
         apps-store/pagination-default
         api/defaults
         api-detail/defaults
         data/defaults
         data/pagination-default
         data-set/defaults
         data-set/pagination-default
         deployments/defaults
         deployments/pagination-default
         deployment-dialog/defaults
         credential/defaults
         docs/defaults
         i18n/defaults
         infra-service/defaults
         infra-service/pagination-default
         infra-service-detail/defaults
         intercom/defaults
         main/defaults
         messages/defaults
         notifications/defaults
         edges/defaults
         edges/pagination-default
         edges-detail/defaults
         edges-detail/deployments-pagination
         profile/defaults
         session/defaults
         job/defaults
         job/pagination-default
         resource-log/defaults
         deployment-sets/defaults
         deployment-sets-detail/defaults
         {:router router}))
