(ns cpm.api
  (:require [cpm.impl.impl :as impl]))

(defn resolve-pkg [{:keys [package force verbose]}]
  (impl/install-package package force verbose))
