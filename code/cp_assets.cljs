(ns cp-assets
  (:require ["fs-extra$default" :as fs]))

;; This
(def
  asset-files
  [["node_modules/semantic-ui-css/semantic.min.css"
    {:target-path "resources/public/ui/css/semantic.min.css"}]
   ["node_modules/semantic-ui-css/themes"
    {:target-path "resources/public/ui/css/themes"}]
   ["node_modules/react-datepicker/dist/react-datepicker.min.css"
    {:target-path "resources/public/ui/css/react-datepicker.min.css"}]
   ["node_modules/codemirror/lib/codemirror.css"
    {:target-path "resources/public/ui/css/codemirror.css"}]
   ["node_modules/codemirror/addon/fold/foldgutter.css"
    {:target-path "resources/public/ui/css/foldgutter.css"}]
   ["node_modules/codemirror/addon/dialog/dialog.css"
    {:target-path "resources/public/ui/css/dialog.css"}]
   ["node_modules/codemirror/addon/search/matchesonscrollbar.css"
    {:target-path "resources/public/ui/css/matchesonscrollbar.css"}]
   ["node_modules/leaflet/dist/leaflet.css"
    {:target-path "resources/public/ui/css/leaflet.css"}]
   ["node_modules/leaflet-draw/dist/leaflet.draw.css"
    {:target-path "resources/public/ui/css/leaflet.draw.css"}]
   ["node_modules/leaflet-draw/dist/images/spritesheet.png"
    {:target-path "resources/public/ui/css/images/spritesheet.png"}]
   ["node_modules/leaflet-draw/dist/images/spritesheet-2x.png"
    {:target-path "resources/public/ui/css/images/spritesheet-2x.png"}]
   ["node_modules/leaflet-draw/dist/images/spritesheet.svg"
    {:target-path "resources/public/ui/css/images/spritesheet.svg"}]
   ["node_modules/leaflet/dist/images"
    {:target-path "resources/public/ui/css/images"}]])

(defn- move-file [[old {new :target-path}]]
  (println "moving " old " to " new)
  (.copySync fs old new ))

(run! move-file asset-files)
