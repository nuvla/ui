(ns sixsq.slipstream.webui.acl.utils)


(defn owner-tuple
  [{:keys [principal type] :as owner}]
  [[false type principal] "ALL"])


(defn rule-tuple
  [{:keys [principal type right] :as rule}]
  [[true type principal] right])


(defn combine-rights
  [[k vs]]
  [k (set (map second vs))])


(defn acl-by-principal
  [{:keys [owner rules] :as acl}]
  (let [entries (concat [(owner-tuple owner)]
                        (map rule-tuple rules))]
    (->> entries
         (group-by first)
         (map combine-rights)
         (into (sorted-map)))))
