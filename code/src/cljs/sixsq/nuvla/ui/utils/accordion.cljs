(ns sixsq.nuvla.ui.utils.accordion)

(defn toggle [v]
  (swap! v not))
