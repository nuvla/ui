(def parent-version "6.7.4")
(def sixsq-nuvla-api-version "2.0.9")
(def version "2.18.0")

(defproject sixsq.nuvla.ui/code "2.18.0"

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
                                    "resources/public/ui/css/matchesonscrollbar.css"
                                    "resources/public/ui/css/leaflet.css"
                                    "resources/public/ui/css/images"]

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
                             {:target-path "resources/public/ui/css/version.css"}]
                            ["node_modules/leaflet/dist/leaflet.css"
                             {:target-path "resources/public/ui/css/leaflet.css"}]
                            ["node_modules/leaflet/dist/images"
                             {:target-path "resources/public/ui/css/images"}]]}

  ;; mark all dependencies as provided to avoid having transitive
  ;; dependencies pulled in by those that depend on this
  :dependencies [[reagent "0.10.0" :scope "provided"
                  :exclusions [cljsjs/react
                               cljsjs/react-dom]]
                 [re-frame "1.1.1" :scope "provided"]
                 [day8.re-frame/http-fx "0.2.1" :scope "provided"]
                 [cljs-ajax "0.8.1" :scope "provided"]
                 [clj-commons/secretary :scope "provided"]
                 [expound :scope "provided"]
                 [com.taoensso/timbre "5.1.0" :scope "provided"]
                 [sixsq.nuvla/api ~sixsq-nuvla-api-version :scope "provided"]
                 [com.taoensso/tempura "1.2.1" :scope "provided"]
                 [com.cemerick/url :scope "provided"]
                 [kwladyka/form-validator-cljs "1.2.1"]
                 [instaparse]
                 [com.degel/re-frame-storage-fx "0.1.1"]
                 [markdown-to-hiccup "0.6.2"]]

  :source-paths ["src/clj" "src/cljs"]

  :profiles
  {:dev   {:dependencies [[org.clojure/clojure "1.10.1"]
                          [org.clojure/clojurescript "1.10.773"
                           :exclusions
                           [com.google.javascript/closure-compiler-unshaded
                            org.clojure/google-closure-library
                            org.clojure/google-closure-library-third-party]]
                          [binaryage/devtools "1.0.2" :scope "test"]
                          [day8.re-frame/re-frame-10x "0.7.0"]]}

   :scljs {:dependencies [[thheller/shadow-cljs "2.11.5"]   ;; WARNING: also in package.json
                          [org.clojure/google-closure-library "0.0-20191016-6ae1f72f"]
                          [org.clojure/google-closure-library-third-party "0.0-20191016-6ae1f72f"]
                          [com.google.javascript/closure-compiler-unshaded "v20200830"]]}}



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
