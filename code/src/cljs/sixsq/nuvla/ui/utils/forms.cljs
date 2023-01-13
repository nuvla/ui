(ns sixsq.nuvla.ui.utils.forms
  (:require [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn on-key
  "Will execute the given no-arg function when the value of k is the
   value for the trigger-key. Before executing the function it will
   blur the active element in the document, ignoring any errors."
  [trigger-key f k]
  (when (and f (= k trigger-key))
    (try
      (some-> js/document .-activeElement .blur)
      (catch :default _ nil))
    (f)))


(defn on-key-code
  "Useful with :on-key-down event"
  [trigger-key f k]
  (on-key trigger-key f (.-keyCode k)))


(defn on-char-code
  "Useful with :on-key-press event"
  [trigger-key f k]
  (on-key trigger-key f (.-charCode k)))


(defn on-return-key
  [f k]
  (on-char-code 13 f k))


(defn on-escape-key
  [f k]
  (on-key-code 27 f k))


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
  [_form-id _text]
  (fn [form-id text]
    ^{:key form-id}
    [ui/Segment {:attached "bottom"}
     [ui/CodeMirror {:value     @text
                     :on-change (fn
                                  [_editor _data value]
                                  (reset! text value))
                     :options   {:mode                "application/json"
                                 :line-numbers        true
                                 :match-brackets      true
                                 :auto-close-brackets true
                                 :style-active-line   true
                                 :fold-gutter         true
                                 :gutters             ["CodeMirror-foldgutter"]}}]]))
