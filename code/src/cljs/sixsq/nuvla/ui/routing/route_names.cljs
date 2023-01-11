(ns sixsq.nuvla.ui.routing.route-names
  #_(:require [reitit.core :as r]
              [sixsq.nuvla.ui.routing.routes :refer [router]]))

;;;;; Ideally this file is auto generated from the current router.
;;;;; Possible solutions:
;;;;; 1. From a clojure file: Can't import router form .cljs file, would have to turn that into .cljc
;;;;; 2. From a clojure script file: This would have to run in node to write to files
;;;;; 3. From an nbb file

(comment
   ;; require reitit and the router and evaled this function for initial routes
  #_(->> (r/route-names router)
      (map #(list 'def (symbol (name %)) %))))


(def root :sixsq.nuvla.ui.routing.routes/root)
(def home-root :sixsq.nuvla.ui.routing.routes/home-root)
(def nuvlabox :sixsq.nuvla.ui.routing.routes/nuvlabox)
(def nuvlabox-slashed :sixsq.nuvla.ui.routing.routes/nuvlabox-slashed)
(def nuvlabox-details :sixsq.nuvla.ui.routing.routes/nuvlabox-details)
(def nuvlabox-cluster-details :sixsq.nuvla.ui.routing.routes/nuvlabox-cluster-details)
(def edges :sixsq.nuvla.ui.routing.routes/edges)
(def edges-slashed :sixsq.nuvla.ui.routing.routes/edges-slashed)
(def edges-details :sixsq.nuvla.ui.routing.routes/edges-details)
(def edges-cluster-details :sixsq.nuvla.ui.routing.routes/edges-cluster-details)
(def edge :sixsq.nuvla.ui.routing.routes/edge)
(def edge-slashed :sixsq.nuvla.ui.routing.routes/edge-slashed)
(def edge-details :sixsq.nuvla.ui.routing.routes/edge-details)
(def edge-cluster-details :sixsq.nuvla.ui.routing.routes/edge-cluster-details)
(def infrastructures :sixsq.nuvla.ui.routing.routes/infrastructures)
(def infrastructures-slashed :sixsq.nuvla.ui.routing.routes/infrastructures-slashed)
(def infrastructures-details :sixsq.nuvla.ui.routing.routes/infrastructures-details)
(def clouds :sixsq.nuvla.ui.routing.routes/clouds)
(def clouds-slashed :sixsq.nuvla.ui.routing.routes/clouds-slashed)
(def clouds-details :sixsq.nuvla.ui.routing.routes/clouds-details)
(def deployment :sixsq.nuvla.ui.routing.routes/deployment)
(def deployment-slashed :sixsq.nuvla.ui.routing.routes/deployment-slashed)
(def deployment-details :sixsq.nuvla.ui.routing.routes/deployment-details)
(def deployments :sixsq.nuvla.ui.routing.routes/deployments)
(def deployments-slashed :sixsq.nuvla.ui.routing.routes/deployments-slashed)
(def deployments-details :sixsq.nuvla.ui.routing.routes/deployments-details)
(def sign-up :sixsq.nuvla.ui.routing.routes/sign-up)
(def sign-in :sixsq.nuvla.ui.routing.routes/sign-in)
(def reset-password :sixsq.nuvla.ui.routing.routes/reset-password)
(def set-password :sixsq.nuvla.ui.routing.routes/set-password)
(def sign-in-token :sixsq.nuvla.ui.routing.routes/sign-in-token)
(def about :sixsq.nuvla.ui.routing.routes/about)
(def home :sixsq.nuvla.ui.routing.routes/home)
(def home-slash :sixsq.nuvla.ui.routing.routes/home-slash)
(def dashboard :sixsq.nuvla.ui.routing.routes/dashboard)
(def apps :sixsq.nuvla.ui.routing.routes/apps)
(def apps-slashed :sixsq.nuvla.ui.routing.routes/apps-slashed)
(def apps-details :sixsq.nuvla.ui.routing.routes/apps-details)
(def credentials :sixsq.nuvla.ui.routing.routes/credentials)
(def credentials-slash :sixsq.nuvla.ui.routing.routes/credentials-slash)
(def notifications :sixsq.nuvla.ui.routing.routes/notifications)
(def data :sixsq.nuvla.ui.routing.routes/data)
(def data-details :sixsq.nuvla.ui.routing.routes/data-details)
(def deployment-sets :sixsq.nuvla.ui.routing.routes/deployment-sets)
(def deployment-sets-slashed :sixsq.nuvla.ui.routing.routes/deployment-sets-slashed)
(def deployment-sets-details :sixsq.nuvla.ui.routing.routes/deployment-sets-details)
(def documentation :sixsq.nuvla.ui.routing.routes/documentation)
(def documentation-sub-page :sixsq.nuvla.ui.routing.routes/documentation-sub-page)
(def api :sixsq.nuvla.ui.routing.routes/api)
(def api-slashed :sixsq.nuvla.ui.routing.routes/api-slashed)
(def api-sub-page :sixsq.nuvla.ui.routing.routes/api-sub-page)
(def profile :sixsq.nuvla.ui.routing.routes/profile)
(def catch-all :sixsq.nuvla.ui.routing.routes/catch-all)
