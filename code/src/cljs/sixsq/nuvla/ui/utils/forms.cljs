(ns sixsq.nuvla.ui.utils.forms
  (:require [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


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
  [_form-id _text _read-only?]
  (fn [form-id text read-only?]
    ^{:key form-id}
    [ui/Segment {:attached "bottom"}
     [uix/EditorJson {:value        @text
                      :on-change    (fn
                                      [value]
                                      (reset! text value))
                      :summary-page read-only?}]]))
