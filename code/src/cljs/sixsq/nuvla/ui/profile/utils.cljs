(ns
  sixsq.nuvla.ui.profile.utils
  (:require
    [re-frame.core :refer [subscribe]]
    [clojure.string :as str]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [taoensso.timbre :as log]
    [cljs.spec.alpha :as s]))


(defn db->new-credential
  [db]
  (let [name                    (get-in db [::spec/credential :name])
        description             (get-in db [::spec/credential :description])
        type                    (get-in db [::spec/credential :type])
        ca                      (get-in db [::spec/credential :ca])
        cert                    (get-in db [::spec/credential :cert])
        key                     (get-in db [::spec/credential :key])
        infrastructure-services (get-in db [:infrastructure-services] [])]
    (log/infof "->credential desc. %s" description)
    (let [c (as-> {} c
                  (dissoc c)
                  (assoc c :name name)
                  (assoc c :description description)
                  (assoc-in c [:template :href] (str "credential-template/" type))
                  (assoc-in c [:template :infrastructure-services] infrastructure-services)
                  (assoc-in c [:template :ca] ca)
                  (assoc-in c [:template :cert] cert)
                  (assoc-in c [:template :key] key))]
      (log/infof "c: %s" c)
      c)))


(defn credential->db
  [db credential]
  (-> db
      (dissoc ::spec/credential)
      (assoc-in [::spec/credential ::spec/name] (get-in credential [:name]))
      (assoc-in [::spec/credential ::spec/description] (get-in credential [:description]))
      (assoc-in [::spec/credential ::spec/ca] (get-in credential [:ca]))
      (assoc-in [::spec/credential ::spec/cert] (get-in credential [:cert]))
      (assoc-in [::spec/credential ::spec/key] (get-in credential [:key]))
      (assoc-in [::spec/credential ::spec/type] (get-in credential [:type]))))


; TODO: has moved to utils/general
(defn can-operation? [operation data]
  (->> data :operations (map :rel) (some #{(name operation)}) nil? not))


(defn can-edit? [data]
  (can-operation? :edit data))


(defn editable?
  [module is-new?]
  (or is-new? (can-edit? module)))


(defn mandatory-name
  [name]
  [:span name [:sup " " [ui/Icon {:name  :asterisk
                                  :size  :tiny
                                  :color :red}]]])
