(ns sixsq.nuvla.ui.profile.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.cimi-api.utils :as cimi-api-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.table :as table]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as values]))


(defn tuple-to-row [[v1 v2]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (str v1)]
   [ui/TableCell v2]])


(def data-to-tuple
  (juxt (comp name first) (comp values/format-value second)))


(defn format-roles
  [{:keys [roles] :as m}]
  (assoc m :roles (values/format-collection (sort (str/split roles #"\s+")))))


(defn username-as-link
  [{:keys [username] :as m}]
  (assoc m :username (values/as-href {:href (str "user/" username)})))


(def session-keys #{:username :roles :clientIP})


(def session-keys-order {:username 1, :clientIP 2, :expiry 3, :roles 4})


(defn add-index
  [[k _ :as entry]]
  (-> k
      (session-keys-order 5)
      (cons entry)))


(defn format-ssh-keys
  [{:keys [sshPublicKey] :as data}]
  (let [keys (str/split sshPublicKey #"[\n\r]+")]
    (assoc data :sshPublicKey (values/format-collection keys))))


(defn process-session-data
  [{:keys [expiry] :as data}]
  (let [locale (subscribe [::i18n-subs/locale])]
    (->> (select-keys data session-keys)
         (cons [:expiry (time/remaining expiry @locale)])
         (map add-index)
         (sort-by first)
         (map rest))))


(defn session-info
  []
  (let [tr (subscribe [::i18n-subs/tr])
        session (subscribe [::authn-subs/session])]
    [ui/Segment style/basic
     (when @session
       [cc/metadata
        {:title       (:username @session)
         :icon        "user"
         :description (str (@tr [:session-expires]) " " (-> @session :expiry time/parse-iso8601 time/ago))}
        (->> @session
             cimi-api-utils/remove-common-attrs
             username-as-link
             format-roles
             process-session-data
             (map data-to-tuple)
             (map tuple-to-row))])
     (when-not @session
       [:p (@tr [:no-session])])]))


(defmethod panel/render :profile
  [path]
  [session-info])

