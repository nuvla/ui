(ns sixsq.nuvla.ui.profile.utils
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [subscribe]]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [taoensso.timbre :as log]))
