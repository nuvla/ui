(ns sixsq.slipstream.webui.utils.semantic-ui
  "Mapping of names of Semantic UI components to the Soda Ash wrappers. This
   namespace has no real functionality; it just keeps Cursive from complaining
   about undefined symbols."
  (:require
    ["codemirror/addon/edit/closebrackets"]
    ["codemirror/addon/edit/matchbrackets"]
    ["codemirror/addon/fold/brace-fold"]
    ["codemirror/addon/fold/foldcode"]
    ["codemirror/addon/fold/foldgutter"]
    ["codemirror/addon/fold/indent-fold"]
    ["codemirror/addon/selection/active-line"]
    ["codemirror/mode/clojure/clojure"]
    ["codemirror/mode/javascript/javascript"]
    ["codemirror/mode/python/python"]
    ["codemirror/mode/shell/shell"]
    ["react-codemirror2" :as code-mirror]
    ["react-copy-to-clipboard" :as copy-to-clipboard]
    ["react-datepicker" :as date-picker]
    ["react-markdown" :as react-markdown]
    ["semantic-ui-react" :as semantic]
    [reagent.core :as reagent]))


;;(def Accordion (reagent/adapt-react-class semantic/Accordion))
;;(def AccordionTitle (reagent/adapt-react-class semantic/AccordionTitle))
;;(def AccordionContent (reagent/adapt-react-class semantic/AccordionContent))

(def Breadcrumb (reagent/adapt-react-class semantic/Breadcrumb))
(def BreadcrumbDivider (reagent/adapt-react-class semantic/BreadcrumbDivider))
(def BreadcrumbSection (reagent/adapt-react-class semantic/BreadcrumbSection))

(def Button (reagent/adapt-react-class semantic/Button))
(def ButtonGroup (reagent/adapt-react-class semantic/ButtonGroup))

(def Card (reagent/adapt-react-class semantic/Card))
(def CardContent (reagent/adapt-react-class semantic/CardContent))
(def CardDescription (reagent/adapt-react-class semantic/CardDescription))
(def CardGroup (reagent/adapt-react-class semantic/CardGroup))
(def CardHeader (reagent/adapt-react-class semantic/CardHeader))
(def CardMeta (reagent/adapt-react-class semantic/CardMeta))

(def Checkbox (reagent/adapt-react-class semantic/Checkbox))

(def Confirm (reagent/adapt-react-class semantic/Confirm))

(def Container (reagent/adapt-react-class semantic/Container))

(def DatePicker (reagent/adapt-react-class date-picker/default))

(def Dimmer (reagent/adapt-react-class semantic/Dimmer))
(def DimmerDimmable (reagent/adapt-react-class semantic/DimmerDimmable))

(def Divider (reagent/adapt-react-class semantic/Divider))

(def Dropdown (reagent/adapt-react-class semantic/Dropdown))
(def DropdownDivider (reagent/adapt-react-class semantic/DropdownDivider))
(def DropdownItem (reagent/adapt-react-class semantic/DropdownItem))
(def DropdownMenu (reagent/adapt-react-class semantic/DropdownMenu))

;;(def Feed (reagent/adapt-react-class semantic/Feed))
;;(def FeedContent (reagent/adapt-react-class semantic/FeedContent))
;;(def FeedDate (reagent/adapt-react-class semantic/FeedDate))
;;(def FeedEvent (reagent/adapt-react-class semantic/FeedEvent))
;;(def FeedExtra (reagent/adapt-react-class semantic/FeedExtra))
;;(def FeedLabel (reagent/adapt-react-class semantic/FeedLabel))
;;(def FeedLike (reagent/adapt-react-class semantic/FeedLike))
;;(def FeedMeta (reagent/adapt-react-class semantic/FeedMeta))
;;(def FeedSummary (reagent/adapt-react-class semantic/FeedSummary))
;;(def FeedUser (reagent/adapt-react-class semantic/FeedUser))

(def Form (reagent/adapt-react-class semantic/Form))
;;(def FormButton (reagent/adapt-react-class semantic/FormButton))
(def FormDropdown (reagent/adapt-react-class semantic/FormDropdown))
(def FormField (reagent/adapt-react-class semantic/FormField))
(def FormGroup (reagent/adapt-react-class semantic/FormGroup))
(def FormInput (reagent/adapt-react-class semantic/FormInput))
(def FormSelect (reagent/adapt-react-class semantic/FormSelect))

(def Grid (reagent/adapt-react-class semantic/Grid))
;;(def GridColumn (reagent/adapt-react-class semantic/GridColumn))
;;(def GridRow (reagent/adapt-react-class semantic/GridRow))

(def Icon (reagent/adapt-react-class semantic/Icon))
(def IconGroup (reagent/adapt-react-class semantic/IconGroup))

;;(def Item (reagent/adapt-react-class semantic/Item))
;;(def ItemContent (reagent/adapt-react-class semantic/ItemContent))
;;(def ItemDescription (reagent/adapt-react-class semantic/ItemDescription))
;;(def ItemExtra (reagent/adapt-react-class semantic/ItemExtra))
;;(def ItemGroup (reagent/adapt-react-class semantic/ItemGroup))
;;(def ItemHeader (reagent/adapt-react-class semantic/ItemHeader))
;;(def ItemImage (reagent/adapt-react-class semantic/ItemImage))
;;(def ItemMeta (reagent/adapt-react-class semantic/ItemMeta))

