(ns sixsq.nuvla.ui.utils.plot
  (:require ["react-chartjs-2" :rename {Bar BarChart Doughnut DoughnutChart Line LineChart Scatter ScatterChart}]
            [clojure.string :as str]
            [reagent.core :as reagent]
            ["chartjs-adapter-date-fns"]))


(def Bar (reagent/adapt-react-class BarChart))

(def Doughnut (reagent/adapt-react-class DoughnutChart))

(def Scatter (reagent/adapt-react-class ScatterChart))

(def Line (reagent/adapt-react-class LineChart))

(defn interpolate [start end percentage]
  (let [beta (- 1.0 percentage)]
    (mapv #(+ (* %1 beta) (* %2 percentage)) start end)))
(def red [255 0 0])
(def green [0 255 0])

(defn color-gradient [color1 color2 percentage]
  (interpolate color1 color2 percentage))

(defn to-rgb [color-vector]
  (str "rgb(" (str/join "," color-vector) ")"))

(def default-colors-palette ["#E6636480"
                             "#63E6B280"
                             "#63A5E680"])

(def pastel-colors-palette ["#FFAAA561"
                            "#A8E6CF61"
                            "#FF8B9461"
                            "#DCEDC161"
                            "#EACACB61"
                            "#504A0961"
                            "#D5E1DF61"
                            "#97A39F61"
                            "#E2B3A361"
                            "#A4B5C661"])


