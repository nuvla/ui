(ns sixsq.nuvla.ui.utils.plot
  (:require ["react-chartjs-2" :rename {Bar BarChart Doughnut DoughnutChart}]
            [reagent.core :as reagent]))


(def Bar (reagent/adapt-react-class BarChart))

(def Doughnut (reagent/adapt-react-class DoughnutChart))
