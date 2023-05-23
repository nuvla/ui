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

(def rocket "fal fa-rocket-launch")
(defn RocketIcon [opts]
  [I opts rocket])

(def folder "fal fa-folder")
(defn FolderIcon
  [opts]
  [I opts folder])

(def folder-full "folder")
(defn FolderIconFull
  [opts]
  [I opts folder-full])

(def grid "fal fa-grid")
(defn GridIcon
  [opts]
  [I opts grid])

(def grid-layout "grid layout")
(defn GridLayoutIcon
  [opts]
  [I opts grid])

(def eye "fal fa-eye")
(defn EyeIcon
  [opts]
  [I opts eye])

(def info "fal fa-circle-info")
(defn InfoIcon
  [opts]
  [I opts info])

(def info-full "info circle")
(defn InfoIconFull
  [opts]
  [I opts info-full])

(def gear "fal fa-gear")
(defn GearIcon
  [opts]
  [I opts gear])


(def code "fal fa-file-code")
(defn CodeIcon
  [opts]
  [I opts code])

(def trash "fal fa-trash")
(defn TrashIcon
  [opts]
  [I opts trash])

(def trash-full "fa-trash")
(defn TrashIconFull
  [opts]
  [I opts trash-full])

(def circle-check "check circle outline")
(defn CircleCheck
  [opts]
  [I opts circle-check])

(def unpublish "fa-light fa-link-simple-slash")
(defn UnpublishIcon
  [opts]
  [I opts unpublish])

(def plus "fa-light fa-plus-large")
(defn AddIcon
  [opts]
  [I opts plus])

(def plus-full "plus")
(defn AddIconFull
  [opts]
  [I opts plus-full])

(def copy "fa-light fa-copy")
(defn CopyIcon
  [opts]
  [I opts copy])

(def cubes "fal fa-cubes")
(defn CubesIcon
  [opts]
  [I opts cubes])

(def docker "docker")
(defn DockerIcon
  [opts]
  [I opts docker])

(def app-sets "th large")
(defn AppSetsIcon
  [opts]
  [I opts app-sets])

(def warning "warning sign")
(defn WarningIcon
  [opts]
  [I opts warning])

(def book "fal fa-book")
(defn BookIcon
  [opts]
  [I opts book])

(def euro  "fa-light fa-euro-sign")
(defn EuroIcon
  [opts]
  [I opts euro])

(def user-group "fa-light fa-user-group")
(defn GroupIcon
  [opts]
  [I opts user-group])

(def user-large "fa-light fa-user-large")
(defn UserLargeIcon
  [opts]
  [I opts user-large])

(def layer-group "fal fa-layer-group")
(defn LayerGroupIcon
  [opts]
  [I opts layer-group])