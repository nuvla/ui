(ns sixsq.nuvla.ui.utils.semver-test
  (:require [cljs.test :refer [are deftest is]]
            [sixsq.nuvla.ui.utils.semver :as t]))

(deftest valid?
  (are [result input]
    (= result (t/valid? input))
    true "1.0.0"
    true "2.3.7-pre-release"
    true "1.0.0-alpha1"
    true "1.4.7+build"
    true "5.1.7+build.2.fff415d"
    false ""))

(deftest parse
  (are [result input]
    (= result (t/parse input))
    (t/Version. 1 0 0 nil nil) "1.0.0"
    (t/Version. 2 3 7 "pre-release" nil) "2.3.7-pre-release"
    (t/Version. 1 4 7 nil "build") "1.4.7+build"
    (t/Version. 5 1 7 nil "build.2.fff415d") "5.1.7+build.2.fff415d"
    nil "foobar"
    nil nil))

(deftest older?
  (are [result args]
    (= result (t/older? (first args) (second args)))
    false [(t/Version. 1 0 0 nil nil)
           (t/Version. 1 0 0 nil nil)]
    false [(t/Version. 2 0 0 nil nil)
           (t/Version. 1 0 1 nil nil)]
    true [(t/Version. 1 0 0 nil nil)
          (t/Version. 1 0 1 nil nil)]
    true [(t/Version. 1 0 0 "pre-release" nil)
          (t/Version. 1 0 0 nil nil)]
    true [(t/Version. 1 0 0 "pre-release" "build.1")
          (t/Version. 1 0 0 nil nil)]
    false [(t/Version. 1 0 0 "pre-release" "build.2")
           (t/Version. 1 0 0 "pre-release" "build.1")]))

(deftest newer?
  (are [result args]
    (= result (t/newer? (first args) (second args)))
    false [(t/Version. 1 0 0 nil nil)
           (t/Version. 1 0 0 nil nil)]
    true [(t/Version. 2 0 0 nil nil)
          (t/Version. 1 0 1 nil nil)]
    false [(t/Version. 1 0 0 nil nil)
           (t/Version. 1 0 1 nil nil)]
    false [(t/Version. 1 0 0 "pre-release" nil)
           (t/Version. 1 0 0 nil nil)]
    false [(t/Version. 1 0 0 "pre-release" "build.1")
           (t/Version. 1 0 0 nil nil)]
    true [(t/Version. 1 0 0 "pre-release" "build.2")
          (t/Version. 1 0 0 "pre-release" "build.1")]))

(deftest equal?
  (are [result args]
    (= result (t/equal? (first args) (second args)))
    true [(t/Version. 1 0 0 nil nil)
          (t/Version. 1 0 0 nil nil)]
    false [(t/Version. 2 0 0 nil nil)
           (t/Version. 1 0 1 nil nil)]
    false [(t/Version. 1 0 0 "pre-release" "build.2")
           (t/Version. 1 0 0 "pre-release" "build.1")]
    true [(t/Version. 1 0 0 "pre-release" "build.2")
          (t/Version. 1 0 0 "pre-release" "build.2")]))

(deftest sort-versions
  (is (= [(t/Version. 5 1 7 nil "build.2.fff415d")
          (t/Version. 2 0 0 nil nil)
          (t/Version. 1 0 0 nil nil)
          (t/Version. 1 0 0 "pre-release" "build.2")
          (t/Version. 1 0 0 "pre-release" "build.1")]
         (t/sort-versions
           [(t/Version. 1 0 0 nil nil)
            (t/Version. 1 0 0 "pre-release" "build.1")
            (t/Version. 5 1 7 nil "build.2.fff415d")
            (t/Version. 2 0 0 nil nil)
            (t/Version. 1 0 0 "pre-release" "build.2")]))))
