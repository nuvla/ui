(ns sixsq.nuvla.ui.components.msg-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(defscene info
  [:<>
   [uix/MsgInfo {:content "hello info"}]
   [uix/MsgInfo {:header  "This is an info with header"
                 :content "hello info"}]])

(defscene warn
  [uix/MsgWarn {:content "hello warn"}])

(defscene error
  [uix/MsgError {:content "hello error"}])

(defscene no-items-to-show
  [:<>
   [uix/MsgNoItemsToShow]
   [uix/MsgNoItemsToShow "Custom no elements"]])
