(ns sixsq.nuvla.ui.ui-demo.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch inject-cofx reg-event-db
                                   reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
            [sixsq.nuvla.ui.cimi.views :refer [MenuBar]]
            [sixsq.nuvla.ui.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

;; default-cols
;; -> vector of field-keys
;;    order important
;; available-cols
;; -> map of field-key->col-config
;; current-cols
;; -> vector of field-keys
;;    order important
;; only current-cols persisted in local-storage

(reg-event-fx
  ::remove-col
  (fn [{{:keys [::current-cols]} :db} [_ col-key]]
    {:fx [[:dispatch [::set-current-cols
                      (filterv #(not= col-key %) current-cols)]]]}))

(def default-columns
  [:id :name :description :created :updated])

(reg-event-db
  ::add-col
  (fn [{{:keys [::current-cols]
         :or {current-cols default-columns}} :db} [_ col-key position]]
    (when-not (some #{col-key} current-cols)
      (let [new-cols (if position
                       (vec (concat (take position current-cols)
                              [col-key]
                              (drop position current-cols)))
                       (into current-cols [col-key]))]
        {:fx [[:dispatch
               [::set-current-cols
                new-cols]]]}))))

(reg-event-fx
  ::reset-current-cols
  (fn [{{defaults ::default-cols} :db}]
    {:fx [[:dispatch [::set-current-cols (or defaults default-columns)]]]}))

(reg-event-db
  ::set-current-cols
  (fn [db [_ cols]]
(js/console.error cols)
    (assoc db ::current-cols cols)))

(comment
  (if (some #{:ka} [:k :b]) true false)

  ;; add as second column
  (dispatch [::add-col :updated 1])

  ;; remove column
  (dispatch [::reset-current-cols])
  (dispatch [::remove-col :updated])
  (dispatch [::remove-col :created])

  ;; no position adds as last column
  (dispatch [::add-col :updated])
  ;; adding same column again does not work
  (dispatch [::add-col :updated])

  ;; setting default column
  (dispatch [::set-current-cols default-columns])

  (dispatch [::reset-current-cols])
  )

(reg-sub
  ::get-current-cols
  :-> ::current-cols)

(reg-sub
  ::get-default-cols
  :-> ::default-cols)

(reg-event-fx
  ::init-table-col-config
  [(inject-cofx :storage/get {:name "nuvla.ui.table.column-configs"})]
  (fn [{{:keys [::current-cols] :as db} :db} [_ cols]]
    (let [defaults (or
                     (some->>
                       cols
                       (map :field-key))
                     default-columns)]
      {:db (assoc db ::default-cols defaults)
       :fx [[:dispatch [::set-current-cols (or current-cols defaults)]]]})))


(defn TableColsEditable
  [props]
  (let [available-cols (merge
                         (let [ks (mapcat keys (:rows props))]
                           (zipmap
                             ks
                             (map (fn [k] {:field-key k}) ks)))
                         (into {} (map (juxt :field-key identity) (:columns props))))
        current-cols   (subscribe [::get-current-cols])
        default-cols   (subscribe [::get-default-cols])
        ]
    (dispatch [::init-table-col-config (:columns props)  ])
    (fn []
      [Table (assoc props :col-config
               {:available-cols available-cols
                :current-cols @current-cols}
               :columns (mapv (fn [k] (available-cols k))
                          (or @current-cols @default-cols)))])))

(defn UiDemo
  []
   (let [collection (subscribe [::cimi-subs/collection])
         icons      (->> (ns-publics 'sixsq.nuvla.ui.utils.icons)
                         (filter #(and
                                    (not (str/starts-with? (name (first %)) "i-"))
                                    (not= (first %) "Icon")))
                         (sort-by first))]
     [ui/Tab
     {:panes [{:menuItem "Table"
               :render #(r/as-element
                          [:<>
                           [MenuBar]
                           [TableColsEditable {:rows (:resources @collection)}]])}
              {:menuItem "Icons"
               :render #(r/as-element
                          [ui/Segment
                           [:div {:style {:display :flex
                                          :flex-wrap :wrap
                                          :gap "50px"
                                          :margin :auto
                                          :margin-top "100px"
                                          :max-width "75%"}}
                            (for [[k v] icons]
                              (when (and
                                      (not (str/starts-with? (name k) "i-"))
                                      (not= (name k) "Icon"))
                                [:div {:style {:width "25%"}}
                                 [v {:size :large}]
                                 [:span (-> v symbol name)]]))]])}]}]))



(comment
  (let [ t {:rows
            [{:description "SSH public key of nacho@sixsq.com",
              :method "generate-ssh-key",
              :updated "2022-03-29T14:21:37.659Z",
              :name "Nacho SSH public key",
              :created "2022-03-01T14:34:11.020Z",
              :updated-by "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
              :created-by "user/85a8e4be-0373-4470-81a5-a7f0df81582a",
              :id "credential/30e769cd-100f-4cfa-818a-1a6bb5310f96",
              :resource-type "credential",
              :public-key
              "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC1lSQ+4ptacrI7qX0FCyQ7RAq9ZP4+PAdlso+OFCHF7342zibeNsnKkeTYqfRgl63qFbAZCXr0cKl/J5dVzDSb4nf3TlUhNU/GYmnXR3Zt0zyyP9BogBr9Lx0/y72lBYDz0hUGA+z/GClLHz1fpvsjJkeGEXJD5wXLM5mqXABwd4nfIshATjxkfx7U+5FxgjXgUrrWfSgMP7mnfvIEEBMbLoFj79o3qlyLvjdmVkrLGl8YERA8+Go8TYGbrRusAUFKWhdxFd8vp84qBsc6gpQjIwERkxc1JaAqOndqXuOGb2DQxub5JsM+SkFBne8GZqhEfCOpw/vAL76w0XMGQv2p3aLG2Y1OZIZJAtXqDZm8QbOMotGXml+2Nu31doqbej9qBQi5CbIkRAF7ZI3gOV9BFFwbvxn86CMWXuqzkyUQZNuyaV2CFZo/j3mg3PQJRB+EjVance4wnOKScTbhKOHrDo0KmIi3BvcUwmKIAlqPw6MoxoknEEDWKXx8pW4bsy0= nacho@sixsq.com",
              :subtype "ssh-key"}
             {:description "Lionel GPG key",
              :method "gpg-key",
              :updated "2022-04-12T17:21:29.219Z",
              :name "Lionel GPG public key",
              :created "2021-06-08T15:17:20.122Z",
              :updated-by "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
              :created-by "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
              :id "credential/c14b8c3c-4ad5-4827-8161-48d0247b1acd",
              :resource-type "credential",
              :public-key
              "-----BEGIN PGP PUBLIC KEY BLOCK-----\n\n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\nQA==\n=Gbex\n-----END PGP PUBLIC KEY BLOCK-----\n",
              :subtype "gpg-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/21301ff1-a058-46b9-bc5b-2ca02382f6cc",
              :parent "nuvlabox/21301ff1-a058-46b9-bc5b-2ca02382f6cc",
              :method "generate-api-key",
              :updated "2022-09-23T19:13:51.883Z",
              :name "[nuvlabox-playbook]21301ff1",
              :claims
              {:identity "nuvlabox/21301ff1-a058-46b9-bc5b-2ca02382f6cc",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/21301ff1-a058-46b9-bc5b-2ca02382f6cc"]},
              :created "2022-09-23T19:13:51.883Z",
              :digest
              "bcrypt+sha512$ce51348684a7be4b5f98b8b45f444d46$12$6ae73441e288673e41db3c8094e1f2afdd2e4e344bf29495",
              :created-by "nuvlabox/21301ff1-a058-46b9-bc5b-2ca02382f6cc",
              :id "credential/11f73f75-79de-4488-ba04-ff7691c5abd1",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/21301ff1-a058-46b9-bc5b-2ca02382f6cc"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/21301ff1-a058-46b9-bc5b-2ca02382f6cc"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/f562e3b9-fc73-4ba7-9cd9-088ed00ce859",
              :parent "nuvlabox/f562e3b9-fc73-4ba7-9cd9-088ed00ce859",
              :method "generate-api-key",
              :updated "2022-09-27T12:22:52.891Z",
              :name "[nuvlabox-playbook]f562e3b9",
              :claims
              {:identity "nuvlabox/f562e3b9-fc73-4ba7-9cd9-088ed00ce859",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/f562e3b9-fc73-4ba7-9cd9-088ed00ce859"]},
              :created "2022-09-27T12:22:52.891Z",
              :digest
              "bcrypt+sha512$f3e8c1b742a57ecd439de7ab78d550ff$12$e1db0110b465eacabaa61fdc0df1d61565d599d29504b037",
              :created-by "nuvlabox/f562e3b9-fc73-4ba7-9cd9-088ed00ce859",
              :id "credential/372471cd-597a-49dc-8314-3612dc93dcf0",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/f562e3b9-fc73-4ba7-9cd9-088ed00ce859"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/f562e3b9-fc73-4ba7-9cd9-088ed00ce859"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/8635bf1a-8d26-4723-9fda-76beb445428b",
              :parent "nuvlabox/8635bf1a-8d26-4723-9fda-76beb445428b",
              :method "generate-api-key",
              :updated "2022-09-27T12:18:09.882Z",
              :name "[nuvlabox-playbook]8635bf1a",
              :claims
              {:identity "nuvlabox/8635bf1a-8d26-4723-9fda-76beb445428b",
               :roles
               ["group/nuvla-nuvlabox"
                "nuvlabox/8635bf1a-8d26-4723-9fda-76beb445428b"
                "group/nuvla-anon"
                "group/nuvla-user"]},
              :created "2022-09-27T12:18:09.882Z",
              :digest
              "bcrypt+sha512$bb2470119044dbfdd3d8b588c7b159b9$12$1a26cffd6c1eccb76b2caaa7d3695ca65085afabbee778c6",
              :created-by "nuvlabox/8635bf1a-8d26-4723-9fda-76beb445428b",
              :id "credential/d7f9416f-ab20-44ad-b59e-dc4b6d84ad23",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/8635bf1a-8d26-4723-9fda-76beb445428b"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/8635bf1a-8d26-4723-9fda-76beb445428b"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/950db691-fedc-4d50-a0b0-f93e2cdeb580",
              :parent "nuvlabox/950db691-fedc-4d50-a0b0-f93e2cdeb580",
              :method "generate-api-key",
              :updated "2022-09-27T12:21:24.379Z",
              :name "[nuvlabox-playbook]950db691",
              :claims
              {:identity "nuvlabox/950db691-fedc-4d50-a0b0-f93e2cdeb580",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/950db691-fedc-4d50-a0b0-f93e2cdeb580"]},
              :created "2022-09-27T12:21:24.379Z",
              :digest
              "bcrypt+sha512$144a60c6902156ef1e2d5f52a4ced09a$12$d13d7a9bfa6ec04ff35b4d112cd85655e9e42f609dd8bac8",
              :created-by "nuvlabox/950db691-fedc-4d50-a0b0-f93e2cdeb580",
              :id "credential/d7677736-1971-44ac-bb8b-895476f34b73",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/950db691-fedc-4d50-a0b0-f93e2cdeb580"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/950db691-fedc-4d50-a0b0-f93e2cdeb580"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/48044a64-0ad9-41b5-8328-5cdc1bead123",
              :parent "nuvlabox/48044a64-0ad9-41b5-8328-5cdc1bead123",
              :method "generate-api-key",
              :updated "2022-09-27T12:26:47.263Z",
              :name "[nuvlabox-playbook]48044a64",
              :claims
              {:identity "nuvlabox/48044a64-0ad9-41b5-8328-5cdc1bead123",
               :roles
               ["group/nuvla-nuvlabox"
                "nuvlabox/48044a64-0ad9-41b5-8328-5cdc1bead123"
                "group/nuvla-anon"
                "group/nuvla-user"]},
              :created "2022-09-27T12:26:47.263Z",
              :digest
              "bcrypt+sha512$c1e274aa03b5686276c4b4aba2e430af$12$29bf966906b40052831aed872ec4cc3de574f0efc5e695ba",
              :created-by "nuvlabox/48044a64-0ad9-41b5-8328-5cdc1bead123",
              :id "credential/d7a805d3-bd99-4bf3-a063-3511643d6336",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/48044a64-0ad9-41b5-8328-5cdc1bead123"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/48044a64-0ad9-41b5-8328-5cdc1bead123"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/190483dc-07d0-4705-aa78-ec7f497659a7",
              :parent "nuvlabox/190483dc-07d0-4705-aa78-ec7f497659a7",
              :method "generate-api-key",
              :updated "2022-09-27T12:26:07.151Z",
              :name "[nuvlabox-playbook]190483dc",
              :claims
              {:identity "nuvlabox/190483dc-07d0-4705-aa78-ec7f497659a7",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/190483dc-07d0-4705-aa78-ec7f497659a7"]},
              :created "2022-09-27T12:26:07.151Z",
              :digest
              "bcrypt+sha512$99bb04eb6d75734a72278ac2b47264ee$12$620de7474c307b27799c296f3c2c5262e7dbd55b7a14f807",
              :created-by "nuvlabox/190483dc-07d0-4705-aa78-ec7f497659a7",
              :id "credential/d2b8bec3-d836-4384-8b39-c41a875b7a79",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/190483dc-07d0-4705-aa78-ec7f497659a7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/190483dc-07d0-4705-aa78-ec7f497659a7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/858d7bf7-3496-42e6-a96c-19e6cc5e6224",
              :parent "nuvlabox/858d7bf7-3496-42e6-a96c-19e6cc5e6224",
              :method "generate-api-key",
              :updated "2022-09-27T13:02:48.417Z",
              :name "[nuvlabox-playbook]858d7bf7",
              :claims
              {:identity "nuvlabox/858d7bf7-3496-42e6-a96c-19e6cc5e6224",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "nuvlabox/858d7bf7-3496-42e6-a96c-19e6cc5e6224"
                "group/nuvla-user"]},
              :created "2022-09-27T13:02:48.417Z",
              :digest
              "bcrypt+sha512$a440354af5e963c7251e7b4bec4a3c47$12$fbbbaab9fca80eb0379ca3794cee69c7def2aad96472dc85",
              :created-by "nuvlabox/858d7bf7-3496-42e6-a96c-19e6cc5e6224",
              :id "credential/acb4c333-6f71-4f53-a0f0-1bb5c62db59a",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/858d7bf7-3496-42e6-a96c-19e6cc5e6224"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/858d7bf7-3496-42e6-a96c-19e6cc5e6224"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/f7b35afd-92de-4b76-8db5-0522c29c44e9",
              :parent "nuvlabox/f7b35afd-92de-4b76-8db5-0522c29c44e9",
              :method "generate-api-key",
              :updated "2022-09-27T12:36:35.926Z",
              :name "[nuvlabox-playbook]f7b35afd",
              :claims
              {:identity "nuvlabox/f7b35afd-92de-4b76-8db5-0522c29c44e9",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/f7b35afd-92de-4b76-8db5-0522c29c44e9"]},
              :created "2022-09-27T12:36:35.926Z",
              :digest
              "bcrypt+sha512$e2025d2cf35bf025e05f3f3adb635b62$12$6566b3665490fae6bb3aa5b293418af8ee0a46178fbb26b1",
              :created-by "nuvlabox/f7b35afd-92de-4b76-8db5-0522c29c44e9",
              :id "credential/20adb4be-b957-4cc1-875b-bdc72955f5aa",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/f7b35afd-92de-4b76-8db5-0522c29c44e9"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/f7b35afd-92de-4b76-8db5-0522c29c44e9"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/750f7ba3-1f56-4e54-869e-3f230e7f3a6b",
              :parent "nuvlabox/750f7ba3-1f56-4e54-869e-3f230e7f3a6b",
              :method "generate-api-key",
              :updated "2022-09-27T12:53:13.194Z",
              :name "[nuvlabox-playbook]750f7ba3",
              :claims
              {:identity "nuvlabox/750f7ba3-1f56-4e54-869e-3f230e7f3a6b",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/750f7ba3-1f56-4e54-869e-3f230e7f3a6b"]},
              :created "2022-09-27T12:53:13.194Z",
              :digest
              "bcrypt+sha512$f70aecf9b238c8e14f5fe81d4c80ce01$12$1dae27dc49c8e0d10ee78cb300240e6e1eae30ed759131b3",
              :created-by "nuvlabox/750f7ba3-1f56-4e54-869e-3f230e7f3a6b",
              :id "credential/238fa623-a1f8-4805-bb22-9ba3fc9bf80a",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/750f7ba3-1f56-4e54-869e-3f230e7f3a6b"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/750f7ba3-1f56-4e54-869e-3f230e7f3a6b"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/ab75db13-9c04-4278-8838-47b982089838",
              :parent "nuvlabox/ab75db13-9c04-4278-8838-47b982089838",
              :method "generate-api-key",
              :updated "2022-10-14T06:35:43.531Z",
              :name "[nuvlabox-playbook]ab75db13",
              :claims
              {:identity "nuvlabox/ab75db13-9c04-4278-8838-47b982089838",
               :roles
               ["group/nuvla-nuvlabox"
                "nuvlabox/ab75db13-9c04-4278-8838-47b982089838"
                "group/nuvla-anon"
                "group/nuvla-user"]},
              :created "2022-10-14T06:35:43.531Z",
              :digest
              "bcrypt+sha512$153dbede775cd56887ce4ec7c2e3101e$12$7d4f05c2b08d9ba9038ea872b76c38fd9e4aca0f75d71ecf",
              :created-by "nuvlabox/ab75db13-9c04-4278-8838-47b982089838",
              :id "credential/2b1023ba-639e-49a2-b438-63dbb3981047",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/ab75db13-9c04-4278-8838-47b982089838"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/ab75db13-9c04-4278-8838-47b982089838"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "SSH credential generated for NuvlaEdge: Testing NuvlaEdge creation",
              :method "generate-ssh-key",
              :updated "2022-10-14T09:06:37.174Z",
              :name "SSH key for Testing NuvlaEdge creation",
              :created "2022-10-14T09:06:37.174Z",
              :created-by "user/90783050-e223-43db-a112-107ef509c6d3",
              :id "credential/4cdb5e08-9bf8-4228-af2e-ea8310f7094c",
              :resource-type "credential",
              :acl
              {:edit-data ["user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"],
               :view-acl ["user/90783050-e223-43db-a112-107ef509c6d3"],
               :delete ["user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta ["user/90783050-e223-43db-a112-107ef509c6d3"],
               :edit-acl ["user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data ["user/90783050-e223-43db-a112-107ef509c6d3"],
               :manage ["user/90783050-e223-43db-a112-107ef509c6d3"],
               :edit-meta ["user/90783050-e223-43db-a112-107ef509c6d3"]},
              :operations
              [{:rel "edit",
                :href "credential/4cdb5e08-9bf8-4228-af2e-ea8310f7094c"}
               {:rel "delete",
                :href "credential/4cdb5e08-9bf8-4228-af2e-ea8310f7094c"}],
              :public-key
              "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCFk0ceLjrDHtv5vkvN6nMUGhFGN7cIVSP+twkcl9ljY+DXrdv+OD2uLCFA2qP/XYFWx6gDBB5yqsTjHGozkWNU7K/CWnVGhhlOsvfSXPN/HAxyNYv4S6isfcEskr0yJkke1pInFfX3tZ2KZC6+5pC+hZxKmkanbP6raFDTsa864OS8xiFbVibpRJbMKKBSziqQrMGKqXfpDCPGIu1GMJEd46Qfj+oCRIxSnX8zmHiMS/pzoUqc7zd5SZ/OUknu5siPKaG8p1BasaXWWrkkuQyOxnPkRRun2qsFb7YU2JIELBv4TGMVocE6oXyrxd7otgsJ/GnFOPXaEHYB9BWHXry1CNXwlAkaoPzfeqyTLmkkkDUcB1UFR9ZQDgjsB8idnYFJHa2G7Tl47k6BX2mvP2GPMfYBy3P8cFUTdiD9tTGSxNFr+ulAcMnn3rCe2fCB68OHlR8GGTIhKvTF58v5pj3UH4a+QI2yBDSscBuF/uo/H9tZG192BEIA5G/tmLXKj6c=",
              :subtype "ssh-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  Testing NuvlaEdgeCreation and Deletion",
              :parent "nuvlabox/27b8d834-693b-499a-94df-107217e740b0",
              :method "generate-api-key",
              :updated "2022-10-14T09:09:29.767Z",
              :name "[nuvlabox-playbook]Testing NuvlaEdgeCreation and Deletion",
              :claims
              {:identity "nuvlabox/27b8d834-693b-499a-94df-107217e740b0",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/27b8d834-693b-499a-94df-107217e740b0"]},
              :created "2022-10-14T09:09:29.767Z",
              :digest
              "bcrypt+sha512$801854e2a5ad14c769fc923f032c4803$12$b204953ca25ff599cb0da458942359164a655950069a244a",
              :created-by "nuvlabox/27b8d834-693b-499a-94df-107217e740b0",
              :id "credential/157404bb-36d0-49a7-adb1-fbb7e0f7f751",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/27b8d834-693b-499a-94df-107217e740b0"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/27b8d834-693b-499a-94df-107217e740b0"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  Testing edge creation and deletion",
              :parent "nuvlabox/99f03762-d4be-49b6-9b73-2cf6579cbcfb",
              :method "generate-api-key",
              :updated "2022-10-14T09:14:28.808Z",
              :name "[nuvlabox-playbook]Testing edge creation and deletion",
              :claims
              {:identity "nuvlabox/99f03762-d4be-49b6-9b73-2cf6579cbcfb",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/99f03762-d4be-49b6-9b73-2cf6579cbcfb"]},
              :created "2022-10-14T09:14:28.808Z",
              :digest
              "bcrypt+sha512$5a89e1845874284a76ce95ada8b44437$12$f16cfe2d6a807ca1d2a3e373668cabb855c00b10bb9dffd2",
              :created-by "nuvlabox/99f03762-d4be-49b6-9b73-2cf6579cbcfb",
              :id "credential/56f32b72-d600-4bcd-a1c5-c4cec349772f",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/99f03762-d4be-49b6-9b73-2cf6579cbcfb"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/99f03762-d4be-49b6-9b73-2cf6579cbcfb"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  Testing NuvlaEdge creation",
              :parent "nuvlabox/c5038a65-e149-4888-a500-bc853e5a2cb6",
              :method "generate-api-key",
              :updated "2022-10-14T09:07:27.382Z",
              :name "[nuvlabox-playbook]Testing NuvlaEdge creation",
              :claims
              {:identity "nuvlabox/c5038a65-e149-4888-a500-bc853e5a2cb6",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "nuvlabox/c5038a65-e149-4888-a500-bc853e5a2cb6"
                "group/nuvla-user"]},
              :created "2022-10-14T09:07:27.382Z",
              :digest
              "bcrypt+sha512$9b500814a26d3e2f9dd9a9d99d00dc87$12$c1b424a4890bd61097e33e7b3f9e794276763eb81fdc7da6",
              :created-by "nuvlabox/c5038a65-e149-4888-a500-bc853e5a2cb6",
              :id "credential/d186c86a-3a42-4186-be5c-00aad908cd24",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/c5038a65-e149-4888-a500-bc853e5a2cb6"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/c5038a65-e149-4888-a500-bc853e5a2cb6"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  nuvlabox/e50cc6ba-6507-49b2-b377-d25ee17db9dc",
              :parent "nuvlabox/e50cc6ba-6507-49b2-b377-d25ee17db9dc",
              :method "generate-api-key",
              :updated "2022-10-14T07:14:13.652Z",
              :name "[nuvlabox-playbook]e50cc6ba",
              :claims
              {:identity "nuvlabox/e50cc6ba-6507-49b2-b377-d25ee17db9dc",
               :roles
               ["group/nuvla-nuvlabox"
                "nuvlabox/e50cc6ba-6507-49b2-b377-d25ee17db9dc"
                "group/nuvla-anon"
                "group/nuvla-user"]},
              :created "2022-10-14T07:14:13.652Z",
              :digest
              "bcrypt+sha512$8156a369d3561833ab1d51f0c08d2231$12$3f714fb5fa3873dbb3a06da69cd4c6498e4fc8474210b192",
              :created-by "nuvlabox/e50cc6ba-6507-49b2-b377-d25ee17db9dc",
              :id "credential/2d58cc4f-c7fc-47b7-a5a9-5d360d80c13f",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/e50cc6ba-6507-49b2-b377-d25ee17db9dc"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/e50cc6ba-6507-49b2-b377-d25ee17db9dc"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  Testing Edge creation and deletion",
              :parent "nuvlabox/2c721c16-97b9-4849-a20c-c632c3f1ed90",
              :method "generate-api-key",
              :updated "2022-10-14T09:59:24.669Z",
              :name "[nuvlabox-playbook]Testing Edge creation and deletion",
              :claims
              {:identity "nuvlabox/2c721c16-97b9-4849-a20c-c632c3f1ed90",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/2c721c16-97b9-4849-a20c-c632c3f1ed90"]},
              :created "2022-10-14T09:59:24.669Z",
              :digest
              "bcrypt+sha512$781b5c4014604909edc1b32595279bf3$12$10f9ae83d46a6f95a4732c4284dd9fa755eaf34aeee2c2a5",
              :created-by "nuvlabox/2c721c16-97b9-4849-a20c-c632c3f1ed90",
              :id "credential/63343741-b8a8-49ff-b993-2df312c3eab5",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/2c721c16-97b9-4849-a20c-c632c3f1ed90"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/2c721c16-97b9-4849-a20c-c632c3f1ed90"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  Testing Edge creation and deletion",
              :parent "nuvlabox/79644fca-42cc-4a42-aba0-9c5333c7636a",
              :method "generate-api-key",
              :updated "2022-10-14T10:00:23.614Z",
              :name "[nuvlabox-playbook]Testing Edge creation and deletion",
              :claims
              {:identity "nuvlabox/79644fca-42cc-4a42-aba0-9c5333c7636a",
               :roles
               ["group/nuvla-nuvlabox"
                "nuvlabox/79644fca-42cc-4a42-aba0-9c5333c7636a"
                "group/nuvla-anon"
                "group/nuvla-user"]},
              :created "2022-10-14T10:00:23.614Z",
              :digest
              "bcrypt+sha512$4549b183cafe49fc5f420dfe14a6f0b3$12$0774fd5ada9515223b67eb6f4935b3c489fd4572144e8e30",
              :created-by "nuvlabox/79644fca-42cc-4a42-aba0-9c5333c7636a",
              :id "credential/b5991ca2-3e48-4a99-861f-08eafa532ad7",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/79644fca-42cc-4a42-aba0-9c5333c7636a"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/79644fca-42cc-4a42-aba0-9c5333c7636a"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}
             {:description
              "[nuvlabox-playbook] Generated API Key for  Testing Edge creation and deletion",
              :parent "nuvlabox/43cbd1c9-f3c1-4b20-ab9d-462e6b8daba4",
              :method "generate-api-key",
              :updated "2022-10-14T09:41:50.174Z",
              :name "[nuvlabox-playbook]Testing Edge creation and deletion",
              :claims
              {:identity "nuvlabox/43cbd1c9-f3c1-4b20-ab9d-462e6b8daba4",
               :roles
               ["group/nuvla-nuvlabox"
                "group/nuvla-anon"
                "group/nuvla-user"
                "nuvlabox/43cbd1c9-f3c1-4b20-ab9d-462e6b8daba4"]},
              :created "2022-10-14T09:41:50.174Z",
              :digest
              "bcrypt+sha512$682ee8dbd030b9734ce3eb8bb77e3e5c$12$4222c06f94d2982f6abfad84b5d5302d33f584e558e6ed07",
              :created-by "nuvlabox/43cbd1c9-f3c1-4b20-ab9d-462e6b8daba4",
              :id "credential/b0d90363-e769-4481-9fc2-4f7d079fa803",
              :resource-type "credential",
              :acl
              {:view-acl
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-meta
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/43cbd1c9-f3c1-4b20-ab9d-462e6b8daba4"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :view-data
               ["infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7"
                "nuvlabox/43cbd1c9-f3c1-4b20-ab9d-462e6b8daba4"
                "user/90783050-e223-43db-a112-107ef509c6d3"],
               :owners ["group/nuvla-admin"]},
              :subtype "api-key"}]}]


    ((some-fn (comp #(some->> % ( map :field-key)) :columns) (comp keys first :rows)) t)
    ))
