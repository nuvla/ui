(ns sixsq.nuvla.ui.utils.datepicker)

(defn datepickerWithLabel [label datepicker-component]
  [:div {:style {:display          "flex"
                 :width            "fit-content"
                 :margin-left      "1em"
                 :align-items      "center"
                 :border-radius    "0.5em"
                 :border-color     "#DADADA"
                 :border-width     1
                 :border-style     "solid"
                 :overflow         "hidden"
                 :background-color "white"}}
   label
   datepicker-component])

(defn label [label]
  [:div {:style {:height           "100%"
                 :background-color "#DADADA"
                 :padding          "0.5em"
                 :border           "1px solid #DADADA"}}
   label])









