(def parent-version "6.8.0")
(def sixsq-nuvla-api-version "2.0.11")

(defproject sixsq.nuvla.ui/code
  ; x-release-please-start-version
  "2.49.1-SNAPSHOT"
  ; x-release-please-end

  :description "Web Browser User Interface for Nuvla"

  :url "https://github.com/nuvla/ui"

  :license {:name         "Apache 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq.nuvla/parent ~parent-version]
                   :inherit [:plugins
                             :min-lein-version
                             :repositories
                             :deploy-repositories]}

  :clean-targets ^{:protect false} ["resources/public/ui/js/"
                                    "target"
                                    "node_modules"
                                    ".shadow-cljs"
                                    "resources/public/ui/index.html"]

  :auto-clean false

  :prep-tasks []

  :pom-location "target/"

  ;; mark all dependencies as provided to avoid having transitive
  ;; dependencies pulled in by those that depend on this
  :dependencies [[reagent "1.2.0" :scope "provided"
                  :exclusions [cljsjs/react
                               cljsjs/react-dom]]
                 [re-frame "1.4.3" :scope "provided"]
                 [day8.re-frame/http-fx "0.2.4" :scope "provided"]
                 [cljs-ajax "0.8.4" :scope "provided"]
                 [expound "0.9.0" :scope "provided"]
                 [com.taoensso/timbre "6.6.1" :scope "provided"]
                 [sixsq.nuvla/api ~sixsq-nuvla-api-version :scope "provided"]
                 [com.taoensso/tempura "1.5.4" :scope "provided"]
                 [com.cemerick/url "0.1.1" :scope "provided"]
                 [kwladyka/form-validator-cljs "1.2.1" :scope "provided"]
                 [instaparse "1.5.0" :scope "provided"]
                 [com.degel/re-frame-storage-fx "0.1.1" :scope "provided"]
                 [markdown-to-hiccup "0.6.2" :scope "provided"]
                 [clj-kondo "RELEASE" :scope "provided"]
                 [metosin/reitit "0.7.2"]]

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/cljs"]

  :profiles
  {:dev       {:dependencies [[org.clojure/clojure "1.11.3"]
                              [org.clojure/clojurescript "1.11.4"
                               :exclusions
                               [com.google.javascript/closure-compiler-unshaded
                                org.clojure/google-closure-library
                                org.clojure/google-closure-library-third-party]]
                              [binaryage/devtools "1.0.7" :scope "test"]
                              [day8.re-frame/re-frame-10x "1.9.10"]
                              [com.github.ljpengelen/shadow-cljs-hash-assets-hook "1.1.0"]]}

   :scljs     {:dependencies [
                              ;; Don't go beyond shadow-cljs v2.17.8 because it will stop working on Cloudflare
                              ;; which removed support of java from their v2 works.
                              ;; Maybe building in Github and push to cloudflare pages. Something to check.
                              [thheller/shadow-cljs "2.17.8"] ;; WARNING: also in package.json
                              [org.clojure/google-closure-library "0.0-20211011-0726fdeb"]
                              [org.clojure/google-closure-library-third-party "0.0-20211011-0726fdeb"]
                              [com.google.javascript/closure-compiler-unshaded "v20220202"]
                              [djblue/portal "0.58.5"]]}

   :portfolio {:dependencies [[no.cjohansen/portfolio "2025.01.28"]]
               :source-paths ["portfolio/src"]}}



  :aliases {"dev"             ["with-profile" "+scljs,+portfolio" "run" "-m" "shadow.cljs.devtools.cli"
                               "watch" "nuvla-ui" "portfolio"]
            "serve"           ["with-profile" "+scljs,+portfolio" "run" "-m" "shadow.cljs.devtools.cli"
                               "server"]
            "cljs-repl"       ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli"
                               "cljs-repl" "nuvla-ui"]
            "install"         ["do"
                               ["with-profile" "+scljs" "run" "-m" "shadow.cljs.devtools.cli"
                                "release" "nuvla-ui"]
                               ["install"]]
            "build-portfolio" ["with-profile" "+scljs,+portfolio" "run" "-m" "shadow.cljs.devtools.cli"
                               "release" "nuvla-ui" "portfolio"]})
