(ns sixsq.nuvla.ui.acl.views
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [reagent.core :as reagent]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def local-acl (reagent/atom nil))

(def rights-keys [:edit-acl,
                  :edit-data,
                  :edit-meta,
                  :view-acl,
                  :view-data,
                  :view-meta,
                  :manage,
                  :delete])


(defn update-acl-principal
  [acl keys fn-update]
  (loop [updated-acl acl
         left-keys keys]
    (let [key (peek left-keys)
          new-acl (update updated-acl key fn-update)]
      (if (empty? left-keys)
        updated-acl
        (recur new-acl (pop left-keys))))))


(defn remove-principal
  [acl keys principal]
  (update-acl-principal acl keys
                        (fn [collection]
                          (remove #(= % principal) collection))))


(defn add-principal
  [acl keys principal]
  (update-acl-principal acl keys
                        (fn [collection]
                          (conj collection principal))))


(defn checked
  [v]
  (if v [ui/Icon {:name "check"}] "\u0020"))


(defn acl-row
  [[right principals]]
  [ui/TableRow
   [ui/TableCell {:collapsing true, :text-align "left"} right]
   [ui/TableCell {:collapsing true, :text-align "left"} (str/join ", " principals)]])


#_(defn acl-table
  [acl]
  (when acl
    (let [rows (map acl-row acl)]
      [table/wrapped-table "shield" "permissions"
       [ui/Table style/acl
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell {:collapsing true, :text-align "left"} "right"]
          [ui/TableHeaderCell {:collapsing true, :text-align "left"} "principals"]]]
        (vec (concat [ui/TableBody] rows))]])))


(defn acl-table-headers
  []
  [ui/TableHeader
   [ui/TableRow
    [ui/TableHeaderCell "Rights"]
    [ui/TableHeaderCell {:text-align "center"} "View"]
    [ui/TableHeaderCell {:text-align "center"} "Edit"]
    [ui/TableHeaderCell {:text-align "center"} "Manage"]
    [ui/TableHeaderCell {:text-align "center"} "Delete"]
    [ui/TableHeaderCell ""]]])


(defn principal-icon
  [principal]
  [ui/Icon {:name (if (str/starts-with? principal "user/")
                    "user"
                    "users")}])


(defn owner-item
  [removable? principal]
  ^{:key (str "owner_" principal)}
  [ui/ListItem
   [ui/ListContent
    [ui/ListHeader
     [principal-icon principal]
     ff/nbsp
     principal
     ff/nbsp
     (when removable? [ui/Icon {:name  "close"
                                :link  true
                                :size  "small"
                                :color "red"}])
     ]]])


(defn right-checkbox
  [acl read-only principal right-kw]
  (let [checked? (boolean (some #(= % principal) (right-kw acl)))]
    [ui/Checkbox {:checked   checked?
                  :on-change #(if checked?
                                (reset! local-acl (remove-principal acl [right-kw] principal))
                                (reset! local-acl (add-principal acl [right-kw] principal)))
                  :disabled  read-only}]))


(defn principal-row
  [acl read-only principal]
  ^{:key (str "right_" principal)}
  [ui/TableRow
   [ui/TableCell
    [principal-icon principal]
    ff/nbsp
    principal]
   [ui/TableCell {:text-align "center"}
    [right-checkbox acl read-only principal :view-acl]]
   [ui/TableCell {:text-align "center"}
    [right-checkbox acl read-only principal :edit-acl]]
   [ui/TableCell {:text-align "center"}
    [right-checkbox acl read-only principal :manage]]
   [ui/TableCell {:text-align "center"}
    [right-checkbox acl read-only principal :delete]]
   [ui/TableCell {:text-align "center"}
    (when-not read-only
      [ui/Icon {:link     true
                :name     "trash"
                :color    "red"
                :on-click #(reset! local-acl (remove-principal
                                               @local-acl
                                               rights-keys
                                               principal))

                }])]])


(defn additional-right
  [local-acl]
  (let [new-permission (reagent/atom {:principal nil
                                      :rights    #{}})]
    (fn []
      (log/warn @new-permission)
      (vec (concat
             [ui/TableRow]

             [[ui/TableCell
               [ui/Dropdown {:allow-additions true
                             :selection       true
                             :addition-label  "select user"
                             :search          true
                             :options         [{:key "a" :text "a" :value "user/a"}
                                               {:key "b" :text "b" :value "user/b"}]
                             ;:on-add-item    #(add-data-type-options (-> % .-target .-value))
                             :on-change       (ui-callback/value
                                                #(reset! new-permission (assoc @new-permission :principal %)))}]]]

             (map
               (fn [right-kw]
                 [ui/TableCell {:text-align "center"}
                  [ui/Checkbox
                   {:default-checked false
                    :on-change       (ui-callback/checked
                                       (fn [checked]
                                         (if checked
                                           (reset! new-permission (update @new-permission :rights conj right-kw))
                                           (reset! new-permission (update @new-permission :rights disj right-kw)))

                                         ))}]])
               [:view-acl :edit-acl :manage :delete])

             [[ui/TableCell {:text-align "center"}
               (let [{:keys [principal rights]} @new-permission]
                 (when (and principal (not-empty rights))
                  [ui/Icon {:name     "plus"
                            :link     true
                            :color    "green"
                            :on-click #(reset! local-acl (add-principal @local-acl (vec rights) principal))
                            }]))]]

             )))))



(defn acl-table
  [acl & {:keys [read-only]
          :or   {read-only false}}]
  (reset! local-acl acl)
  (fn []
    (log/error @local-acl)
    (when @local-acl
      (let [principals (->> (dissoc @local-acl :owners)
                            (mapcat (fn [[right principal]] principal))
                            (set)
                            (sort))
            owners (:owners @local-acl)]
        [:div
         [ui/Header {:as "h5" :attached "top"} "Owners"]
         [ui/Segment {:attached true}
          (vec (concat [ui/ListSA {:horizontal true}]
                       (map (partial owner-item (pos-int? (dec (count owners)))) owners)
                       (when-not read-only
                         [[ui/ListItem
                           [ui/ListContent
                            [ui/ListHeader
                             [ui/Input]
                             ]]]])))]
         [ui/Segment (merge {:attached true} style/autoscroll-x)
          ;(pr-str acl)
          ;(pr-str principals)
          [ui/Segment {:basic true}
           [ui/Table {:basic       "very"
                      :unstackable true}
            [acl-table-headers]

            (vec (concat [ui/TableBody]
                         (map (partial principal-row @local-acl read-only) principals)

                         [[additional-right local-acl]]

                         ))

            ]]
          ]
         ])
      #_(pr-str local-acl)))
  )
