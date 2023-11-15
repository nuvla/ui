(ns sixsq.nuvla.ui.apps-store.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.nav-tab :as tab-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]))

(def virtual-apps-set-parent-path "apps-sets")

(s/def ::modules any?)
(s/def ::tab any?)
(s/def ::pagination-appstore any?)
(s/def ::pagination-allapps any?)
(s/def ::pagination-myapps any?)
(s/def ::modules-search any?)

(def defaults
  {::modules        nil
   ::tab            (tab-plugin/build-spec :default-tab :appstore)
   ::modules-search (full-text-search-plugin/build-spec)})

(def appstore-key :appstore)
(def allapps-key :allapps)
(def myapps-key :myapps)
(def navigate-key :navigate)

(def page-keys->pagination-db-path
  (zipmap [appstore-key allapps-key myapps-key]
          [::pagination-appstore ::pagination-allapps ::pagination-myapps]))

(def pagination-default {::pagination-appstore (pagination-plugin/build-spec
                                                 :default-items-per-page 8)
                         ::pagination-allapps (pagination-plugin/build-spec
                                                :default-items-per-page 8)
                         ::pagination-myapps (pagination-plugin/build-spec
                                               :default-items-per-page 8)})