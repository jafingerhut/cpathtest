(ns cpathtest.core
  (:import (java.io File))
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.file :as file]))


(defn remove-prefix
  "If string s starts with the string prefix, return s with that
  prefix removed.  Otherwise, return s."
  [^String s ^String prefix]
  (if (.startsWith s prefix)
    (subs s (count prefix))
    s))


(defn map-vals [f m]
  (into (empty m)
        (for [[k v] m] [k (f v)])))


(defn filename-to-ns [fname]
  (-> fname
      (str/replace-first #"\.clj$" "")
      (str/replace "_" "-")
      (str/replace File/separator ".")
      symbol))


(defn ns-to-filename [namespace]
  (str (-> namespace
           str
           (str/replace "-" "_")
           (str/replace "." File/separator))
       ".clj"))


(defn canonical-filename
  "Returns the canonical file name for the given file name.  A
canonical file name is platform dependent, but is both absolute and
unique.  See the Java docs for getCanonicalPath for some more details,
and the examples below.

    http://docs.oracle.com/javase/7/docs/api/java/io/File.html#getCanonicalPath%28%29

Examples:

Context: A Linux or Mac OS X system, where the current working
directory is /Users/jafinger/clj/dolly

user=> (ns/canonical-filename \"README.md\")
\"/Users/jafinger/clj/dolly/README.md\"

user=> (ns/canonical-filename \"../../Documents/\")
\"/Users/jafinger/Documents\"

user=> (ns/canonical-filename \"../.././clj/../Documents/././\")
\"/Users/jafinger/Documents\"

Context: A Windows 7 system, where the current working directory is
C:\\Users\\jafinger\\clj\\dolly

user=> (ns/canonical-filename \"README.md\")
\"C:\\Users\\jafinger\\clj\\dolly\\README.md\"

user=> (ns/canonical-filename \"..\\..\\Documents\\\")
\"C:\\Users\\jafinger\\Documents\"

user=> (ns/canonical-filename \"..\\..\\.\\clj\\..\\Documents\\.\\.\\\")
\"C:\\Users\\jafinger\\Documents\""
  [fname]
  (let [^java.io.File f (if (instance? java.io.File fname)
                          fname
                          (java.io.File. ^String fname))]
    (.getCanonicalPath f)))


(defn die [fmt-str & args]
  (apply printf fmt-str args)
  (flush)
  (System/exit 1))


(defn int->bitvec [n num-bits]
  (vec (for [i (range (dec num-bits) -1 -1)]
         (if (bit-test n i)
           1
           0))))


(defn files-to-create-for-idx [idx fname-suffix-list classpath-dir-list]
  (let [n (count fname-suffix-list)
        m (count classpath-dir-list)
        bits (int->bitvec idx (* n m))
        grouped-bits (partition m bits)
        _ (println (format "n=%d m=%d bits=%s grouped-bits=%s"
                           n m bits (seq grouped-bits)))
        namespace (str/join "-"
                            (map (fn [suffix bits] (apply str suffix bits))
                                 fname-suffix-list
                                 grouped-bits))
        fname-suffixes (vec fname-suffix-list)
        classpath-dirs (vec classpath-dir-list)]
    (remove nil?
            (map-indexed
             (fn [i bit]
               (if (== bit 1)
                 (let [fname-suffix-idx (quot i m)
                       classpath-dir-idx (mod i m)]
                   {:directory (nth classpath-dirs classpath-dir-idx)
                    :fname-suffix (nth fname-suffixes fname-suffix-idx)
                    :namespace namespace})))
             bits))))


(defn ns-name->fname [ns-name-str]
  (str/replace ns-name-str "-" "_"))


(defn write-file [info]
  (let [{:keys [directory fname-suffix namespace]} info
        dirname (str directory File/separator "foo")
        fname (str dirname File/separator
                   (ns-name->fname namespace) "." fname-suffix)
        ^File dir (io/file dirname)]
    (when-not (.exists dir)
      (when-not (.mkdirs dir)
        (die ".mkdirs %s failed.  Aborting.\n"  dir)))
    (spit fname (format
                 "(ns foo.%s)

(defn bar []
  :redefd-later)


(defn bar []
  %s)
"
                 namespace info))))


;; Number of namespaces to be created is 2^(n*m), where n=# of
;; elements in fname-suffix-list, and m=# of elements in
;; classpath-dir-list.

(defn create-files [classpath-dir-list fname-suffix-list]
  (let [n (count fname-suffix-list)
        m (count classpath-dir-list)
        num-namespaces (bit-shift-left 1 (* n m))]
    (dotimes [i num-namespaces]
      (doseq [m (files-to-create-for-idx i fname-suffix-list
                                         classpath-dir-list)]
        (write-file m)))))



(defn clojure-files [dir-name-str]
;;  (:clojure-files
   (#'dir/find-files [dir-name-str])
;;   )
  )


(defn fname-nss [dir-name-strs]
  (let [files-by-dir (into {} (for [dir-name-str dir-name-strs]
                                [dir-name-str (clojure-files dir-name-str)]))
        fd-by-dir (map-vals (fn [files]
                              (#'file/files-and-deps files))
                            files-by-dir)]
    (into
     {}
     (for [[dir fd] fd-by-dir,
           [f namespace] (:filemap fd)
           :let [dir-with-sep (str dir File/separator)
                 fname (remove-prefix (str f) dir-with-sep)
                 desired-ns (filename-to-ns fname)
                 desired-fname (ns-to-filename namespace)]]
       [fname {:dir dir, :namespace namespace,
               :recommended-fname desired-fname,
               :recommended-namespace desired-ns,
               :mismatch (not= fname desired-fname)}]))))


(defn java-classpath []
  (let [cl (ClassLoader/getSystemClassLoader)
        urls (. cl getURLs)]
    (map (fn [url] (.getFile url)) urls)))


;; Examples:

;; user=> (get-resource "clojure/java/io.clj")
;; #object[java.net.URL 0xc8cefeb "jar:file:/Users/jafinger/.m2/repository/org/clojure/clojure/1.7.0/clojure-1.7.0.jar!/clojure/java/io.clj"]

;; user=> (get-resource "clojure/tools/namespace/file.clj")
;; #object[java.net.URL 0x65734645 "jar:file:/Users/jafinger/.m2/repository/org/clojure/tools.namespace/0.2.11/tools.namespace-0.2.11.jar!/clojure/tools/namespace/file.clj"]


(defn get-resource [name]
  (let [bl (clojure.lang.RT/baseLoader)]
    (clojure.lang.RT/getResource bl name)))


(comment

(use 'clojure.pprint)
(require '[cpathtest.core :as c])
(def cpaths (mapv c/canonical-filename [ "cp1" "cp2" ]))
(def suffs [ "clj" "cljc" ])   

;; Create files to test on
(c/create-files cpaths suffs)

;; Get file names and namespaces
(def x (c/fname-nss cpaths))

;; When you use tools.namespace version 0.2.11, with at least some
;; support for .cljc files, the value of x returned from the call
;; above includes both of these key/value pairs:

 "foo/clj10_cljc10.clj"
 {:dir "/Users/jafinger/clj/eastwood/cpathtest/cp1",
  :namespace foo.clj10-cljc10,
  :recommended-fname "foo/clj10_cljc10.clj",
  :recommended-namespace foo.clj10-cljc10,
  :mismatch false},

 "foo/clj10_cljc10.cljc"
 {:dir "/Users/jafinger/clj/eastwood/cpathtest/cp1",
  :namespace foo.clj10-cljc10,
  :recommended-fname "foo/clj10_cljc10.clj",
  :recommended-namespace foo.clj10-cljc10.cljc,
  :mismatch true},


(require '[eastwood.lint :as l])
(require '[clojure.java.io :as io])
(def opts {:enabled-linters [:no-ns-form-found]
           :cwd (.getCanonicalFile (io/file "."))})
(def warning-count (atom 0))
(def x (l/nss-in-dirs cpaths opts warning-count))
(pprint x)

)
