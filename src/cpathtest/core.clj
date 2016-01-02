(ns cpathtest.core
  (:import (java.io File)
           (java.util.jar JarFile))
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.classpath :as classpath]
            [clojure.tools.namespace.find :as find]
            [cpathtest.util :as util]
            ))


(defn find-source-files
  "Searches a sequence of java.io.File objects (both directories and
  JAR files) in 'classpath' for platform source files.  Returns a
  sequence of maps, one map per platform source file, with these keys:

    :classpath-file - One of the java.io.File objects given as input.

    :filename - A platform source file name that was found inside of
                the directory, or JAR file, that is the value
                of :classpath-file.  It does not begin with a /
                character, and if the value of :classpath-file is a
                directory, it is a file name relative to that directory.

    :resource - The value of :filename without the extension,
                e.g. without a suffix like \".clj\" or \".cljc\"

    :extension - The suffix that, when appended to the value
                 of :resource, equals the value of :filename.

  Use with clojure.java.classpath to search Clojure's classpath.

  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  ([classpath]
   (find-source-files classpath find/clj))
  ([classpath platform]
   (let [suffixes (:extensions platform)
         ret-item (fn [file filename]
                    (let [[filename-sans-suffix suffix]
                          (util/separate-suffix filename suffixes)]
                      {:classpath-file file
                       :filename filename
                       :resource filename-sans-suffix
                       :extension suffix}))]
     (mapcat (fn [^File file]
               (cond (.isDirectory file)
                     (let [prefix (str file File/separator)]
                       (map #(ret-item file (util/remove-prefix (str %) prefix))
                            (find/find-sources-in-dir file platform)))
                          
                     (classpath/jar-file? file)
                     (map #(ret-item file %)
                          (find/sources-in-jar (JarFile. file) platform))
                          
                     :else []))
             classpath))))


(comment

(use 'clojure.pprint)
(require '[cpathtest.eastwood :as e])
(require '[cpathtest.util :as u])
(def cpaths (mapv u/canonical-filename [ "cp1" "cp2" ]))
(def suffs [ "clj" "cljc" ])   

;; Get file names and namespaces
(def x (e/fname-nss cpaths))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require '[clojure.java.classpath :as cp])
(def path (cp/system-classpath))
(pprint path)

(require '[clojure.java.io :as io])
(require '[clojure.tools.namespace.find :as find])

(def x (find/find-sources-in-dir (io/file "cp1")))
(pprint x)

(require '[cpathtest.util :as u])
(require '[cpathtest.core :as core])
(def clj17jar (io/file "/Users/jafinger/.m2/repository/org/clojure/clojure/1.7.0/clojure-1.7.0.jar"))

(def x2 (find/find-sources-in-dir clj17jar))
(def x3 (find/find-namespaces [clj17jar]))

(def cp1 [(io/file "cp1") (io/file "cp2") clj17jar])
(def sf1 (core/find-source-files cp1))
(def gsf1 (->> sf1 (group-by :resource) (u/filter-vals #(>= (count %) 2))))

(def cp2 (cp/system-classpath))
(def sf2 (core/find-source-files cp2))
(def gsf2 (->> sf2 (group-by :resource) (u/filter-vals #(>= (count %) 2))))


;; If I want something that can search for files in both directories
;; and JAR files, tools.namespace does not have that directly.  It
;; does have find-ns-decls and find-namespaces, but those call
;; read-file-ns-decl on each file, and return only the namespace name,
;; not the file names.

;; For Eastwood, I want something that can search all directories and
;; JAR files in the classpath (or a specified subset), and return all
;; '.clj' and '.cljc' files in them.

;; I want it to determine which of those files have ns forms in them
;; using parse/read-ns-decl in tools.namespace, perhaps via calling
;; the appropriate one of read-ns-decl-from-jarfile-entry and
;; find-ns-decls-in-dir.

;; Optionally warn about the ones that do not have ns forms in them.
;; However, whether they are warned about or not, we still need to
;; remember they exist when it comes to determining which files shadow
;; other files, below.

;;     Details about previous paragraph:

;;     I created an experiment to test this.

;;     File cp1/bar/x.clj and cp2/bar/x.clj, where cp2/bar/x.clj had a
;;     correct ns form in it, but cp1/bar/x.clj had no ns form at all.

;;     Classpath had "cp1" before "cp2".

;;     'lein eastwood' with Eastwood 0.2.3 correctly warned that
;;     cp1/bar/x.clj had no ns form in it.  The message said 'it would
;;     not be linted', but that was incorrect, because it was linted.
;;     It was linted because cp2/bar/x.clj caused the namespace bar.x
;;     to be in the list of namespaces to be linted, and later when
;;     Eastwood linted namespace bar.x, it found the file
;;     cp1/bar/x.clj since it was earlier in the classpath.

;;     Attempting to do (require 'bar.x) in a REPL caused the expected
;;     error that namespace 'bar.x' was not found after loading
;;     '/bar/x'.  This is expected because loading file cp1/bar/x.clj
;;     does not create the namespace bar.x as a side effect of loading
;;     it, and require checks whether the namespace exists after
;;     loading the file.

;;     Thus it is important to *continue* considering all file names,
;;     whether they contain ns forms or not, to determine which files
;;     are shadowed.

;; Among those that do have ns forms, give an error about all of them
;; that have mismatching file/entry names vs. their namespace name.
;; The warnings that suggest a file name should give all possible file
;; names, not only those ending with '.clj'.  If there are no such
;; mismatches, continue onwards.

;; Now among all files, determine if any of them 'shadow' another with
;; the same namespace, and if so, which one will be loaded by
;; Clojure's 'require' or 'use'.  Any shadowed files/entries should be
;; warned about, with the name of the file that will be require/use'd
;; instead.  The information needed is:

;; * An ordered list of classpath directories and/or JAR files, in the
;;   order used by require/use.

;; * All .clj and .cljc files, whether they have ns forms in them or
;;   not.


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; older stuff

(require '[eastwood.lint :as l])
(require '[clojure.java.io :as io])
(def opts {:enabled-linters [:no-ns-form-found]
           :cwd (.getCanonicalFile (io/file "."))})
(def warning-count (atom 0))
(def x (l/nss-in-dirs cpaths opts warning-count))
(pprint x)

)
