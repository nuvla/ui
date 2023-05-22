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
               {:name fa-class})])

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

(def eye "fa-light fa-eye")
(defn EyeIcon
  [opts]
  [I opts eye])

(def info "fal fa-circle-info")
(defn InfoIcon
  [opts]
  [I opts info])


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

(def publish "check circle outline")
(defn PublishIcon
  [opts]
  [I opts publish])

(def unpublish "fa-light fa-link-simple-slash")
(defn UnpublishIcon
  [opts]
  [I opts unpublish])

(def plus "fa-light fa-plus-large")
(defn AddIcon
  [opts]
  [I opts plus])

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