(def Image (reagent/adapt-react-class semantic/Image))

(def Input (reagent/adapt-react-class semantic/Input))

(def Header (reagent/adapt-react-class semantic/Header))
;;(def HeaderContent (reagent/adapt-react-class semantic/HeaderContent))
(def HeaderSubheader (reagent/adapt-react-class semantic/HeaderSubheader))

(def Label (reagent/adapt-react-class semantic/Label))
(def LabelDetail (reagent/adapt-react-class semantic/LabelDetail))

(def ListSA (reagent/adapt-react-class semantic/List))
(def ListContent (reagent/adapt-react-class semantic/ListContent))
(def ListDescription (reagent/adapt-react-class semantic/ListDescription))
(def ListHeader (reagent/adapt-react-class semantic/ListHeader))
(def ListIcon (reagent/adapt-react-class semantic/ListIcon))
(def ListItem (reagent/adapt-react-class semantic/ListItem))

(def Loader (reagent/adapt-react-class semantic/Loader))

(def MenuRaw semantic/Menu)

(def Menu (reagent/adapt-react-class semantic/Menu))
(def MenuItem (reagent/adapt-react-class semantic/MenuItem))
(def MenuMenu (reagent/adapt-react-class semantic/MenuMenu))

(def Message (reagent/adapt-react-class semantic/Message))
(def MessageHeader (reagent/adapt-react-class semantic/MessageHeader))
(def MessageContent (reagent/adapt-react-class semantic/MessageContent))
;;(def MessageList (reagent/adapt-react-class semantic/MessageList))
;;(def MessageItem (reagent/adapt-react-class semantic/MessageItem))

(def Modal (reagent/adapt-react-class semantic/Modal))
(def ModalActions (reagent/adapt-react-class semantic/ModalActions))
(def ModalContent (reagent/adapt-react-class semantic/ModalContent))
(def ModalDescription (reagent/adapt-react-class semantic/ModalDescription))
(def ModalHeader (reagent/adapt-react-class semantic/ModalHeader))

(def Pagination (reagent/adapt-react-class semantic/Pagination))

(def Popup (reagent/adapt-react-class semantic/Popup))
(def PopupHeader (reagent/adapt-react-class semantic/PopupHeader))
(def PopupContent (reagent/adapt-react-class semantic/PopupContent))
(def Progress (reagent/adapt-react-class semantic/Progress))

;;(def Rail (reagent/adapt-react-class semantic/Rail))
;;(def Ref (reagent/adapt-react-class semantic/Ref))

(def Responsive (reagent/adapt-react-class semantic/Responsive))

(def SegmentRaw semantic/Segment)
(def Segment (reagent/adapt-react-class semantic/Segment))
;;(def SegmentGroup (reagent/adapt-react-class semantic/SegmentGroup))

(def Sidebar (reagent/adapt-react-class semantic/Sidebar))
(def SidebarPushable (reagent/adapt-react-class semantic/SidebarPushable))
(def SidebarPusher (reagent/adapt-react-class semantic/SidebarPusher))

(def Statistic (reagent/adapt-react-class semantic/Statistic))
(def StatisticGroup (reagent/adapt-react-class semantic/StatisticGroup))
(def StatisticLabel (reagent/adapt-react-class semantic/StatisticLabel))
(def StatisticValue (reagent/adapt-react-class semantic/StatisticValue))

(def Step (reagent/adapt-react-class semantic/Step))
(def StepContent (reagent/adapt-react-class semantic/StepContent))
(def StepGroup (reagent/adapt-react-class semantic/StepGroup))
(def StepTitle (reagent/adapt-react-class semantic/StepTitle))

(def Tab (reagent/adapt-react-class semantic/Tab))
(def TabPane (reagent/adapt-react-class semantic/TabPane))

(def Table (reagent/adapt-react-class semantic/Table))
(def TableBody (reagent/adapt-react-class semantic/TableBody))
(def TableCell (reagent/adapt-react-class semantic/TableCell))
(def TableFooter (reagent/adapt-react-class semantic/TableFooter))
(def TableHeader (reagent/adapt-react-class semantic/TableHeader))
(def TableHeaderCell (reagent/adapt-react-class semantic/TableHeaderCell))
(def TableRow (reagent/adapt-react-class semantic/TableRow))

(def TextArea (reagent/adapt-react-class semantic/TextArea))

(def Transition (reagent/adapt-react-class semantic/Transition))

(def TransitionablePortal (reagent/adapt-react-class semantic/TransitionablePortal))

;;
;; markdown
;;

(def ReactMarkdown (reagent/adapt-react-class react-markdown))

;;
;; copy
;;

(def CopyToClipboard (reagent/adapt-react-class copy-to-clipboard/CopyToClipboard))


;;
;; code mirror
;;
(def CodeMirror (reagent/adapt-react-class code-mirror/UnControlled))
(def CodeMirrorControlled (reagent/adapt-react-class code-mirror/Controlled))
