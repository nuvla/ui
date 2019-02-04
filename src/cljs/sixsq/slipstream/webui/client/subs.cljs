(ns sixsq.slipstream.webui.client.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.client.spec :as client-spec]))


(reg-sub
  ::client
  ::client-spec/client)

(reg-sub
  ::slipstream-url
  ::client-spec/slipstream-url)
