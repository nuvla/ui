(ns sixsq.nuvla.ui.utils.icons
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn Icon
  [{:keys [name] :as opts}]
  [ui/Icon
   (if (some #(str/starts-with? name %) ["fa-" "fal " "fad " "fas "])
     (-> opts (dissoc :name) (assoc :class name))
     opts)])

(defn- I
  [opts fa-class]
  [Icon (merge (dissoc opts :name)
               {:name (->> (remove nil?
                                   [fa-class
                                    (:name opts)
                                    (:class opts)
                                    (:class-name opts)
                                    (:className opts)])
                           (str/join " "))})])

(def i-rocket "fal fa-rocket-launch")
(defn RocketIcon [opts]
  [I opts i-rocket])

(def i-folder "fal fa-folder")
(defn FolderIcon
  [opts]
  [I opts i-folder])

(def i-folder-full "folder")
(defn FolderIconFull
  [opts]
  [I opts i-folder-full])

(def i-grid "fal fa-grid")
(defn GridIcon
  [opts]
  [I opts i-grid])

(def i-grid-layout "grid layout")
(defn GridLayoutIcon
  [opts]
  [I opts i-grid])

(def i-eye "fal fa-eye")
(defn EyeIcon
  [opts]
  [I opts i-eye])

(def i-info "fal fa-circle-info")
(defn InfoIcon
  [opts]
  [I opts i-info])

(def i-info-full "info circle")
(defn InfoIconFull
  [opts]
  [I opts i-info-full])

(def i-gear "fal fa-gear")
(defn GearIcon
  [opts]
  [I opts i-gear])

(def i-code "fal fa-file-code")
(defn CodeIcon
  [opts]
  [I opts i-code])

(def i-trash "fal fa-trash")
(defn TrashIcon
  [opts]
  [I opts i-trash])

(def i-trash-full "fa-trash")
(defn TrashIconFull
  [opts]
  [I opts i-trash-full])

(def i-circle-check "check circle outline")
(defn CircleCheck
  [opts]
  [I opts i-circle-check])

(def i-unpublish "fal fa-link-simple-slash")
(defn UnpublishIcon
  [opts]
  [I opts i-unpublish])

(def i-plus "fal fa-plus-large")
(defn AddIcon
  [opts]
  [I opts i-plus])

(def i-plus-full "plus")
(defn AddIconFull
  [opts]
  [I opts i-plus-full])

(def i-copy "fal fa-copy")
(defn CopyIcon
  [opts]
  [I opts i-copy])

(def i-cubes "fal fa-cubes")
(defn CubesIcon
  [opts]
  [I opts i-cubes])

(def i-docker "docker")
(defn DockerIcon
  [opts]
  [I opts i-docker])

(def i-app-sets "th large")
(defn AppSetsIcon
  [opts]
  [I opts i-app-sets])

(def i-warning "warning sign")
(defn WarningIcon
  [opts]
  [I opts i-warning])

(def i-book "fal fa-book")
(defn BookIcon
  [opts]
  [I opts i-book])

(def i-euro  "fal fa-euro-sign")
(defn EuroIcon
  [opts]
  [I opts i-euro])

(def i-user-group "fal fa-user-group")
(defn UserGroupIcon
  [opts]
  [I opts i-user-group])

(def i-user-large "fal fa-user-large")
(defn UserLargeIcon
  [opts]
  [I opts i-user-large])

(def i-users "fal fa-users")
(defn UsersIcon
  [opts]
  [I opts i-users])

(def i-user  "fal fa-user")
(defn UserIcon
  [opts]
  [I opts i-user])

(def i-layer-group "fal fa-layer-group")
(defn LayerGroupIcon
  [opts]
  [I opts i-layer-group])


(def i-box "fal fa-box")
(defn BoxIcon
  [opts]
  [I opts i-box])

(def i-key "fal fa-key")
(defn KeyIcon
  [opts]
  [I opts i-key])

(def i-bullseye "fal fa-bullseye")
(defn BullseyeIcon
  [opts]
  [I opts i-bullseye])

(def i-bell  "fal fa-bell")
(defn BellIcon
  [opts]
  [I opts i-bell])

(def i-db "fal fa-database")
(defn DbIcon
  [opts]
  [I opts i-db])

(def i-db-full "fa-database")
(defn DbIconFull
  [opts]
  [I opts i-db-full])

(def i-cloud "fal fa-cloud")
(defn CloudIcon
  [opts]
  [I opts i-cloud])

(def i-table-cells "fal fa-table-cells-large")
(defn TableCellIcon
  [opts]
  [I opts i-table-cells])

(def i-floppy "fal fa-floppy-disk")
(defn FloppyIcon
  [opts]
  [I opts i-floppy])

(def i-tag "fa-light fa-tag")
(defn TagIcon
  [opts]
  [I opts i-tag])

(def i-crown "fas fa-crown")
(defn CrownIcon
  [opts]
  [I opts i-crown])

(def i-power-off "fal fa-power-off")
(defn PowerOffIcon
  [opts]
  [I opts i-power-off])

(def i-store "fal fa-store")
(defn StoreIcon
  [opts]
  [I opts i-store])

(def i-list "fal fa-list")
(defn ListIcon
  [opts]
  [I opts i-list])

(def i-star "fa-light fa-star")
(defn StarIcon
  [opts]
  [I opts i-star])

(def i-robot "fa-solid fa-robot")
(defn RobotIcon
  [opts]
  [I opts i-robot])

(def i-chart-network "fas fa-chart-network")
(defn ChartNetworkIcon
  [opts]
  [I opts i-chart-network])

(def i-check "fal fa-check")
(defn CheckNetworkIcon
  [opts]
  [I opts i-check])

(def i-handshake "fal fa-handshake")
(defn HandshakeIcon
  [opts]
  [I opts i-handshake])

(def i-dolly "fal fa-dolly")
(defn DollyIcon
  [opts]
  [I opts i-dolly])

(def i-eraser "fal fa-eraser")
(defn EraserIcon
  [opts]
  [I opts i-eraser])

(def i-ban "fal fa-ban")
(defn BanIcon
  [opts]
  [I opts i-ban])

(def i-pause "fal fa-pause")
(defn PauseIcon
  [opts]
  [I opts i-pause])

(def i-exclamation "fal fa-exclamation")
(defn ExclamationIcon
  [opts]
  [I opts i-exclamation])

(def i-circle-play "fal fa-circle-play")
(defn CirclePlayIcon
  [opts]
  [I opts i-circle-play])

(def i-loader "fal fa-loader")
(defn LoaderIcon
  [opts]
  [I opts i-loader])

(def i-circle-stop "fal fa-circle-stop")
(defn CircleStopIcon
  [opts]
  [I opts i-circle-stop])

(def i-triangle-exclamation "fal fa-triangle-exclamation")
(defn TriangleExclamationIcon
  [opts]
  [I opts i-triangle-exclamation])

(def i-arrow-down "fa-light fa-arrow-down")
(defn ArrowDownIcon
  [opts]
  [I opts i-arrow-down])

(def i-arrow-right-bracket "fa-light fa-arrow-right-from-bracket")
(defn ArrowRightFromBracketIcon
  [opts]
  [I opts i-arrow-right-bracket])