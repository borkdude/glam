(ns glam.main-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :as t :refer [deftest is testing]]
            [glam.main :as main]))

(deftest setup-test
  (with-out-str
    (binding [*err* *out*] (main/main "setup"))))

(defmacro suppress-output [& body]
  `(let [sw# (java.io.StringWriter.)]
     (binding [*out* sw#
               *err* sw#]
       ~@body)))

(deftest install-test
  (let [output (with-out-str (main/main "install" "org.babashka/babashka@0.2.2"))
        cache-dir (io/file "test-dir" ".cache" ".glam" "repository" "org.babashka" "babashka" "0.2.2")
        data-dir (io/file "test-dir" ".data" ".glam" "repository" "org.babashka" "babashka" "0.2.2")
        bb-executable (io/file data-dir "bb")]
    (is (.exists cache-dir) (str cache-dir " doesn't exist"))
    (is (.exists data-dir) (str data-dir " doesn't exist"))
    (is (.exists bb-executable))
    (is (.canExecute bb-executable))
    (is (str/includes? output (.getPath data-dir)))
    (testing "exit code is zero on install success"
      (let [exit (suppress-output (main/main "install" "org.babashka/babashka@0.2.2"))]
        (is (zero? exit))))
    (testing "exit code is positive on install failure"
      (let [exit (suppress-output (main/main "install" "org.foo/bar"))]
        (is (pos? exit))))))

(deftest install-global-test
  ;; TODO: global install test
  ;; Check: if installed.edn and path file are correctly set
  ;; Btw, we probably also have to update our bash script to account for XDG_CONFIG_HOME and XDG_DATA_HOME
  )

(deftest config-test
  ;; TODO: repositories.edn
  )

(defn test-ns-hook []
  (setup-test)
  (install-test))
