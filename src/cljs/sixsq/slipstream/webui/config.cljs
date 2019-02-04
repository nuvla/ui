(ns sixsq.slipstream.webui.config
  (:require
    [sixsq.slipstream.webui.history.utils :as utils]))

(def debug?
  ^boolean goog.DEBUG)

(def context "/webui")

(def path-prefix (delay (str (utils/host-url) context)))
