(ns cpathtest.util)


(defn map-vals [f m]
  (into (empty m)
        (for [[k v] m] [k (f v)])))


(defn remove-prefix
  "If string s starts with the string prefix, return s with that
  prefix removed.  Otherwise, return s."
  [^String s ^String prefix]
  (if (.startsWith s prefix)
    (subs s (count prefix))
    s))


(defn die [fmt-str & args]
  (apply printf fmt-str args)
  (flush)
  (System/exit 1))


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
