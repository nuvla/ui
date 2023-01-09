(ns sixsq.nuvla.ui.utils.intercom
  (:require ["react-intercom" :as react-intercom]
            [reagent.core :as reagent]))


(def intercom-api react-intercom/IntercomAPI)


(def Intercom (reagent/adapt-react-class react-intercom/default))
