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

(def subcommands #{"install" "setup"
                   "package" "add" "set-current"})

(defn parse-args [args]
  (->> args
       (split-when #(or (str/starts-with? % "-")
                        (contains? subcommands %)))
       (reduce (fn [acc [k & vs]]
                 (assoc acc k
                        (or vs
                            (when (contains? boolean-flags k)
                              true))))
               {})))

(defn main
  [& args]
  (when-let [subc (first args)]
    (let [parsed (parse-args args)]
      (case subc
        "install"
        (let [{:keys [:path :exit]}
              (impl/install (get parsed "install")
                            (boolean (get parsed "--force"))
                            (boolean (get parsed "--verbose"))
                            ;; not used anymore:
                            (boolean (or (get parsed "--global")
                                         (get parsed "-g"))))]
          (println path)
          exit)
        "setup"
        (let [parsed (parse-args (cons subc (rest args)))]
          (impl/setup (boolean (get parsed "--force"))))
        "package"
        (case (second args)
          "add"
          (impl/package-add (get parsed "add"))
          "set-current"
          (impl/package-set-current (get parsed "set-current")))
        ;; fallback:
        (impl/warn "Unknown command:" subc)))))

(defn -main [& args]
  (let [exit (apply main args)]
    (shutdown-agents)
    (System/exit (or exit 0))))
