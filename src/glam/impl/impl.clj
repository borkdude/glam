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

(defn unzip [{:keys [^java.io.File zip-file
                     ^java.io.File destination-dir
                     verbose]}]
  (when verbose (warn "Unzipping" (.getPath zip-file) "to" (.getPath destination-dir)))
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
                  new-path (.resolve output-path entry-name)]
              (if (.isDirectory entry)
                (Files/createDirectories new-path (into-array []))
                (Files/copy ^java.io.InputStream zis
                            new-path
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

(def glam-dir
  (delay (io/file (System/getProperty "user.home")
                  ".glam")))

(def cfg-dir
  (delay (let [config-dir (or (System/getenv "XDG_CONFIG_HOME")
                              (io/file (System/getProperty "user.home")
                                       ".config"))]
           (io/file config-dir "glam"))))

(def cfg-file
  (delay (io/file @cfg-dir "glam.edn")))

(defn repo-config []
  (:glam/repos (edn/read-string (slurp @cfg-file))))

(defn repo-dir [repo-name]
  (io/file @glam-dir "packages" (str repo-name)))

(defn repo-dirs [repo-cfg]
  (let [names (map :repo/name repo-cfg)]
    (map repo-dir names)))

(defn package-resource
  ([package-name]
   (package-resource package-name (repo-config)))
  ([package-name repo-cfg]
   (let [dirs (repo-dirs repo-cfg)
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

(defn cache-dir
  ^java.io.File
  [{package-name :package/name
    package-version :package/version}]
  (io/file (or
            (System/getenv "XDG_CACHE_HOME")
            (System/getProperty "user.home"))
           ".glam"
           "repository"
           (str package-name)
           package-version))

(defn data-dir
  ^java.io.File
  [{package-name :package/name
    package-version :package/version}]
  (io/file (or
            (System/getenv "XDG_DATA_HOME")
            (System/getProperty "user.home"))
           ".glam"
           "repository"
           (str package-name)
           package-version))

(defn sha256 [file]
  (let [buf (byte-array 8192)
        digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [bis (io/input-stream (io/file file))]
      (loop []
        (let [count (.read bis buf)]
          (when (pos? count)
            (.update digest buf 0 count)
            (recur)))))
    (-> (.encode (java.util.Base64/getEncoder)
                 (.digest digest))
        (String. "UTF-8"))))

(defn install-package [package force? verbose? _global?]
  (when-let [package (find-package-descriptor package)]
    (let [artifacts (match-artifacts package)
          cdir (cache-dir package)
          ddir (data-dir package)]
      (mapv (fn [artifact]
              (let [url (:artifact/url artifact)
                    file-name (last (str/split url #"/"))
                    cache-file (io/file cdir file-name)]
                (if (and (not force?) (.exists cdir))
                  (when verbose?
                    (warn "Package" (pkg-name package) "already installed"))
                  (do (download url cache-file verbose?)
                      (when-let [expected-sha (:artifact/hash artifact)]
                        (let [sha (sha256 cache-file)]
                          (when-not (= (str/replace expected-sha #"^sha256:" "")
                                       sha)
                            (throw (ex-info (str "Wrong SHA-256 for file" (str cache-file))
                                            {:sha sha
                                             :expected-sha expected-sha})))))
                      (let [filename (.getName cache-file)]
                        (cond (str/ends-with? filename ".zip")
                              (unzip {:zip-file cache-file
                                      :destination-dir ddir
                                      :verbose verbose?})
                              (or (str/ends-with? filename ".tgz")
                                  (str/ends-with? filename ".tar.gz"))
                              (un-tgz cache-file ddir
                                      verbose?)))
                      (make-executable ddir (:artifact/executables artifact) verbose?)))
                (.getPath ddir))) artifacts)
      ddir)))

(def path-sep (System/getProperty "path.separator"))


(defn project-packages []
  (let [glam-edn (io/file "glam.edn")]
    (when (.exists glam-edn)
      (let [edn (edn/read-string (slurp glam-edn))
            deps (:glam/deps edn)
            deps (mapv (fn [[k v]]
                         (str k "@" v))
                       deps)]
        deps))))

(defn global-packages []
  (let [glam-edn (io/file @cfg-dir "glam.edn")]
    (when (.exists glam-edn)
      (let [edn (edn/read-string (slurp glam-edn))
            deps (:glam/deps edn)
            deps (mapv (fn [[k v]]
                         (str k (when-not (identical? v :latest)
                                  (str "@" v))))
                       deps)]
        deps))))

(defn install [packages force? verbose? global?]
  (let [global-pkgs (global-packages)
        installed-glb (keep find-package-descriptor global-pkgs)
        installed-glb (keep #(install-package % force? verbose? global?) installed-glb)
        global-path (str/join path-sep installed-glb)
        project-pkgs (project-packages)
        installed-proj (keep find-package-descriptor project-pkgs)
        installed-proj (keep #(install-package % force? verbose? global?) installed-proj)
        proj-path (str/join path-sep installed-proj)
        pkgs (keep find-package-descriptor packages)
        pkgs (keep #(install-package % force? verbose? global?) pkgs)
        path (str/join path-sep pkgs)
        global-path-file (io/file @glam-dir "path")]
    (spit global-path-file global-path)
    (when verbose?
      (warn "Wrote" (.getPath global-path-file)))
    (when (.exists (io/file "glam.edn"))
      (let [path-file (io/file ".glam" "path")]
        (io/make-parents path-file)
        (spit path-file proj-path)
        (when verbose?
          (warn "Wrote" (.getPath path-file)))))
    {:path path
     :exit (if (= (+ (count global-pkgs) (count project-pkgs) (count packages))
                  (+ (count installed-glb) (count installed-proj) (count pkgs)))
             0
             1)}))

(defn pull-packages []
  (let [cfg (repo-config)]
    (doseq [{repo-name :repo/name
             git-url   :git/url} cfg]
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

(defn setup [force?]
  (let [glam-sh-dest (io/file @glam-dir "scripts" "glam.sh")]
    (io/make-parents glam-sh-dest)
    (spit glam-sh-dest (slurp (io/resource "glam/scripts/glam.sh")))
    (let [^java.io.File cfg-file @cfg-file]
      (when (or (not (.exists cfg-file))
                force?)
        (io/make-parents cfg-file)
        (spit cfg-file (slurp (io/resource "glam/glam.edn")))))
    (pull-packages)
    (warn "Include this in your .bashrc analog to finish setup:")
    (warn)
    (warn "source" "$HOME/.glam/scripts/glam.sh")))

;;;; Package creation

(defn artifact-sha [artifact]
  (when-let [k (:artifact/hash artifact)]
    (let [url (:artifact/url artifact)
          tmp-file (java.io.File/createTempFile "glam" "glam")
          _ (download url tmp-file true)
          sha (sha256 tmp-file)
          _ (.delete tmp-file)]
      [k sha])))

(defn calculate-hashes [pkg]
  (map artifact-sha (:package/artifacts pkg)))

(defn package-add [[package]]
  (let [[package version] (str/split package #"@")]
    (if version
      (let [template-resource (str package ".glam.template.edn")
            f (io/file template-resource)]
        (if (.exists f)
          (let [pkg-dir (-> f .getParentFile .getParentFile)
                pkg-str (slurp f)
                pkg-str (str/replace pkg-str "{{version}}" version)
                pkg-edn (edn/read-string pkg-str)
                replacements (calculate-hashes pkg-edn)
                pkg-str (reduce (fn [acc [k v]]
                                  (str/replace acc k (str "sha256:" v)))
                                pkg-str
                                replacements)
                pkg-file (io/file pkg-dir (str package "@" version ".glam.edn"))]
            (spit pkg-file pkg-str)
            (warn "Package created at" (str pkg-file)))
          (warn "No template found")))
      (warn "Please specify version using @version"))))

(defn package-set-current [[package-with-version]]
  (let [[package version] (str/split package-with-version #"@")]
    (if version
      (let [resource (str package-with-version ".glam.edn")
            f (io/file resource)]
        (if (.exists f)
          (let [pkg-dir (-> f .getParentFile .getParentFile)
                pkg-str (slurp f)
                pkg-file (io/file pkg-dir (str package ".glam.edn"))]
            (spit pkg-file pkg-str)
            (warn "Package created at" (str pkg-file)))
          (warn "Package not found:" package-with-version)))
      (warn "Please specify version using @version"))))
