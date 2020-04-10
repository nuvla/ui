(ns sixsq.nuvla.ui.welcome.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.session.subs :as session-subs]
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
        iframe?      (subscribe [::main-subs/iframe?])
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
               :reversed      :mobile
               :style {:margin-top "8px"}}
      [ui/GridColumn {:width 6}
       [ui/Header {:as "h1"}
        "We enable your edge, as a Service"]
       [ui/HeaderSubheader {:as "h2"}
        "Deploy any containerised app, to the edge, Kubernetes, Docker and Docker Swarm"]
       [ui/HeaderSubheader {:as "h3"}
        "New to Nuvla.io? Start here:"]
       [:div {:style {:padding "10px 0"}}
        [:a {:class [:ui :primary :button] :href "#add-nuvlabox"} "Create a NuvlaBox"]]
       [:div {:style {:padding "10px 0"}}
        [:a {:class [:ui :primary :button] :href "#launch-app"} "Launch an app"]]
       ;[:div {:style {:padding "10px 0"}}
       ; [:a {:class [:ui :primary :button] :href "#deploy-app"} "Register an app"] [ui/Label {:tag true :color :yellow} "coming soon"]]
       ;[:div {:style {:padding "10px 0"}}
       ; [:a {:class [:ui :primary :button] :href "#add-nuvlabox"} "Register an container infrastructure"] [ui/Label {:tag true :color :yellow} "coming soon"]]
       [ui/HeaderSubheader {:as "h3"}
        "And more advanced features, we think simply rock:"]
       [:div {:style {:padding "10px 0"}}
        [:a {:class [:ui :button] :href "#video-at-the-edge"} "Process video at the edge"]]
       ;[:div {:style {:padding "10px 0"}}
       ; [:a {:class [:ui :button] :href "#data-at-the-edge"} "Manage data at the edge"] [ui/Label {:tag true :color :yellow} "coming soon"]]
       ;[:div {:style {:padding "10px 0"}}
       ; [:a {:class [:ui :button] :href "#edge-to-cloud-data"} "Manage data across edges and cloud"] [ui/Label {:tag true :color :yellow} "coming soon"]]
       ]
      [ui/GridColumn {:width 6}
       [ui/Embed {:id           "yYJ6laT_6M4"
                  :placeholder  "https://img.youtube.com/vi/yYJ6laT_6M4/maxresdefault.jpg"
                  :source       "youtube"}]]]

     [ui/Divider]

     [ui/Header {:as "h2" :style {:text-align :center} :size :huge}
      "Resources"]
     [ui/HeaderSubheader {:as "h3" :style {:text-align :center}}
      "Here find a wealth of Nuvla resources."]

     [ui/CardGroup {:stackable true
               :centered true
               :columns 4
               :padded true}

      [ui/Card
       [ui/CardContent {:text-align :center}
        [ui/Header {:as :h2} "Documentation"]
        [ui/Icon {:name  "book"
                  :size  "massive"
                  :color "blue"}]
        [ui/Header {:as :h4}
         "Find "
         [:a {:href "https://docs.nuvla.io"} "here"]
         " the complete Nuvla documentation"]]]
      [ui/Card {:textAlign :center}
       [ui/CardContent {:text-align :center}
        [ui/Header {:as :h2} "Videos"]
        [ui/Icon {:name  "video"
                  :size  "massive"
                  :color "blue"}]
        [ui/Header {:as :h4}
         "Access our "
         [:a {:href "https://sixsq.com/videos"}
          "video channel"]
         " to see talks, presentations and tutorials"]]]]

     ;[ui/Divider]
     ;
     ;[ui/Header {:as "h2" :style {:text-align :center} :size :huge}
     ; "Workflows"]
     ;[ui/HeaderSubheader {:as "h3" :style {:text-align :center}}
     ; "These will help you understanding how Nuvla hangs together"]
     ;
     ;
     ;[ui/HeaderSubheader {:as "h2"}
     ; "Manage your own infrastructures"]
     ;
     ;[ui/StepGroup {:widths 2}
     ; (step nil "Create an infrastructure" "Here you have two choices:"
     ;       [ui/Step {:vertical true :attached true}
     ;        [ui/StepGroup {:vertical true :style {:margin-left "-10px" :margin-right "-10px"}}
     ;         (step 1 "Create a NuvlaBox" [:a {:href "#add-nuvlabox"} "Show me"])
     ;         (step "1'" "Register orchestration engine" "" [:a {:href "#add-k8s"} "Show me"])]])
     ; (step 2 "Launch a test application" [:a {:href "#launch"} "Show me"])]
     ;
     ;[ui/Divider]
     ;
     ;[ui/HeaderSubheader {:as "h2"}
     ; "Launch applications"]
     ;
     ;[ui/StepGroup {:widths 3}
     ; (step 1 "Select an app" "Use the App Store to search and find the app you want to launch.")
     ; (step 2 "Launch the application" "By choosing the target infrastructure. " [:a {:href "#launch"} "Show me"])
     ; (step 3 "Manage your apps" "You can now manage, monitor and update your apps. " [:a {:href "/ui/dashboard"} "Show me"])]
     ;
     ;[ui/Divider]
     ;
     ;[ui/HeaderSubheader {:as "h2"}
     ; "Register applications"]
     ;
     ;[ui/StepGroup {:widths 3}
     ; (step nil "Register a containerised application" "Here you have three choices:"
     ;       [ui/Step {:vertical true :attached true}
     ;        [ui/StepGroup {:vertical true}
     ;         (step 1 "Single Docker container" "Click or tap " [:a {:href "/ui/edge"} "here"] " to learn how to create and register a NuvlaBox.")
     ;         (step "1'" "Multiple containers" [:a {:href "https://docs.nuvla.io"} "Show me"] " how to register a Docker Compose for Docker or Docker Swarm.")
     ;         (step "1''" "Kubernetes manifest" [:a {:href "https://docs.nuvla.io"} "Show me"] " how to register a Kubernetes manifest.")]])
     ; (step 2 "Test your new application" [:a {:href "#launch"} "Show me"])
     ; (step 3 "Manage your apps" "Using the " [:a {:href "/ui/dashboard"} "dashboard"] ", you can now view in one place, all your applications, and manage them.")]
     ;
     ;[ui/Divider]
     ;
     ;[ui/HeaderSubheader {:as "h2"}
     ; "Manage data at the edge"]
     ;
     ;[ui/StepGroup {:widths 3}
     ; (step nil "Register a containerised application" "Here you have three choices:"
     ;       [ui/Step {:vertical true :attached true}
     ;        [ui/StepGroup {:vertical true}
     ;         (step 1 "Single Docker container" "Click or tap " [:a {:href "/ui/edge"} "here"] " to learn how to create and register a NuvlaBox.")
     ;         (step "1'" "Multiple containers (Compose file)" [:a {:href "https://docs.nuvla.io"} "Show me"] " how to register a Docker Compose for Docker or Docker Swarm.")
     ;         (step "1''" "Kubernetes manifest" [:a {:href "https://docs.nuvla.io"} "Show me"] " how to register a Kubernetes manifest.")]])
     ; (step 2 "Launch an application" [:a {:href "#launch"} "Show me"])
     ; (step 3 "Manage your apps" "Using the " [:a {:href "/ui/dashboard"} "dashboard"] ", you can now view in one place, all your applications, and manage them.")]
     ;
     ;[ui/Divider]
     ;
     ;[ui/HeaderSubheader {:as "h2"}
     ; "Manage data across edge and cloud"]
     ;
     ;[ui/StepGroup {:widths 3}
     ; (step nil "Register a containerised application" "Here you have three choices:"
     ;       [ui/Step {:vertical true :attached true}
     ;        [ui/StepGroup {:vertical true}
     ;         (step 1 "Single Docker container" "Click or tap " [:a {:href "/ui/edge"} "here"] " to learn how to create and register a NuvlaBox.")
     ;         (step "1'" "Multiple containers (Compose file)" [:a {:href "https://docs.nuvla.io"} "Show me"] " how to register a Docker Compose for Docker or Docker Swarm.")
     ;         (step "1''" "Kubernetes manifest" [:a {:href "https://docs.nuvla.io"} "Show me"] " how to register a Kubernetes manifest.")]])
     ; (step 2 "Launch an application" [:a {:href "#launch"} "Show me"])
     ; (step 3 "Manage your apps" "Using the " [:a {:href "/ui/dashboard"} "dashboard"] ", you can now view in one place, all your applications, and manage them.")]

     [ui/Divider]

     [ui/Header {:as "h2" :style {:text-align :center} :size :huge}
      "How To's"]
     [ui/HeaderSubheader {:as "h3" :style {:text-align :center :padding-bottom "20px"}}
      "These instructions will guide you through specific tasks."]

     [ui/Grid {:id            :add-nuvlabox
               :centered      true
               :stackable     true
               :verticalAlign :middle}
      [ui/GridColumn {:width 6}
       [ui/Image {:floated "right" :src "/ui/images/welcome-nb.png" :fluid true}]]
      [ui/GridColumn {:width 9}
       [ui/HeaderSubheader {:as "h2"}
        "Create and configure your first NuvlaBox (smart edge device)"]
       [ui/HeaderSubheader {:as "h4"}
        "Follow these steps or find detailed instructions " [:a {:href "https://docs.nuvla.io/nuvlabox/nuvlabox-engine"} "here"] "."]
       [ui/StepGroup {:vertical true}
        (step 1
              "Prepare your NuvlaBox"
              "Here is "
              [:a {:target "_blank" :href "https://docs.nuvla.io/nuvlabox/nuvlabox-engine/requirements"} "what you have to do"]
              " to prepare your hardware platform.")
        (step 2
              "Create a new NuvlaBox entry in Nuvla" "Go to the " [navigate-link "edge" "edge page"] " and click or tap on the " [:strong "add button"] ".")
        (step 3 "Start NuvlaBox Engine" "Copy the compose file to your new NuvlaBox and start the NuvlaBox Engine. Here's a link to the " [:a {:target "_blank" :href "https://docs.nuvla.io/nuvlabox/nuvlabox-engine/quickstart"} "documentation"] " for details.")
        (step 4 "Manage your NuvlaBox" "Once the NuvlaBox Engine has started, you'll see it turn active on the " [navigate-link "edge" "edge page"] ". From there, you'll be able to see telemetry and control your edge device.")
        (step 5 "Deploy apps to your NuvlaBox" "You can now deploy any containerised app compatible with your edge hardware architecture. " [:a {:href "#deploy-app"} "Show me"])]]]

     [ui/Divider]

     [ui/Grid {:id            :launch-app
               :centered      true
               :stackable     true
               :verticalAlign :middle}
      [ui/GridColumn {:width 9}
       [ui/HeaderSubheader {:as "h2"}
        "Launch any containerised app"]
       [ui/HeaderSubheader {:as "h4"}
        "You will find detailed instructions " [:a {:href "https://docs.nuvla.io/nuvla/launch-app"} "here"] "."]
       [ui/StepGroup {:vertical true}
        (step 1 "Find you app in the App Store" "The " [navigate-link "apps" "App Store"] " contains public and private containerised apps. Use the search field to find what you are looking for. Try for example 'nginx examples'. Then click/tap 'launch'.")
        (step 2 "Configure the app" "Here you will tell Nuvla which target infrastructure to deploy to, as well as configuration parameters.")
        (step 3 "Launch!" "Once all this is done, you simply have to press the launch button")
        (step 4 "Enjoy :-)" "Once the app is deployed, from the " [navigate-link "dashboard" "Dashboard"] ", or the detailed deployment page, you'll be able to access your app.")]]
      [ui/GridColumn {:width 6}
       [ui/Image {:floated "right" :src "/ui/images/welcome-appstore.png" :fluid true}]]]

     [ui/Divider]

     [ui/Grid {:id :video-at-the-edge
               :centered true
               :stackable true
               :verticalAlign :middle
               :reversed "mobile"}
      [ui/GridColumn {:width 6}
       [ui/Embed {:id          "BHzbEDzyfnQ"
                  :placeholder "https://img.youtube.com/vi/BHzbEDzyfnQ/maxresdefault.jpg"
                  :source      "youtube"}]]
      [ui/GridColumn {:width 9}
       [ui/HeaderSubheader {:as "h2"}
        "Process video stream at the edge"]
       [ui/HeaderSubheader {:as "h4"}
        "Follow these steps and/or watch the video."]
       [ui/StepGroup {:vertical true}
        (step 1 "Install a NuvlaBox" "Follow these " [:a {:href "#add-nuvlabox"} "steps"] " to setup your own NuvlaBox.")
        (step 2 "Connect a video camera" "This tutorial assumes you have connected a USB camera to your device. Make sure it appears in the peripheral of you NuvlaBox on the " [navigate-link "edge" "edge page"] ".")
        (step 3 "Deploy a video app to process the stream" "The video in this section will walk you through these steps")]]]]))
