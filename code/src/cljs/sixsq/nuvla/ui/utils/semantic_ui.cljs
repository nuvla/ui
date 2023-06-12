(ns sixsq.nuvla.ui.utils.semantic-ui
  "Mapping of names of Semantic UI components to the Soda Ash wrappers. This
   namespace has no real functionality; it just keeps Cursive from complaining
   about undefined symbols."
  (:require ["@uiw/react-codemirror" :as code-mirror]
            ["qrcode.react" :refer (QRCodeCanvas)]
            ["react-copy-to-clipboard" :as copy-to-clipboard]
            ["react-datepicker" :as date-picker]
            ["react-diff-viewer-continued" :as react-diff-viewer]
            ["react-markdown" :as react-markdown]
            ["semantic-ui-react" :as semantic]
            [reagent.core :as r]))


(def Accordion (r/adapt-react-class semantic/Accordion))
(def AccordionTitle (r/adapt-react-class semantic/AccordionTitle))
(def AccordionContent (r/adapt-react-class semantic/AccordionContent))

(def Breadcrumb (r/adapt-react-class semantic/Breadcrumb))
(def BreadcrumbDivider (r/adapt-react-class semantic/BreadcrumbDivider))
(def BreadcrumbSection (r/adapt-react-class semantic/BreadcrumbSection))

(def Button (r/adapt-react-class semantic/Button))
(def ButtonOr (r/adapt-react-class semantic/ButtonOr))
(def ButtonContent (r/adapt-react-class semantic/ButtonContent))
(def ButtonGroup (r/adapt-react-class semantic/ButtonGroup))
;(def ButtonOr (r/adapt-react-class semantic/ButtonOr))

(def Card (r/adapt-react-class semantic/Card))
(def CardContent (r/adapt-react-class semantic/CardContent))
(def CardDescription (r/adapt-react-class semantic/CardDescription))
(def CardGroup (r/adapt-react-class semantic/CardGroup))
(def CardHeader (r/adapt-react-class semantic/CardHeader))
(def CardMeta (r/adapt-react-class semantic/CardMeta))

(def Checkbox (r/adapt-react-class semantic/Checkbox))
;(def Comment (r/adapt-react-class semantic/Comment))
;(def CommentAvatar (r/adapt-react-class semantic/CommentAvatar))
;(def CommentContent (r/adapt-react-class semantic/CommentContent))
(def Container (r/adapt-react-class semantic/Container))

(def DatePicker (r/adapt-react-class date-picker/default))

(def Dimmer (r/adapt-react-class semantic/Dimmer))
(def DimmerDimmable (r/adapt-react-class semantic/DimmerDimmable))

(def Divider (r/adapt-react-class semantic/Divider))

(def Dropdown (r/adapt-react-class semantic/Dropdown))
(def DropdownDivider (r/adapt-react-class semantic/DropdownDivider))
(def DropdownHeader (r/adapt-react-class semantic/DropdownHeader))
(def DropdownItem (r/adapt-react-class semantic/DropdownItem))
(def DropdownMenu (r/adapt-react-class semantic/DropdownMenu))

;;(def Feed (r/adapt-react-class semantic/Feed))
;;(def FeedContent (r/adapt-react-class semantic/FeedContent))
;;(def FeedDate (r/adapt-react-class semantic/FeedDate))
;;(def FeedEvent (r/adapt-react-class semantic/FeedEvent))
;;(def FeedExtra (r/adapt-react-class semantic/FeedExtra))
;;(def FeedLabel (r/adapt-react-class semantic/FeedLabel))
;;(def FeedLike (r/adapt-react-class semantic/FeedLike))
;;(def FeedMeta (r/adapt-react-class semantic/FeedMeta))
;;(def FeedSummary (r/adapt-react-class semantic/FeedSummary))
;;(def FeedUser (r/adapt-react-class semantic/FeedUser))

(def Form (r/adapt-react-class semantic/Form))
;;(def FormButton (r/adapt-react-class semantic/FormButton))
(def FormCheckbox (r/adapt-react-class semantic/FormCheckbox))
(def FormDropdown (r/adapt-react-class semantic/FormDropdown))
(def FormField (r/adapt-react-class semantic/FormField))
(def FormGroup (r/adapt-react-class semantic/FormGroup))
(def FormInput (r/adapt-react-class semantic/FormInput))
(def FormRadio (r/adapt-react-class semantic/FormRadio))
(def FormSelect (r/adapt-react-class semantic/FormSelect))

(def Grid (r/adapt-react-class semantic/Grid))
(def GridColumn (r/adapt-react-class semantic/GridColumn))
(def GridRow (r/adapt-react-class semantic/GridRow))

(def Icon (r/adapt-react-class semantic/Icon))
(def IconGroup (r/adapt-react-class semantic/IconGroup))

(def Item (r/adapt-react-class semantic/Item))
(def ItemContent (r/adapt-react-class semantic/ItemContent))
;;(def ItemDescription (r/adapt-react-class semantic/ItemDescription))
;;(def ItemExtra (r/adapt-react-class semantic/ItemExtra))
;;(def ItemGroup (r/adapt-react-class semantic/ItemGroup))
;;(def ItemHeader (r/adapt-react-class semantic/ItemHeader))
;;(def ItemImage (r/adapt-react-class semantic/ItemImage))
;;(def ItemMeta (r/adapt-react-class semantic/ItemMeta))

