(def parent-version "6.6.0")
(def sixsq-nuvla-api-version "2.0.3")
(def version "2.4.5")

(defproject sixsq.nuvla.ui/code "2.4.5"

  :description "Web Browser User Interface for Nuvla"

  :url "https://github.com/nuvla/ui"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]
            [lein-unpack-resources "0.1.1"]
            [pdok/lein-filegen "0.1.0"]
            [lein-resource "16.9.1"]]

  :parent-project {:coords  [sixsq.nuvla/parent ~parent-version]
                   :inherit [:plugins
                             :min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :clean-targets ^{:protect false} ["resources/public/ui/js/"
                                    "target"
                                    "node_modules"
                                    ".shadow-cljs"
                                    "resources/public/ui/css/semantic.min.css"
                                    "resources/public/ui/css/themes"
                                    "resources/public/ui/css/version.css"
                                    "resources/public/ui/css/codemirror.css"
                                    "resources/public/ui/css/foldgutter.css"
                                    "resources/public/ui/css/react-datepicker.min.css"
                                    "resources/public/ui/css/codemirror.css"
                                    "resources/public/ui/css/dialog.css"
                                    "resources/public/ui/css/matchesonscrollbar.css"]

  :auto-clean false

  :prep-tasks []

  :pom-location "target/"

  :filegen [{:data        ["#release-version:after {content: '" ~version "';}\n"]
             :template-fn #(apply str %)
             :target      "target/version.css"}]

  :resource {:skip-stencil [#".*"]
             :resource-paths
                           [["node_modules/semantic-ui-css/semantic.min.css"
                             {:target-path "resources/public/ui/css/semantic.min.css"}]
                            ["node_modules/semantic-ui-css/themes"
                             {:target-path "resources/public/ui/css/themes"}]
                            ["node_modules/react-datepicker/dist/react-datepicker.min.css"
                             {:target-path "resources/public/ui/css/react-datepicker.min.css"}]
                            ["node_modules/codemirror/lib/codemirror.css"
                             {:target-path "resources/public/ui/css/codemirror.css"}]
                            ["node_modules/codemirror/addon/fold/foldgutter.css"
                             {:target-path "resources/public/ui/css/foldgutter.css"}]
                            ["node_modules/codemirror/addon/dialog/dialog.css"
                             {:target-path "resources/public/ui/css/dialog.css"}]
                            ["node_modules/codemirror/addon/search/matchesonscrollbar.css"
                             {:target-path "resources/public/ui/css/matchesonscrollbar.css"}]
                            ["target/version.css"
                             {:target-path "resources/public/ui/css/version.css"}]]}

  ;; mark all dependencies as provided to avoid having transitive
  ;; dependencies pulled in by those that depend on this
  :dependencies [[reagent "0.9.0-20190110.190613-2" :scope "provided"
                  :exclusions [cljsjs/react
                               cljsjs/react-dom]]
                 [re-frame "0.10.9" :scope "provided"]
                 [clj-commons/secretary :scope "provided"]
                 [expound :scope "provided"]
                 [com.taoensso/timbre "4.10.0"  :scope "provided"]
                 [sixsq.nuvla/api ~sixsq-nuvla-api-version :scope "provided"]
                 [com.taoensso/tempura "1.2.1" :scope "provided"]
                 [com.cemerick/url :scope "provided"]]

  :source-paths ["src/clj" "src/cljs"]

  :profiles
  {:dev   {:dependencies [[org.clojure/clojure]
                          [org.clojure/clojurescript]
                          [binaryage/devtools "0.9.10" :scope "test"]]}

   :scljs {:dependencies [[thheller/shadow-cljs "2.8.52"]   ;; WARNING: also in package.json
                          [com.google.javascript/closure-compiler-unshaded "v20190909"]]}}



  :aliases {"prepare"   ["do"
                         ["filegen"]
                         ["resource"]]
            "dev"       ["do"
                         "prepare"
                         ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli"
                          "watch" "nuvla-ui"]]
            "cljs-repl" ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli"
                         "cljs-repl" "nuvla-ui"]
            "install"   ["do" "prepare"
                         ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli"
                          "release" "nuvla-ui"]
                         ["install"]]})
