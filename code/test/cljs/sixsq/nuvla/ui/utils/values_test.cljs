(ns sixsq.nuvla.ui.utils.values-test
  (:require [cljs.test :refer [are deftest]]
            [sixsq.nuvla.ui.utils.values :refer [markdown->summary]]))


(deftest markdown->summary-test
  (are [result input]
    (= result
       (markdown->summary input))
    "simple paragraph" "simple paragraph"
    "simple paragraph with title" "#Title\nsimple paragraph with title"
    "simple paragraph with title after line break second line break" "#Title\nsimple paragraph with title\nafter line break\nsecond line break\n\nanother paragraph"
    "Emphasis start paragraph" "##Title\n\n*Emphasis start* paragraph"
    "Bold start paragraph" "##Title\n\n**Bold start** paragraph"
    "Paragraph with link" "##Title\n\nParagraph with [link](http://example.com)"))