(def Image (r/adapt-react-class semantic/Image))

(def Embed (r/adapt-react-class semantic/Embed))

(def Input (r/adapt-react-class semantic/Input))

(def Header (r/adapt-react-class semantic/Header))
;;(def HeaderContent (r/adapt-react-class semantic/HeaderContent))
(def HeaderSubheader (r/adapt-react-class semantic/HeaderSubheader))

(def Label (r/adapt-react-class semantic/Label))
(def LabelGroup (r/adapt-react-class semantic/LabelGroup))
(def LabelDetail (r/adapt-react-class semantic/LabelDetail))

(def ListSA (r/adapt-react-class semantic/List))
(def ListList (r/adapt-react-class semantic/ListList))
(def ListContent (r/adapt-react-class semantic/ListContent))
(def ListDescription (r/adapt-react-class semantic/ListDescription))
(def ListHeader (r/adapt-react-class semantic/ListHeader))
(def ListIcon (r/adapt-react-class semantic/ListIcon))
(def ListItem (r/adapt-react-class semantic/ListItem))

(def Loader (r/adapt-react-class semantic/Loader))

(def Menu (r/adapt-react-class semantic/Menu))
(def MenuItem (r/adapt-react-class semantic/MenuItem))
(def MenuMenu (r/adapt-react-class semantic/MenuMenu))

(def Message (r/adapt-react-class semantic/Message))
(def MessageHeader (r/adapt-react-class semantic/MessageHeader))
(def MessageContent (r/adapt-react-class semantic/MessageContent))
(def MessageList (r/adapt-react-class semantic/MessageList))
(def MessageItem (r/adapt-react-class semantic/MessageItem))

(def Modal (r/adapt-react-class semantic/Modal))
(def ModalActions (r/adapt-react-class semantic/ModalActions))
(def ModalContent (r/adapt-react-class semantic/ModalContent))
(def ModalDescription (r/adapt-react-class semantic/ModalDescription))
(def ModalHeader (r/adapt-react-class semantic/ModalHeader))

(def Pagination (r/adapt-react-class semantic/Pagination))

(def Popup (r/adapt-react-class semantic/Popup))
(def PopupHeader (r/adapt-react-class semantic/PopupHeader))
(def PopupContent (r/adapt-react-class semantic/PopupContent))
(def Progress (r/adapt-react-class semantic/Progress))

;;(def Rail (r/adapt-react-class semantic/Rail))
;;(def Ref (r/adapt-react-class semantic/Ref))

(def Radio (r/adapt-react-class semantic/Radio))
;(def Reveal (r/adapt-react-class semantic/Reveal))
;(def RevealContent (r/adapt-react-class semantic/RevealContent))

(def Segment (r/adapt-react-class semantic/Segment))
(def SegmentGroup (r/adapt-react-class semantic/SegmentGroup))

(def Sidebar (r/adapt-react-class semantic/Sidebar))
(def SidebarPushable (r/adapt-react-class semantic/SidebarPushable))
(def SidebarPusher (r/adapt-react-class semantic/SidebarPusher))

(def Statistic (r/adapt-react-class semantic/Statistic))
(def StatisticGroup (r/adapt-react-class semantic/StatisticGroup))
(def StatisticLabel (r/adapt-react-class semantic/StatisticLabel))
(def StatisticValue (r/adapt-react-class semantic/StatisticValue))

(def Step (r/adapt-react-class semantic/Step))
(def StepContent (r/adapt-react-class semantic/StepContent))
(def StepGroup (r/adapt-react-class semantic/StepGroup))
(def StepTitle (r/adapt-react-class semantic/StepTitle))

(def Sticky (r/adapt-react-class semantic/Sticky))

(def Tab (r/adapt-react-class semantic/Tab))
(def TabPane (r/adapt-react-class semantic/TabPane))

(def Table (r/adapt-react-class semantic/Table))
(def TableBody (r/adapt-react-class semantic/TableBody))
(def TableCell (r/adapt-react-class semantic/TableCell))
(def TableFooter (r/adapt-react-class semantic/TableFooter))
(def TableHeader (r/adapt-react-class semantic/TableHeader))
(def TableHeaderCell (r/adapt-react-class semantic/TableHeaderCell))
(def TableRow (r/adapt-react-class semantic/TableRow))

(def TextArea (r/adapt-react-class semantic/TextArea))

(def Transition (r/adapt-react-class semantic/Transition))

(def TransitionablePortal (r/adapt-react-class semantic/TransitionablePortal))

;;
;; markdown
;;

(def ReactMarkdown (r/adapt-react-class react-markdown))

;;
;; copy
;;

(def CopyToClipboard (r/adapt-react-class copy-to-clipboard/CopyToClipboard))


;;
;; code mirror
;;

(def CodeMirror (r/adapt-react-class code-mirror/default))

;;
;; Diff viewer
;;
(def DiffViewer (r/adapt-react-class react-diff-viewer/default))

;;
;; QR Code
;;
(def QRCode (r/adapt-react-class QRCodeCanvas))
