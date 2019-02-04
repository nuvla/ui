(ns sixsq.slipstream.webui.plot.plot
  (:require
    ["chart.js" :as chartjs]
    ["react-chartjs-2" :as chartjs2]
    [reagent.core :as reagent]))


;; setup global defaults
(def dodger-blue "rgba(30, 144, 255, 0.5)")
(def dodger-blue-opaque "rgba(30, 144, 255, 1.00)")
(def chartjs-global (.-global (.-defaults chartjs)))

(-> chartjs-global .-defaultColor (set! dodger-blue-opaque))
(-> chartjs-global .-elements .-rectangle .-backgroundColor (set! dodger-blue))
(-> chartjs-global .-elements .-rectangle .-borderColor (set! dodger-blue-opaque))
(-> chartjs-global .-elements .-rectangle .-borderWidth (set! 2))
(-> chartjs-global .-legend .-display (set! false))


(def HorizontalBar (reagent/adapt-react-class chartjs2/HorizontalBar))

;(def Bar (reagent/adapt-react-class chartjs2/Bar))

;(def Doughnut (reagent/adapt-react-class chartjs2/Doughnut))

;(def Pie (reagent/adapt-react-class chartjs2/Pie))

;(def Line (reagent/adapt-react-class chartjs2/Line))

;(def Radar (reagent/adapt-react-class chartjs2/Radar))

;(def Polar (reagent/adapt-react-class chartjs2/Polar))

;(def Bubble (reagent/adapt-react-class chartjs2/Bubble))

;(def Scatter (reagent/adapt-react-class chartjs2/Scatter))

