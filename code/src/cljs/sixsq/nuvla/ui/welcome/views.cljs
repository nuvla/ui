(ns sixsq.nuvla.ui.welcome.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn navigate-link [target-page text]
  [:a {:on-click #(dispatch [::history-events/navigate target-page])
       :style    {:cursor "pointer"}}
   text])

(defn step [number title & content]
  [ui/Step
   [ui/ListSA
    ^{:key title}
    [ui/ListItem
     (when-not (nil? number)
       [ui/Icon
        [ui/Label
         {:size :big, :circular true, :color "blue"}
         number]])
     [ui/ListContent
      [ui/ListHeader title]
      [ui/ListDescription content]]]]])

(defmethod panel/render :welcome
  [path]
  (let [tr           (subscribe [::i18n-subs/tr])
        query-params (subscribe [::main-subs/nav-query-params])
        {:keys [message, error]} @query-params]
    (when @query-params

      (when (or message error)
        (dispatch [:sixsq.nuvla.ui.main.events/set-message
                   (if error :error :success) (or error message)]))

      (dispatch [::history-events/navigate (str (first path) "/")]))

    [:<>
     [ui/Grid {:stackable     true
               :centered      true
               :verticalAlign :middle
               :reversed      :mobil
               :style         {:margin-top        "8px"
                               :background-image  "url(/ui/images/bg-hero-home.jpeg)"
                               :background-size   "cover"
                               :background-repeat "no-repeat"}}
      [ui/GridColumn {:width 6}
       [ui/Header {:as "h1"}
        (@tr [:welcome-header])]
       [:p
        (@tr [:welcome-subheader])]
       [:div {:style {:padding "10px 0"}}
        [:a {:class [:ui :primary :button] :href "#add-nuvlabox"} (@tr [:welcome-create-nuvlabox])]
        [:a {:class [:ui :primary :button] :href "#launch-app"} (@tr [:welcome-launch-app])]
        [:a {:class [:ui :secondary :button] :href "#video-at-the-edge"} (@tr [:welcome-process-video-at-edge])]]]
      [ui/GridColumn {:width 6}
       [ui/Embed {:id          "yYJ6laT_6M4"
                  :placeholder "/ui/images/Cover-video-edge.png"
                  :source      "youtube"}]]]

     [ui/Divider]

     [ui/Header {:as "h2" :style {:text-align :center} :size :huge}
      (@tr [:welcome-resources])]
     [ui/HeaderSubheader {:as "h3" :style {:text-align :center}}
      (@tr [:welcome-resources-subheader])]

     [ui/CardGroup {:stackable true
                    :centered  true
                    :columns   4}

      [ui/Card
       [ui/CardContent {:text-align :center}
        [ui/Header {:as :h2} (str/capitalize (@tr [:documentation]))]
        [ui/Icon {:name  "book"
                  :size  "massive"
                  :color "blue"}]
        [ui/Header {:as :h4}
         (@tr [:welcome-doc-subheader-pre]) " "
         [:a {:href "https://docs.nuvla.io"} (@tr [:here])]
         " " (@tr [:welcome-doc-subheader-post])]]]
      [ui/Card
       [ui/CardContent {:text-align :center}
        [ui/Header {:as :h2} (str/capitalize (@tr [:videos]))]
        [ui/Icon {:name  "video"
                  :size  "massive"
                  :color "blue"}]
        [ui/Header {:as :h4}
         (@tr [:welcome-video-subheader-pre]) " "
         [:a {:href "https://sixsq.com/media/videos.html"}
          (@tr [:video-channel])]
         " " (@tr [:welcome-video-subheader-post])]]]]

     [ui/Divider]

     [ui/Header {:as "h2" :style {:text-align :center} :size :huge}
      (@tr [:welcome-how-to-header])]
     [ui/HeaderSubheader {:as "h3" :style {:text-align :center :padding-bottom "20px"}}
      (@tr [:welcome-how-to-subheader])]

     [ui/Grid {:id            :add-nuvlabox
               :centered      true
               :stackable     true
               :verticalAlign :middle}
      [ui/GridColumn {:width 6}
       [ui/Image {:floated "right" :src "/ui/images/welcome-nb.png" :fluid true}]]
      [ui/GridColumn {:width 9}
       [ui/HeaderSubheader {:as "h2"}
        (@tr [:welcome-how-to-nb-header])]
       [ui/HeaderSubheader {:as "h4"}
        (@tr [:welcome-how-to-nb-subheader]) " " [:a {:href "https://docs.nuvla.io/nuvlaedge/nuvlaedge-software/"
                                                      :target "_blank"} (@tr [:here])] "."]
       [ui/StepGroup {:vertical true}
        (step 1
              (@tr [:welcome-how-to-nb-1-header])
              (@tr [:welcome-how-to-nb-1-subheader-pre])
              " "
              [:a {:target "_blank"
                   :key    "nuvlabox-engine-requirements"
                   :href   "https://docs.nuvla.io/nuvlaedge/installation/requirements/"}
               (@tr [:welcome-how-to-nb-1-subheader-mid])]
              " "
              (@tr [:welcome-how-to-nb-1-subheader-post])
              ".")
        (step 2
              (@tr [:welcome-how-to-nb-2-header])
              (@tr [:welcome-how-to-nb-2-subheader-pre])
              " "
              ^{:key (@tr [:welcome-how-to-nb-2-subheader-mid])}
              [navigate-link "edges" (@tr [:welcome-how-to-nb-2-subheader-mid])]
              " "
              (@tr [:welcome-how-to-nb-2-subheader-post])
              ".")
        (step 3
              (@tr [:welcome-how-to-nb-3-header])
              (@tr [:welcome-how-to-nb-3-subheader-pre])
              " "
              [:a {:target "_blank"
                   :key    "nuvlabox-engine-quickstart"
                   :href   "https://docs.nuvla.io/nuvlaedge/installation/"}
               (@tr [:documentation])]
              " "
              (@tr [:welcome-how-to-nb-3-subheader-post])
              ".")
        (step 4
              (@tr [:welcome-how-to-nb-4-header])
              (@tr [:welcome-how-to-nb-4-subheader-pre])
              " "
              ^{:key (@tr [:welcome-how-to-nb-4-subheader-mid])}
              [navigate-link "edges" (@tr [:welcome-how-to-nb-4-subheader-mid])]
              ". "
              (@tr [:welcome-how-to-nb-4-subheader-post])
              ".")

        (step 5
              (@tr [:welcome-how-to-nb-5-header])
              (@tr [:welcome-how-to-nb-5-subheader-pre])
              ". "
              ^{:key (@tr [:show-me])}
              [:a {:href "#deploy-app"} (str/capitalize (@tr [:show-me]))]
              ".")]]]

     [ui/Divider]

     [ui/Grid {:id            :launch-app
               :centered      true
               :stackable     true
               :verticalAlign :middle
               :reversed      "mobile"}
      [ui/GridColumn {:width 9}
       [ui/HeaderSubheader {:as "h2", :id "deploy-app"}
        (@tr [:welcome-how-to-launch-header])]
       [ui/HeaderSubheader {:as "h4"}
        (@tr [:welcome-how-to-launch-subheader]) " " [:a {:href "https://docs.nuvla.io/tutorials/deploying-apps/first-app/"} (@tr [:here])] "."]
       [ui/StepGroup {:vertical true}
        (step 1
              (@tr [:welcome-how-to-launch-1-header-])
              (@tr [:welcome-how-to-launch-1-subheader-pre])
              " "
              ^{:key (@tr [:appstore])}
              [navigate-link "apps" (@tr [:appstore])]
              " "
              (@tr [:welcome-how-to-launch-1-subheader-post])
              ".")
        (step 2
              (@tr [:welcome-how-to-launch-2-header])
              (@tr [:welcome-how-to-launch-2-subheader-pre]))
        (step 3
              (@tr [:welcome-how-to-launch-3-header])
              (@tr [:welcome-how-to-launch-3-subheader-pre])
              " ")
        (step 4
              (@tr [:welcome-how-to-launch-4-header])
              (@tr [:welcome-how-to-launch-4-subheader-pre])
              " "
              ^{:key (@tr [:dashboard])}
              [navigate-link "dashboard" (@tr [:dashboard])]
              ", "
              (@tr [:welcome-how-to-launch-4-subheader-post])
              ".")]]
      [ui/GridColumn {:width 6}
       [ui/Image {:floated "right" :src "/ui/images/welcome-appstore.png" :fluid true}]]]

     [ui/Divider]

     [ui/Grid {:id            :video-at-the-edge
               :centered      true
               :stackable     true
               :verticalAlign :middle}
      [ui/GridColumn {:width 6}
       [ui/Embed {:id          "BHzbEDzyfnQ"
                  :placeholder "https://img.youtube.com/vi/BHzbEDzyfnQ/maxresdefault.jpg"
                  :source      "youtube"}]]
      [ui/GridColumn {:width 9}
       [ui/HeaderSubheader {:as "h2"}
        (@tr [:welcome-how-to-video-header])]
       [ui/HeaderSubheader {:as "h4"}
        (@tr [:welcome-how-to-video-subheader])]
       [ui/StepGroup {:vertical true}
        (step 1
              (@tr [:welcome-how-to-video-1-header])
              (@tr [:welcome-how-to-video-1-subheader-pre])
              " "
              ^{:key (@tr [:welcome-how-to-video-1-subheader-mid])}
              [:a {:target "_blank"
                   :key    "video-nuvlabox-engine-quickstart"
                   :href   "https://docs.nuvla.io/nuvlaedge/installation/"}
               (@tr [:welcome-how-to-video-1-subheader-mid])]
              " "
              (@tr [:welcome-how-to-video-1-subheader-post])
              ".")
        (step 2
              (@tr [:welcome-how-to-video-2-header])
              (@tr [:welcome-how-to-video-2-subheader-pre])
              " "
              ^{:key (@tr [:welcome-how-to-video-2-subheader-post])}
              [navigate-link "edges" (@tr [:welcome-how-to-video-2-subheader-post])] ".")
        (step 3
              (@tr [:welcome-how-to-video-3-header])
              (@tr [:welcome-how-to-video-3-subheader-pre]))]]]]))
