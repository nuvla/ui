(ns sixsq.nuvla.ui.utils.icons
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn Icon
  [{:keys [name] :as opts}]
  [ui/Icon
   (if (and (or (string? name) (keyword? name))
            (some #(str/starts-with? (str (symbol name)) %) ["fa-" "fal " "fad " "fas "]))
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

(def i-file-code "fal fa-file-code")
(defn FileCodeIcon
  [opts]
  [I opts i-file-code])

(def i-file "fal fa-file")
(defn FileIcon
  [opts]
  [I opts i-file])

(def i-trash "fal fa-trash")
(defn TrashIcon
  [opts]
  [I opts i-trash])

(def i-trash-full "fa-trash")
(defn TrashIconFull
  [opts]
  [I opts i-trash-full])

(def i-circle-check "circle check outline")
(defn CircleCheck
  [opts]
  [I opts i-circle-check])

(def i-unpublish "fal fa-link-simple-slash")
(defn UnpublishIcon
  [opts]
  [I opts i-unpublish])

(def i-link "fal fa-link")
(defn LinkIcon
  [opts]
  [I opts i-link])

(def i-plus-large "fal fa-plus-large")
(defn AddIconLarge
  [opts]
  [I opts i-plus-large])

(def i-plus "fal fa-plus")
(defn AddIcon  [opts]
  [I opts i-plus-large])

(def i-plus-full "plus")
(defn AddIconFull
  [opts]
  [I opts i-plus-full])

(def i-minus "fal fa-minus")
(defn MinusIcon
  [opts]
  [I opts i-minus])

(def i-clone "fal fa-clone")
(defn CloneIcon
  [opts]
  [I opts i-clone])

(def i-copy "fal fa-copy")
(defn CopyIcon
  [opts]
  [I opts i-copy])

(def i-paste "fa-paste")
(defn PasteIcon
  [opts]
  [I opts i-paste])

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

(def i-user "fal fa-user")
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

(def i-table "fa-table")
(defn TableIcon
  [opts]
  [I opts i-table])

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

(def i-power "fal fa-power-off")
(defn PowerIcon
  [opts]
  [I opts i-power])

(def i-power-full "fa-power-off")
(defn PowerIconFull
  [opts]
  [I opts i-power-full])

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
(defn CheckIcon
  [opts]
  [I opts i-check])

(def i-check-full "fa-check")
(defn CheckIconFull
  [opts]
  [I opts i-check-full])

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

(def i-arrow-left "fa-light fa-arrow-left")
(defn ArrowLeftIcon
  [opts]
  [I opts i-arrow-left])

(def i-arrow-right-bracket "fa-light fa-arrow-right-from-bracket")
(defn ArrowRightFromBracketIcon
  [opts]
  [I opts i-arrow-right-bracket])

(def i-arrow-rotate "fa-light fa-arrows-rotate")
(defn ArrowRotateIcon
  [opts]
  [I opts i-arrow-rotate])

(def i-angle-down "fa-light fa-angle-down")
(defn AngleDownIcon
  [opts]
  [I opts i-angle-down])

(def i-angle-up "fa-light fa-angle-up")
(defn AngleUpIcon
  [opts]
  [I opts i-angle-up])

(def i-angle-right "fa-light fa-angle-right")
(defn AngleRightIcon
  [opts]
  [I opts i-angle-right])

(def i-medal "fas fa-medal")
(defn MedalIcon
  [opts]
  [I opts i-medal])

(def i-xmark "fa-xmark")
(defn XMarkIcon
  [opts]
  [I opts i-xmark])

(def i-filter-full "fa-filter")
(defn FilterIconFull
  [opts]
  [I opts i-filter-full])

(def i-filter "fal fa-filter")
(defn FilterIcon
  [opts]
  [I opts i-filter])

(def i-spell-check "fad fa-spell-check")
(defn SpellCheckIcon
  [opts]
  [I opts i-spell-check])

(def i-globe "fa-light fa-globe")
(defn GlobeIcon
  [opts]
  [I opts i-globe])

(def i-house "fa-light fa-house")
(defn HouseIcon
  [opts]
  [I opts i-house])

(def i-gauge "fa-light fa-gauge-min")
(defn GaugeIcon
  [opts]
  [I opts i-gauge])

(def i-code "fa-light fa-code")
(defn CodeIcon
  [opts]
  [I opts i-code])

(def i-bars "fa-light fa-bars")
(defn BarsIcon
  [opts]
  [I opts i-bars])

(def i-sign-in-alt "fad fa-sign-in-alt")
(defn SigninIcon
  [opts]
  [I opts i-sign-in-alt])

(def i-money-check-edit "fad fa-money-check-edit")
(defn MoneyCheckEditIcon
  [opts]
  [I opts i-money-check-edit])

(def i-credit-card "fal fa-credit-card")
(defn CreditCardIcon
  [opts]
  [I opts i-credit-card])

(def i-shopping-cart "fal fa-shopping-cart")
(defn ShoppingCartIcon
  [opts]
  [I opts i-shopping-cart])

(def i-file-invoice "fad fa-file-invoice")
(defn FileInvoiceIcon
  [opts]
  [I opts i-file-invoice])


(def i-file-invoice-dollar "fad fa-file-invoice-dollar")
(defn FileInvoiceDollarIcon
  [opts]
  [I opts i-file-invoice-dollar])

(def i-ticket "fad fa-ticket")
(defn TicketIcon
  [opts]
  [I opts i-ticket])

(def i-envelope-open-dollar "fad fa-envelope-open-dollar")
(defn EnvelopeOpenDollarIcon
  [opts]
  [I opts i-envelope-open-dollar])

(def i-envelop "fad fa-envelope")
(defn EnvelopeIcon
  [opts]
  [I opts i-envelop])

(def i-pencil "fal fa-pen")
(defn PencilIcon
  [opts]
  [I opts i-pencil])

(def i-columns "fa-columns")
(defn ColumnIcon
  [opts]
  [I opts i-columns])

(def i-hard-drive "fal fa-hard-drive")
(defn HardDriveIcon
  [opts]
  [I opts i-hard-drive])

(def i-circle-question "fal fa-circle-question")
(defn QuestionCircleIcon
  [opts]
  [I opts i-circle-question])

(def i-redo "fal fa-redo")
(defn RedoIcon
  [opts]
  [I opts i-redo])

(def i-play "fal fa-play")
(defn PlayIcon
  [opts]
  [I opts i-play])

(def i-stop "fal fa-stop")
(defn StopIcon
  [opts]
  [I opts i-stop])

(def i-stop-full "fas fa-stop")
(defn StopIconFull
  [opts]
  [I opts i-stop-full])

(def i-bolt "fal fa-bolt")
(defn BoltIcon
  [opts]
  [I opts i-bolt])


(def i-sliders "fal fa-sliders")
(defn SlidersIcon
  [opts]
  [I opts i-sliders])

(def i-clipboard "fal fa-clipboard")
(defn ClipboardIcon
  [opts]
  [I opts i-clipboard])

(def i-location-dot "fal fa-location-dot")
(defn LocationDotIcon
  [opts]
  [I opts i-location-dot])

(def i-usb-drive "fal fa-usb-drive")
(defn USBIcon
  [opts]
  [I opts i-usb-drive])

(def i-shield "fal fa-shield-check")
(defn ShieldIcon
  [opts]
  [I opts i-shield])

(def i-close "fal fa-close")
(defn CloseIcon
  [opts]
  [I opts i-close])

(def i-compress "fal fa-compress")
(defn CompressIcon
  [opts]
  [I opts i-compress])

(def i-expand "fal fa-expand")
(defn ExpandIcon
  [opts]
  [I opts i-expand])

(def i-world "fal world")
(defn WorldIcon
  [opts]
  [I opts i-world])

(def i-caret-down "caret down")
(defn CaretDownIcon
  [opts]
  [I opts i-caret-down])

(def i-caret-up "caret up")
(defn CaretUpIcon
  [opts]
  [I opts i-caret-up])

(def i-caret-left "caret left")
(defn CaretLeftIcon
  [opts]
  [I opts i-caret-left])

(def i-asterisk "asterisk")
(defn AsteriskIcon
  [opts]
  [I opts i-asterisk])

(def i-delete "delete")
(defn DeleteIcon
  [opts]
  [I opts i-delete])

(def i-text-file "file text")
(defn TextFileIcon
  [opts]
  [I opts i-text-file])

(def i-settings "settings")
(defn SettingsIcon
  [opts]
  [I opts i-settings])

(def i-expanding-arrows "expand arrows alternate")
(defn ExpandingArrowsIcon
  [opts]
  [I opts i-expanding-arrows])

(defn CloudDownloadIcon
  [opts]
  [I opts (str i-cloud "-download")])

(def i-circle-outline "circle outline")
(defn CircleOutlineIcon
  [opts]
  [I opts i-circle-outline])

(def i-question-circle-outline "question circle outline")
(defn QuestionCircleOutlineIcon
  [opts]
  [I opts i-question-circle-outline])

(def i-map "fa-map")
(defn MapIcon
  [opts]
  [I opts i-map])

(def i-plus-square-icon "plus square outline")
(defn PlusSquareIcon
  [opts]
  [I opts i-plus-square-icon])

(def i-square-outline "square outline")
(defn SquareOutlineIcon
  [opts]
  [I opts i-square-outline])

(def i-check-square-outline "check square outline")
(defn CheckSquareOutlineIcon
  [opts]
  [I opts i-check-square-outline])

(def i-sticky-note "fal sticky note")
(defn StickyNoteIcon
  [opts]
  [I opts i-sticky-note])

(def i-heartbeat "heartbeat")
(defn HeartbeatIcon
  [opts]
  [I opts i-heartbeat])

(def i-spinner "spinner")
(defn SpinnerIcon
  [opts]
  [I opts i-spinner])

(def i-lock "fal fa-lock")

(def i-sync "fal fa-sync")

(def i-edit "fal fa-edit")
(defn EditIcon
  [opts]
  [I opts i-edit])

(def i-undo "fal fa-undo")
(defn UndoIcon
  [opts]
  [I opts i-undo])

(def i-share "share square outline")
