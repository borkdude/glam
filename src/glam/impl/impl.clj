(ns glam.impl.impl
  {:no-doc true}
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str])
  (:import [java.net URL HttpURLConnection]
           [java.nio.file Files]))

(set! *warn-on-reflection* true)

(defn normalize-arch [arch]
  (if (= "amd64" arch)
    "x86_64"
    arch))

(def os {:os/name (System/getProperty "os.name")
         :os/arch (let [arch (System/getProperty "os.arch")]
                    (normalize-arch arch))})

(defn warn [& strs]
  (binding [*out* *err*]
    (apply println strs)))

(defn match-artifacts [package]
  (let [artifacts (:package/artifacts package)]
    (filter (fn [{os-name :os/name
                  os-arch :os/arch}]
              (let [os-arch (normalize-arch os-arch)]
                (and (re-matches (re-pattern os-name) (:os/name os))
                     (re-matches (re-pattern os-arch)
                                 (:os/arch os)))))
            artifacts)))

(defn unzip [^java.io.File zip-file ^java.io.File destination-dir verbose?]
  (when verbose? (warn "Unzipping" (.getPath zip-file) "to" (.getPath destination-dir)))
  (let [output-path (.toPath destination-dir)
        zip-file (io/file zip-file)
        _ (.mkdirs (io/file destination-dir))]
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
                (Files/copy ^java.io.InputStream zis
                            resolved-path
                            ^"[Ljava.nio.file.CopyOption;"
                            (into-array
                             [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))
            (recur)))))))

(defn un-tgz [^java.io.File zip-file ^java.io.File destination-dir verbose?]
  (when verbose? (warn "Unzipping" (.getPath zip-file) "to" (.getPath destination-dir)))
  (let [tmp-file (java.io.File/createTempFile "glam" ".tar")
        output-path (.toPath tmp-file)]
    (with-open
      [fis (Files/newInputStream (.toPath zip-file) (into-array java.nio.file.OpenOption []))
       zis (java.util.zip.GZIPInputStream. fis)]
      (Files/copy ^java.io.InputStream zis
                  output-path
                  ^"[Ljava.nio.file.CopyOption;"
                  (into-array
                   [java.nio.file.StandardCopyOption/REPLACE_EXISTING])))
    (sh "tar" "xf" (.getPath tmp-file) "--directory" (.getPath destination-dir))
    (.delete tmp-file)))

(defn make-executable [dest-dir executables verbose?]
  (doseq [e executables]
    (let [f (io/file dest-dir e)]
      (when verbose? (warn "Making" (.getPath f) "executable."))
      (.setExecutable f true))))

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

(def glam-dir
  (delay (io/file (System/getProperty "user.home")
                  ".glam")))

(def global-install-file
  (delay (io/file @glam-dir
                  "installed.edn")))

(def cfg-dir
  (delay (let [config-dir (or (System/getenv "XDG_CONFIG_HOME")
                              (io/file (System/getProperty "user.home")
                                       ".config"))]
           (io/file config-dir "glam"))))

(def cfg-file
  (delay (io/file @cfg-dir "config.edn")))

(defn config []
  (edn/read-string (slurp @cfg-file)))

(defn repo-dir [repo-name]
  (io/file @glam-dir "packages" (str repo-name)))

(defn repo-dirs [cfg]
  (let [repos (:package/repos cfg)
        names (map :repo/name repos)]
    (map repo-dir names)))

(defn package-resource
  ([package-name]
   (package-resource package-name (config)))
  ([package-name cfg]
   (let [dirs (repo-dirs cfg)
         f (str package-name)]
     (some (fn [dir]
             (let [f (io/file dir f)]
               (when (.exists f)
                 f)))
           dirs))))

