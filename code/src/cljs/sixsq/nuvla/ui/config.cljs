(ns sixsq.nuvla.ui.config
  (:require [sixsq.nuvla.ui.routing.effects :refer [host-url]]))

(def debug?
  ^boolean goog.DEBUG)

(def base-path "/ui")

(def path-prefix (delay (str (host-url) base-path)))
