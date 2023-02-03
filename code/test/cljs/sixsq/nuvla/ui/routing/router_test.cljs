(ns sixsq.nuvla.ui.routing.router-test
  (:require [cljs.test :refer [are deftest is testing]]
            [sixsq.nuvla.ui.routing.router :refer [init-routes!]]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href pathify
                                                  to-pathname]]))

(init-routes!)


(deftest name->href-test
  (testing "Creating paths from route names"
    (testing "for static routes"
      (are [path route-args]
        (= path
           (apply name->href route-args))
        "/ui/edges" [routes/edges]
        "/ui/edges" [routes/edges {:uuid "1234"}]
        "/ui/edges?hello=world&this=is-nice" [routes/edges nil {:hello "world", :this :is-nice}]))
    (testing "for dynamic routes"
      (testing "with single dynamic path segments"
        (are [path route-args]
          (= path
             (apply name->href route-args))
          "/ui/edges/1234" [routes/edges-details {:uuid "1234"}]
          "/ui/edges/1234" [routes/edges-details {:uuid "1234" :hello "world"}]
          "/ui/edges/1234?hello=world&this=is-nice" [routes/edges-details {:uuid "1234"}
                                                     {:hello "world", :this :is-nice}]
          nil [routes/edges-details]
          nil [routes/edges-details {:no-match "here"}]))
      (testing "that are catch-all routes where multi segment
                paths as :sub-path values get URI encoded by reitit,
                which is a problem for '/'. That's why name->href uses
                js/decodeURIComponent to make it work."
        (is (not= "/ui/apps/this-works%2Fperhaps%2Funexpected?query-param=hello%2Fworld"
               (name->href routes/apps-details
                           {:sub-path "this-works/perhaps/unexpected"}
                           {:query-param "hello/world"})))
        (is (=
              "/ui/apps/this-works/perhaps/unexpected?query-param=hello/world"
              (name->href routes/apps-details
                          {:sub-path "this-works/perhaps/unexpected"}
                          {:query-param "hello/world"}))))))
  (testing "Creating paths using `to-pathname`"
    (is "/ui/apps/this/works/too" (to-pathname ["apps" "this" "works" "too"])))
  (testing "Creating paths using `to` and route name"
    (is "/ui/apps/this-works/as/expected" (pathify [(name->href routes/apps) "this-works" "as" "expected"])
        ;; => "/ui/apps/this-works/as/expected"
        )))


#_(comment
    ;;;; MATCHING ;;;;

    ;; Two kinds of routes with dynamic path segments (as opposed to static paths, e.g. "ui/welcome" or "/ui/edges"):
    ;;  1. single segments: "/ui/edges/:uuid"
    ;;     -> only matches "/ui/edges/1234"
    (r/match-by-path router "/ui/edges/1234")
    ;;     => #reitit.core.Match{:template "/ui/edges/:uuid", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/edges-details, :view #object[sixsq$nuvla$ui$edges$views$edges_view], :link-text "edges-details"}, :result nil, :path-params {:uuid "1234"}, :path "/ui/edges/1234"}
    ;;         -> but not "/ui/edges/1234/5678"
    (r/match-by-path router "/ui/edges/1234/5678")
    ;;     => #reitit.core.Match{:template "/*sub-path", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/catch-all, :view #object[sixsq$nuvla$ui$unknown_resource$UnknownResource]}, :result nil, :path-params {:sub-path "ui/edges/1234/5678"}, :path "/ui/edges/1234/5678"}
    ;;        This matches the top level catch-all route "/*sub-path", which returns UnknownResource view,
    ;;        and not "/ui/edges/:uuid"

    ;;  2. catch-alls "/apps/*sub-path"
    ;;     -> matches all sub paths of apps:
    (r/match-by-path router "/ui/apps/1234")
    ;;     => #reitit.core.Match{:template "/ui/apps/*sub-path", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/apps-details, :view #object[sixsq$nuvla$ui$apps$views$AppDetails], :link-text "Apps"}, :result nil, :path-params {:sub-path "1234"}, :path "/ui/apps/1234"}
    (r/match-by-path router "/ui/apps/1234/5678")
    ;;     => #reitit.core.Match{:template "/ui/apps/*sub-path", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/apps-details, :view #object[sixsq$nuvla$ui$apps$views$AppDetails], :link-text "Apps"}, :result nil, :path-params {:sub-path "1234/5678"}, :path "/ui/apps/1234/5678"}


    ;; MORE ABOUT INTERNALS AND ONE WARNING

    ;; internally `rfe/push-state` matches route by calling match-by-name,
    ;; then calls:
    ;; 1) js/window.pushState with found path and
    ;; 2) provided -on-navigate handler

    ;; this navigates to "/ui/apps"
    (rfe/push-state :apps {} nil)

    ;; this navigates to "/ui/apps/sixsq"
    (rfe/push-state :apps-details {:sub-path "sixsq"} nil)


    ;; ...but be warned: this call navigates to "/ui/apps/sixsq%2Fblackbox":
    (rfe/push-state :apps-details {:sub-path "sixsq/blackbox"} nil)
    ;; ...and this to "/ui/apps/sixsq%2FNew%20Project?subtype=project":
    (rfe/push-state :apps-details {:sub-path "sixsq/New Project"} {:subtype "project"})


    ;;;; This happens because reitit uses `js/encodeURIComponent` to turn path param values into a `path`.


    ;; This has two implications:

    ;; 1. to navigate by path, we have a
    ;;    `::push-state` event dispatching `:push-state` effect which manually calls
    ;;    (.pushState js/window.history nil {} path)
    ;;    followed by
    ;;    (rfh/-on-navigate @history path)
    ;;    -> because pushState does not call popState, we have to call -on-navigate manually.
    ;;    That's how it is done in reitit source and also what they recommended via Slack.

    ;; 2. `href` works the same, so be mindful when you use it
    ;;    This means we cannot call this with a :sub-path value comprised of multiple path segments
    ;;    e.g. to navigate to "/ui/apps/this-works/perhaps/unexpected"
    (name->href :apps-details {:sub-path "this-works/perhaps/unexpected"} {:query-param "hello/world"})
    ;; => "/ui/apps/this-works%2Fperhaps%2Funexpected?query-param=hello%2Fworld"
    (name->href :apps-details {:sub-path "sixsq/blackbox"})

    ;; so construct the path using the parent route...
    (str (name->href routes/apps) "/" "this-works/as/expected")
    ;; => "/ui/apps/this-works/as/expected"
    (str (name->href routes/apps) "/" "sixsq/blackbox")

    ;; ...or with pathify helper
    (pathify [(name->href routes/apps) "this-works" "as" "expected"])
    ;; => "/ui/apps/this-works/as/expected"

    ;; or with to-pathname helper, which adds base-path
    (to-pathname ["apps" "this" "works" "too"])
    ;; => "/ui/apps/this/works/too"
    ;; this last one shouldn't be used in new code anymore, because
    ;; we path strings directly, but it's useful in legacy code.
    )