(ns glam.impl.impl
  {:no-doc true}
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net URL HttpURLConnection]
           [java.nio.file Files]))

(set! *warn-on-reflection* true)

(def os {:os/name (System/getProperty "os.name")
         :os/arch (System/getProperty "os.arch")})

(defn warn [& strs]
  (binding [*out* *err*]
    (apply println strs)))

(defn match-artifacts [package]
  (let [artifacts (:package/artifacts package)]
    (filter (fn [{os-name :os/name
                  os-arch :os/arch}]
              (and (re-matches (re-pattern os-name) (:os/name os))
                   (re-matches (re-pattern os-arch) (:os/arch os))))
            artifacts)))

(defn unzip [^java.io.File zip-file ^java.io.File destination-dir executables verbose?]
  (when verbose? (warn "Unzipping" (.getPath zip-file) "to" (.getPath destination-dir)))
  (let [output-path (.toPath destination-dir)
        zip-file (io/file zip-file)
        _ (.mkdirs (io/file destination-dir))
        executables (set executables)]
    (with-open
      [fis (Files/newInputStream (.toPath zip-file) (into-array java.nio.file.OpenOption []))
       zis (java.util.zip.ZipInputStream. fis)]
      (loop []
        (let [entry (.getNextEntry zis)]
          (when entry
            (let [entry-name (.getName entry)
                  new-path (.resolve output-path entry-name)
                  resolved-path (.resolve output-path new-path)]
              (if (.isDirectory entry)
                (Files/createDirectories new-path (into-array []))
                (do (Files/copy ^java.io.InputStream zis
                                resolved-path
                                ^"[Ljava.nio.file.CopyOption;"
                                (into-array
                                 [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
                    (when (contains? executables entry-name)
                      (let [f (.toFile resolved-path)]
                        (when verbose? (warn "Making" (.getPath f) "executable."))
                        (.setExecutable f true))))))
            (recur)))))))

(defn download [source ^java.io.File dest verbose?]
  (when verbose? (warn "Downloading" source "to" (.getPath dest)))
  (let [source (URL. source)
        dest (io/file dest)
        conn ^HttpURLConnection (.openConnection ^URL source)]
    (.setInstanceFollowRedirects conn true)
    (.connect conn)
    (io/make-parents dest)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))
    (when verbose? (warn "Download complete."))))

(defn destination-dir
  ^java.io.File
  [{package-name :package/name
    package-version :package/version}]
  (io/file (System/getProperty "user.home")
           ".glam"
           "repository"
           (str package-name)
           package-version))

(defn find-package-descriptor [package]
  (if (not (map? package))
    (let [;; package (str/replace (str package) "/" ".")
          resource (str package ".glam.edn")]
      (if-let [f (io/resource resource)]
        (let [pkg (edn/read-string (slurp f))]
          pkg)
        (warn "Package" package "not found")))
    package))

(defn pkg-name [package]
  (:package/name package))

(def ^java.io.File glam-dir
  (io/file (System/getProperty "user.home")
           ".glam"))

(def ^java.io.File global-install-file
  (io/file glam-dir
           "installed.edn"))

(defn ensure-global-path-exists []
  (when-not (.exists global-install-file)
    (spit global-install-file "")))

(defn add-package-to-global [package]
  (ensure-global-path-exists)
  (let [installed (edn/read-string (slurp global-install-file))
        installed (assoc installed (:package/name package) (:package/version package))]
    (spit global-install-file installed)))

(defn install-package [package force? verbose? global?]
  (when-let [package (find-package-descriptor package)]
    (let [artifacts (match-artifacts package)
          dest-dir (destination-dir package)]
      (mapv (fn [artifact]
              (let [url (:artifact/url artifact)
                    file-name (last (str/split url #"/"))
                    dest-file (io/file dest-dir file-name)]
                (if (and (not force?) (.exists dest-file))
                  (when verbose?
                    (warn "Package" (pkg-name package) "already installed"))
                  (do (download url dest-file verbose?)
                      (unzip dest-file dest-dir
                             (:artifact/executables artifact)
                             verbose?)
                      (when global?
                        (add-package-to-global package))))
                (.getPath dest-dir))) artifacts)
      dest-dir)))

(def path-sep (System/getProperty "path.separator"))

(defn global-path []
  (ensure-global-path-exists)
  (let [installed (edn/read-string (slurp global-install-file))
        paths (reduce (fn [acc [k v]]
                        (conj acc (.getPath (io/file glam-dir
                                                     "repository"
                                                     (str k)
                                                     (str v)))))
                      []
                      installed)]
    (str/join path-sep paths)))

(defn path-with-pkgs [packages force? verbose? global?]
  (let [packages (keep find-package-descriptor packages)
        paths (mapv #(install-package % force? verbose? global?) packages)]
    (if global?
      (let [gp (global-path)
            gpf (io/file glam-dir "path")]
        (spit gpf gp)
        (when verbose?
          (warn "Wrote" (.getPath gpf)))
        gp)
      (str/join path-sep paths))))

(defn setup []
  (let [glam-sh-dest (io/file glam-dir "scripts" "glam.sh")]
    (io/make-parents glam-sh-dest)
    (spit glam-sh-dest (slurp (io/resource "borkdude/glam/scripts/glam.sh")))
    (warn "Include this in your .bashrc analog to finish setup:")
    (warn)
    (warn "source" "$HOME/.glam/scripts/glam.sh")))
