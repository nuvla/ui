(ns sixsq.slipstream.webui.i18n.utils
  (:require
    [sixsq.slipstream.webui.i18n.dictionary :refer [dictionary]]
    [taoensso.tempura :as tempura]))


(defn create-tr-fn
  "Returns a translation function from the dictionary and the provided
   locale-id, which can be either a string or keyword. The English locale (en)
   is always the fallback."
  [locale-id]
  (partial tempura/tr {:dict dictionary :default-locale :en} [(keyword locale-id)]))


(defn get-locale-label
  "Returns the localized name of the language if specified in the dictionary.
   Returns the locale abbreviation otherwise. The locale argument can be either
   a string or keyword."
  [locale]
  (or (get-in dictionary [(keyword locale) :lang])
      (name locale)))


(defn locale-choice
  "Returns a map with the :value key set to the locale abbreviation as a
   string and the :text key set to the locale label. Used to provide a choice
   in the i18n menu.  The argument can be a string or keyword."
  [locale]
  {:value (name locale)
   :text  (get-locale-label locale)})


(defn locale-choices
  "Provides a vector of available locale choices ordered alphabetically by the
   locale labels."
  []
  (->> dictionary
       keys
       (map locale-choice)
       (sort-by :text)
       vec))

