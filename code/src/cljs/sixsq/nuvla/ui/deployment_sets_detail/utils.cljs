(ns sixsq.nuvla.ui.deployment-sets-detail.utils)

(def editable-keys [:name :description :applications-sets])

(defn unsaved-changes?
  [deployment-set deployment-set-edited]
  (and (some? deployment-set-edited)
       (not= (select-keys deployment-set-edited editable-keys)
             (select-keys deployment-set editable-keys))))
