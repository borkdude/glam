(ns cpm.main
  (:require [cpm.impl.impl :as impl]))

(defn -main [& args]
  (println (impl/path-with-pkgs args false false)))
