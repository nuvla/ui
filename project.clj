(def +version+ "3.69-SNAPSHOT")

(defproject sixsq.nuvla/ui "3.69-SNAPSHOT"

  :description "Web Browser User Interface for Nuvla"

  :url "https://github.com/nuvla/ui"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]
            [lein-unpack-resources "0.1.1"]
            [pdok/lein-filegen "0.1.0"]
            [lein-resource "16.9.1"]]

  :parent-project {:coords  [sixsq.nuvla/parent "6.0.0"]
                   :inherit [:plugins
                             :min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :clean-targets ^{:protect false} ["resources/public/js/"
                                    "target"
                                    "resources/public/css/version.css"
                                    "resources/public/css/codemirror.css"
                                    "resources/public/css/foldgutter.css"
                                    "resources/public/css/react-datepicker.min.css"
                                    "resources/public/css/codemirror.css"]

  :auto-clean false

  :prep-tasks []

  :pom-location "target/"

  :filegen [{:data        ["#release-version:after {content: '" ~+version+ "';}\n"]
             :template-fn #(apply str %)
             :target      "target/version.css"}]

  :resource {:resource-paths
             [["node_modules/react-datepicker/dist/react-datepicker.min.css"
               {:target-path "resources/public/css/react-datepicker.min.css"}]
              ["node_modules/codemirror/lib/codemirror.css"
               {:target-path "resources/public/css/codemirror.css"}]
              ["node_modules/codemirror/addon/fold/foldgutter.css"
               {:target-path "resources/public/css/foldgutter.css"}]
              ["target/version.css"
               {:target-path "resources/public/css/version.css"}]]}

  :dependencies [[reagent]
                 [re-frame]
                 [day8.re-frame/http-fx]
                 [clj-commons/secretary]
                 [expound]
                 [com.taoensso/timbre]
                 [com.sixsq.slipstream/SlipStreamClojureAPI-cimi ~+version+]
                 [com.taoensso/tempura]
                 [funcool/promesa]
                 [com.taoensso/encore]                      ;; fix conflict, needed indirectly
                 [camel-snake-kebab]]

  :source-paths ["src/clj" "src/cljs"]

  :profiles
  {:dev   {:dependencies [[org.clojure/clojure]
                          [org.clojure/clojurescript]
                          [binaryage/devtools]]}

   :scljs {:dependencies [[thheller/shadow-cljs]            ;; WARNING: also in package.json
                          [com.google.javascript/closure-compiler-unshaded]]}}


  :aliases {"prepare"   ["do" ["filegen"] ["resource"]]
            "dev"       ["do" "prepare" ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli" "watch" "webui"]]
            "cljs-repl" ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli" "cljs-repl" "webui"]
            "install"   ["do" "prepare" ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli" "release" "webui"] ["install"]]})
