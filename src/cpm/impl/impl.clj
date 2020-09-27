(ns cpm.impl.impl
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
            (let [new-path (.resolve output-path (.getName entry))]
              (if (.isDirectory entry)
                (Files/createDirectories new-path (into-array []))
                (do (when-not (Files/exists (.getParent new-path) (into-array java.nio.file.LinkOption []))
                      (Files/createDirectories (.getParent new-path) (into-array [])))
                    (with-open [bos (Files/newOutputStream (.resolve output-path new-path)
                                                           (into-array java.nio.file.OpenOption []))]
                      (let [buf (byte-array (Math/toIntExact (.getSize entry)))]
                        (loop []
                          (let [loc (.read zis buf)]
                            (when-not (= -1 loc)
                              (.write bos buf 0 loc)
                              (recur)))))))))
            (recur)))))))

(defn download [source ^java.io.File dest verbose?]
  (when verbose? (warn "Downloading" source "to" (.getPath dest)))
  (let [source (URL. source)
        dest (io/file dest)
        conn ^HttpURLConnection (.openConnection ^URL source)]
    (.setInstanceFollowRedirects conn true)
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(defn destination-dir
  ^java.io.File
  [{package-name :package/name
    package-version :package/version}]
  (io/file (System/getProperty "user.home")
           ".cpm"
           "packages"
           (str/replace package-name
                        #"\." "/")
           package-version))

(defn find-package-descriptor [package]
  (if (not (map? package))
    (when-let [f (io/resource (str package ".cpm.edn"))]
      (let [pkg (edn/read-string (slurp f))]
        pkg))
    package))

(defn install-package [package force? verbose?]
  (when-let [package (find-package-descriptor package)]
    (let [artifacts (match-artifacts package)]
      (mapv (fn [artifact]
              (let [dest-dir (destination-dir package)
                    url (:artifact/url artifact)
                    file-name (last (str/split url #"/"))
                    dest-file (io/file dest-dir file-name)]
                (when (or force? (not (.exists dest-file)))
                  (download url dest-file verbose?)
                  (unzip dest-file dest-dir verbose?))
                (.getPath dest-dir))) artifacts))))

(def path-sep (System/getProperty "path.separator"))

(defn path-with-pkgs [packages force? verbose?]
  (let [packages (keep find-package-descriptor packages)
        paths (doall (mapcat #(install-package % force? verbose?) packages))]
    (str/join path-sep paths)))
