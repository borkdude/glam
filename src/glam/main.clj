(ns glam.main
  (:require [clojure.string :as str]
            [glam.impl.impl :as impl]))

(defn split-when
  "Like partition-by but splits collection only when `pred` returns
  a truthy value. E.g. `(split-when odd? [1 2 3 4 5]) => ((1 2) (3 4) (5))`"
  [pred coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [fst (first s)
           f (complement pred)
           run (cons fst (take-while #(f %) (next s)))]
       (cons run (split-when pred (lazy-seq (drop (count run) s))))))))

(def boolean-flags
  #{"--force" "--verbose" "--global" "-g"})

(defn parse-args [args]
  (->> args
       (split-when #(str/starts-with? % "-"))
       (reduce (fn [acc [k & vs]]
                 (assoc acc k
                        (or vs
                            (when (contains? boolean-flags k)
                              true))))
               {})))

(def subcommand
  {"install" "--install"
   "setup" "--setup"})

(defn -main [& args]
  (when-let [subc* (first args)]
    (let [subc (get subcommand subc* subc*)
          args (cons subc (rest args))
          parsed (parse-args args)]
      (case subc
        "--install"
        (println (impl/path-with-pkgs (get parsed "--install")
                                      (boolean (get parsed "--force"))
                                      (boolean (get parsed "--verbose"))
                                      (boolean (or (get parsed "--global")
                                                   (get parsed "-g")))))
        "--setup"
        (impl/setup)
        (impl/warn "Unknown command:" subc*))))
  (shutdown-agents))
