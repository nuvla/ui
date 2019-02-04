(ns sixsq.nuvla.webui.client.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.webui.client.spec :as client-spec]))


(reg-sub
  ::client
  ::client-spec/client)

(reg-sub
  ::nuvla-url
  ::client-spec/nuvla-url)
