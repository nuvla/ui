(ns sixsq.nuvla.ui.utils.plot
  (:require
    ["react-chartjs-2" :as chartjs2]
    [reagent.core :as reagent]))


(def Bar (reagent/adapt-react-class chartjs2/Bar))

(def Doughnut (reagent/adapt-react-class chartjs2/Doughnut))
