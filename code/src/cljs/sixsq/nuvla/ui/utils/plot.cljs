(ns sixsq.nuvla.ui.utils.plot
  (:require ["react-chartjs-2" :rename {Bar BarChart Doughnut DoughnutChart Line LineChart}]
            [reagent.core :as reagent]
            ["chartjs-plugin-zoom" :refer [zoomPlugin]]))


(def Bar (reagent/adapt-react-class BarChart))

(def Doughnut (reagent/adapt-react-class DoughnutChart))

(def Line (reagent/adapt-react-class LineChart))
(.register Line zoomPlugin)



