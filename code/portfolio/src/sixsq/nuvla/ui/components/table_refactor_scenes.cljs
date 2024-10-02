(ns sixsq.nuvla.ui.components.table-refactor-scenes
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn TableController
  [{:keys [selectable? !global-filter]
    :or   {selectable? false}}]
  (r/with-let [reset-atom (r/atom 0)]
    [:div
     [ui/Button {:on-click #(swap! reset-atom inc)} "Reset"]
     ^{:key @reset-atom}
     [table-refactor/TableController {:!selectable?     (r/atom selectable?)
                                      :!columns         (r/atom [{::table-refactor/field-key      :Id
                                                                  ::table-refactor/header-content "Id"
                                                                  ::table-refactor/no-delete      true}
                                                                 {::table-refactor/field-key      :Size
                                                                  ::table-refactor/header-content "Size"}
                                                                 {::table-refactor/field-key      :Created
                                                                  ::table-refactor/header-content "Created"}])
                                      :!default-columns (r/atom [:Id :Size :Created])
                                      :!global-filter   !global-filter
                                      :row-id-fn        :Id
                                      :!data            (r/atom [{:RepoDigests
                                                                  ["nuvladev/nuvlaedge@sha256:56f8fe1fdf35d50577ab135dcbf78cfb877ccdc41948ec9352d526614b7462f2"],
                                                                  :Labels
                                                                  {:git.build.time                   "$(date --utc +%FT%T.%3NZ)",
                                                                   :org.opencontainers.image.vendor  "SixSq SA",
                                                                   :org.opencontainers.image.created "$(date --utc +%FT%T.%3NZ)",
                                                                   :git.run.id                       "10816164086",
                                                                   :org.opencontainers.image.url
                                                                   "https://github.com/nuvlaedge/nuvlaedge",
                                                                   :org.opencontainers.image.authors "support@sixsq.com",
                                                                   :git.branch                       "coe-resources",
                                                                   :git.commit.id                    "24bb0659461896b22a4a8b675a30b011bbf4efe4",
                                                                   :org.opencontainers.image.title   "NuvlaEdge",
                                                                   :org.opencontainers.image.description
                                                                   "Common image for NuvlaEdge software components",
                                                                   :git.run.number                   "839"},
                                                                  :SharedSize -1,
                                                                  :Size       192121737,
                                                                  :Id
                                                                  "sha256:b4a4526cfd461c7bd1ad3b3e864b9a3f671890b2c42ea0cbad55dd999ab6ae9c",
                                                                  :Containers -1,
                                                                  :ParentId   "",
                                                                  :Created    1726074087,
                                                                  :RepoTags   ["nuvladev/nuvlaedge:coe-resources"]}
                                                                 {:RepoDigests
                                                                  ["nuvladev/nuvlaedge@sha256:33426aed6440dccdd36e75b5a46073d0888295496c17e2afcdddb51539ea7b99"],
                                                                  :Labels
                                                                  {:git.build.time                   "$(date --utc +%FT%T.%3NZ)",
                                                                   :org.opencontainers.image.vendor  "SixSq SA",
                                                                   :org.opencontainers.image.created "$(date --utc +%FT%T.%3NZ)",
                                                                   :git.run.id                       "10746857192",
                                                                   :org.opencontainers.image.url
                                                                   "https://github.com/nuvlaedge/nuvlaedge",
                                                                   :org.opencontainers.image.authors "support@sixsq.com",
                                                                   :git.branch                       "coe-resources",
                                                                   :git.commit.id                    "46a2ba7903ee7a1faa54b2aba9e283242c1bee5a",
                                                                   :org.opencontainers.image.title   "NuvlaEdge",
                                                                   :org.opencontainers.image.description
                                                                   "Common image for NuvlaEdge software components",
                                                                   :git.run.number                   "836"},
                                                                  :SharedSize -1,
                                                                  :Size       191903136,
                                                                  :Id
                                                                  "sha256:bd1e8ef984a199d31d3fc478431165ca0236176ad62fab2a4e68a2c5b8e12fbd",
                                                                  :Containers -1,
                                                                  :ParentId   "",
                                                                  :Created    1725667915,
                                                                  :RepoTags   []}
                                                                 {:RepoDigests
                                                                  ["nuvladev/nuvlaedge@sha256:2d92c970a5d8ce3e2fae5b88bb4d2a2cf701b0cdd4aa41e883aea79cd3e61859"],
                                                                  :Labels
                                                                  {:git.build.time                   "$(date --utc +%FT%T.%3NZ)",
                                                                   :org.opencontainers.image.vendor  "SixSq SA",
                                                                   :org.opencontainers.image.created "$(date --utc +%FT%T.%3NZ)",
                                                                   :git.run.id                       "10746651311",
                                                                   :org.opencontainers.image.url
                                                                   "https://github.com/nuvlaedge/nuvlaedge",
                                                                   :org.opencontainers.image.authors "support@sixsq.com",
                                                                   :git.branch                       "coe-resources",
                                                                   :git.commit.id                    "109a34446f5edf006fb1400ca266490492bf7363",
                                                                   :org.opencontainers.image.title   "NuvlaEdge",
                                                                   :org.opencontainers.image.description
                                                                   "Common image for NuvlaEdge software components",
                                                                   :git.run.number                   "835"},
                                                                  :SharedSize -1,
                                                                  :Size       191903117,
                                                                  :Id
                                                                  "sha256:0ec61197db8b0989753da0c499be52b48c5d746a7d675ae358e157912d7d47bb",
                                                                  :Containers -1,
                                                                  :ParentId   "",
                                                                  :Created    1725666894,
                                                                  :RepoTags   []}])}]]))

(defscene Table
  [TableController])

(defscene Selectable
  [TableController {:selectable? true}])

(defn SearchInput
  [!global-filter]
  (js/console.info "Render SearchInput")
  [ui/Input {:style       {:padding "4px"}
             :class       :global-filter
             :placeholder "search..."
             :on-change   (ui-callback/input-callback #(reset! !global-filter %))}])

(defscene GlobalFilter
  (let [!global-filter (r/atom "")]
    [:div
     [SearchInput !global-filter]
     [TableController {:global-filter? true
                       :!global-filter !global-filter}]]))