(defn find-package-descriptor [package]
  (if (not (map? package))
    (let [resource (str package ".glam.edn")]
      (if-let [f (package-resource resource)]
        (let [pkg (edn/read-string (slurp f))]
          pkg)
        ;; Template fallback
        (let [[package version] (str/split package #"@")]
          (if version
            (let [template-resource (str package ".glam.template.edn")]
              (if-let [f (package-resource template-resource)]
                (let [pkg-str (slurp f)
                      pkg-str (str/replace pkg-str "{{version}}" version)
                      pkg (edn/read-string pkg-str)]
                  (warn "Package" package "not found, attempting template fallback")
                  pkg)
                (warn "Package" package "not found")))
            (warn "Package" package "not found")))))
    package))

(defn pkg-name [package]
  (str (:package/name package) "@"
       (:package/version package)))

(defn ensure-global-path-exists []
  (when-not (.exists (io/file @global-install-file))
    (spit @global-install-file "")))

(defn add-package-to-global [package]
  (ensure-global-path-exists)
  (let [installed (edn/read-string (slurp @global-install-file))
        installed (assoc installed (:package/name package) (:package/version package))]
    (spit @global-install-file installed)))

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
                      (let [filename (.getName dest-file)]
                        (cond (str/ends-with? filename ".zip")
                              (unzip dest-file dest-dir
                                     verbose?)
                              (or (str/ends-with? filename ".tgz")
                                  (str/ends-with? filename ".tar.gz"))
                              (un-tgz dest-file dest-dir
                                      verbose?)))
                      (make-executable dest-dir (:artifact/executables artifact) verbose?)
                      (when global?
                        (add-package-to-global package))))
                (.getPath dest-dir))) artifacts)
      dest-dir)))

(def path-sep (System/getProperty "path.separator"))

(defn global-path []
  (ensure-global-path-exists)
  (let [installed (edn/read-string (slurp @global-install-file))
        paths (reduce (fn [acc [k v]]
                        (conj acc (.getPath (io/file @glam-dir
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
            gpf (io/file @glam-dir "path")]
        (spit gpf gp)
        (when verbose?
          (warn "Wrote" (.getPath gpf)))
        gp)
      (str/join path-sep paths))))

(defn pull-packages []
  (let [cfg (edn/read-string (slurp @cfg-file))]
    (doseq [{repo-name :repo/name
             git-url   :git/url} (:package/repos cfg)]
      (let [repo-dir (io/file (io/file (System/getProperty "user.home")
                                       ".glam" "packages")
                              (str repo-name))
            exists? (.exists repo-dir)]
        (if exists?
          (do
            (warn "Pulling" git-url "to" (str repo-dir))
            ;; TODO: error handling
            (sh "git" "-C" (.getPath repo-dir) "pull" git-url))
          (do
            (.mkdirs repo-dir)
            (warn "Cloning" git-url "to" (str repo-dir))
            (sh "git" "-C" (.getParent repo-dir) "clone" git-url
                (last (str/split (str repo-dir) #"/")))))))))

(def default-config
  '{:package/repos [{:repo/name glam/core
                     :git/url "https://github.com/glam-pm/packages"}]})

(defn setup [force?]
  (let [glam-sh-dest (io/file @glam-dir "scripts" "glam.sh")]
    (io/make-parents glam-sh-dest)
    (spit glam-sh-dest (slurp (io/resource "borkdude/glam/scripts/glam.sh")))
    (let [cfg-file (io/file @cfg-dir "config.edn")]
      (when (or (not (.exists cfg-file))
                force?)
        (io/make-parents cfg-file)
        (spit cfg-file default-config)))
    (pull-packages)
    (warn "Include this in your .bashrc analog to finish setup:")
    (warn)
    (warn "source" "$HOME/.glam/scripts/glam.sh")))

(defn sha256 [^String string]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn add-package [[package]]
  (let [[package version] (str/split package #"@")]
    (if version
      (let [template-resource (str package ".glam.template.edn")]
        (if-let [f (package-resource template-resource)]
          (let [f (io/file f)
                pkg-dir (-> f .getParentFile .getParentFile)
                pkg-str (slurp f)
                pkg-str (str/replace pkg-str "{{version}}" version)
                pkg (edn/read-string pkg-str)
                pkg-file (io/file pkg-dir (str package "@" version ".glam.edn"))]
            ;; TODO: add sha256
            (install-package pkg false true false)
            (spit pkg-file pkg-str)
            (warn "Package created at" (str pkg-file)))
          (warn "No template found")))
      (warn "Please specify version using @version"))))
