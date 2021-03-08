(ns sixsq.nuvla.ui.utils.forms
  (:require
    [re-frame.core :refer [subscribe]]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn on-return-key
  "Will execute the given no-arg function when the value of k is the
   value for the return key (13). Before executing the function it will
   blur the active element in the document, ignoring any errors."
  [f k]
  (when (and f (= (.-charCode k) 13))
    (try
      (some-> js/document .-activeElement .blur)
      (catch :default _ nil))
    (f)))


(defn descriptions->options [descriptions]
  (mapv (fn [{:keys [id label]}] {:value id, :text (or label id)}) descriptions))


(def dark-red "#9f3a38")

(defn validation-error-msg
  [message show?]
  [:span {:style {:color dark-red :display (if show? "inline-block" "none")}} message])


;;
;; public component
;;


(defn resource-editor
  [form-id text]
  (fn [form-id text]
    ^{:key form-id}
    [ui/Segment {:attached "bottom"}
     [ui/CodeMirror {:value     @text
                     :on-change (fn [editor data value]
                                  (reset! text value))
                     :options   {:mode                "application/json"
                                 :line-numbers        true
                                 :match-brackets      true
                                 :auto-close-brackets true
                                 :style-active-line   true
                                 :fold-gutter         true
                                 :gutters             ["CodeMirror-foldgutter"]}}]]))
