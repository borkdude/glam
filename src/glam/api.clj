(ns glam.api
  (:require [glam.impl.impl :as impl]))

(defn resolve-pkg [{:keys [package force verbose]}]
  (impl/install-package package force verbose false))
