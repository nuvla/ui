(ns sixsq.nuvla.ui.client.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.client.spec :as client-spec]))


(reg-sub
  ::client
  ::client-spec/client)

(reg-sub
  ::nuvla-url
  ::client-spec/nuvla-url)
