(ns sixsq.nuvla.ui.utils.plot
  (:require ["react-chartjs-2" :rename {Bar BarChart Doughnut DoughnutChart Line LineChart Scatter ScatterChart}]
            [reagent.core :as reagent]
            ["chartjs-plugin-zoom" :refer [zoomPlugin]]
            ["chartjs-adapter-date-fns"]))


(def Bar (reagent/adapt-react-class BarChart))

(def Doughnut (reagent/adapt-react-class DoughnutChart))

(def Scatter (reagent/adapt-react-class ScatterChart))

(def Line (reagent/adapt-react-class LineChart))
(.register LineChart zoomPlugin)



