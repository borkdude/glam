(ns glam.main-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :as t :refer [deftest is #_testing]]
            [glam.main :as main]))

(deftest setup-test
  (with-out-str
    (binding [*err* *out*] (main/main "setup"))))

(deftest install-test
  (let [output (with-out-str (main/main "install" "org.babashka/babashka@0.2.2" #_"--verbose"))
        cache-dir (io/file "test-dir" ".cache" ".glam" "repository" "org.babashka" "babashka" "0.2.2")
        data-dir (io/file "test-dir" ".data" ".glam" "repository" "org.babashka" "babashka" "0.2.2")
        bb-executable (io/file data-dir "bb")]
    (is (.exists cache-dir) (str cache-dir " doesn't exist"))
    (is (.exists data-dir) (str data-dir " doesn't exist"))
    (is (.exists bb-executable))
    (is (.canExecute bb-executable))
    (is (str/includes? output (.getPath data-dir)))))

(defn test-ns-hook []
  (setup-test)
  (install-test))
