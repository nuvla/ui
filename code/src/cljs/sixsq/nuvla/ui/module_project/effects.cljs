(ns sixsq.nuvla.ui.module-project.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [reg-fx]]
    [sixsq.nuvla.client.api :as api]))


