(ns sixsq.nuvla.ui.pages.groups.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.pages.groups.spec :as spec]))

(reg-sub
  ::pending-invitations
  :-> ::spec/pending-invitations)
