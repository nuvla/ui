(ns sixsq.nuvla.ui.pages.apps.utils-test
  (:require [cljs.test :refer [deftest is testing]]
            [sixsq.nuvla.ui.pages.apps.utils :as utils]))

(deftest public-mec-content-test
  (testing "package artifact metadata is excluded from the editable AppD payload"
    (is (= {:appName "My MEC App"}
           (utils/public-mec-content
             {:appName "My MEC App"
              :packageContentData "UEsDB"
              :packageContentFilename "app.csar.zip"})))))

(deftest mec-package-source-test
  (testing "package source defaults to manual AppD without stored artifact"
    (is (= utils/mec-source-appd
           (utils/mec-package-source {:content {:appName "Manual"}}))))
  (testing "package source switches to CSAR when a ZIP artifact is already stored"
    (is (= utils/mec-source-csar
           (utils/mec-package-source {:content {:packageContentData "UEsDB"}})))))

(deftest mec-package-input-valid-test
  (testing "manual AppD mode requires valid JSON"
    (is (true? (utils/mec-package-input-valid? utils/mec-source-appd nil false "{\"appName\":\"ok\"}")))
    (is (false? (utils/mec-package-input-valid? utils/mec-source-appd nil false "{broken"))))
  (testing "CSAR mode accepts either a selected ZIP or an already stored artifact"
    (is (true? (utils/mec-package-input-valid? utils/mec-source-csar #js {:name "demo.zip"} false nil)))
    (is (true? (utils/mec-package-input-valid? utils/mec-source-csar nil true nil)))
    (is (false? (utils/mec-package-input-valid? utils/mec-source-csar nil false nil)))))
