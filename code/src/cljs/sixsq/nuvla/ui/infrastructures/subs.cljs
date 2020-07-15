(ns sixsq.nuvla.ui.infrastructures.subs
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.infrastructures.spec :as spec]))


(reg-sub
  ::infra-service-groups
  ::spec/infra-service-groups)


(reg-sub
  ::infra-services
  (fn [db]
    (::spec/infra-services db)))


(reg-sub
  ::services-in-group
  :<- [::infra-services]
  (fn [services [_ group-id]]
    (-> services
        :groups
        (get group-id))))


(reg-sub
 ::management-credentials-available
 (fn [db]
   (::spec/management-credentials-available db)))


(reg-sub
  ::elements-per-page
  ::spec/elements-per-page)


(reg-sub
  ::page
  ::spec/page)


(reg-sub
  ::is-new?
  ::spec/is-new?)


;; Validation

; Is the form valid?

(reg-sub
  ::form-valid?
  ::spec/form-valid?)


; Should the form be validated?

(reg-sub
  ::validate-form?
  ::spec/validate-form?)


(reg-sub
  ::active-input
  ::spec/active-input)


(reg-sub
  ::infra-service
  ::spec/infra-service)


(reg-sub
  ::service-group
  ::spec/service-group)


(reg-sub
  ::service-modal-visible?
  (fn [db]
    (::spec/service-modal-visible? db)))


(reg-sub
  ::add-service-modal-visible?
  (fn [db]
    (::spec/add-service-modal-visible? db)))

(reg-sub
 ::ssh-keys
 (fn [db]
   (::spec/ssh-keys db)))


(reg-sub
 ::ssh-keys-options
 (fn [db]
   (let [ssh-keys-infra          (::spec/ssh-keys-infra db)
         ssh-keys-set            (-> db
                                     ::spec/ssh-keys
                                     set)
         ssh-keys-infra-set      (set (map :id ssh-keys-infra))
         not-existing-ssh-keys   (set/difference ssh-keys-set ssh-keys-infra-set)]
     (map (fn [{:keys [id name]}]
            {:key id, :value id, :text (or name id)})
          (concat ssh-keys-infra
                  (map (fn [id] {:id id}) not-existing-ssh-keys))))))


(reg-sub
  ::mgmt-creds-set?
  (fn [db]
    (let [creds (get-in db [::spec/infra-service :management-credential])]
      (and creds (not (str/blank? creds))))))

(reg-sub
  ::mgmt-cred-subtype
  (fn [db]
    (if-let [mgmt-cred-id (get-in db [::spec/infra-service :management-credential])]
      (->> (::spec/management-credentials-available db)
           (filter (comp #{mgmt-cred-id} :id))
           first
           :subtype))))