(ns cpm.main
  (:require [clojure.string :as str]
            [cpm.impl.impl :as impl]))

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

(defn parse-args [args]
  (->> args
       (split-when #(str/starts-with? % "-"))
       (reduce (fn [acc [k & vs]]
                 (assoc acc k (or vs
                                  true)))
               {})))

(defn -main [& args]
  (let [parsed (parse-args args)]
    (println (impl/path-with-pkgs (get parsed "--install")
                                  (boolean (get parsed "--force"))
                                  (boolean (get parsed "--verbose"))
                                  (boolean (get parsed "--global"))))))
