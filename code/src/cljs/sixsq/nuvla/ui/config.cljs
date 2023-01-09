(ns sixsq.nuvla.ui.config
  (:require [sixsq.nuvla.ui.history.utils :as utils]))

(def debug?
  ^boolean goog.DEBUG)

(def base-path "/ui")

(def path-prefix (delay (str (utils/host-url) base-path)))
