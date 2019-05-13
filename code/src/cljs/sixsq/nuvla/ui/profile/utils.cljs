(ns sixsq.nuvla.ui.profile.utils
  (:require
    [re-frame.core :refer [subscribe]]
    [clojure.string :as str]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [taoensso.timbre :as log]
    [cljs.spec.alpha :as s]))
