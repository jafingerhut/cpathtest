(ns cpathtest.core
  (:import (java.io File))
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.file :as file]))


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


(require '[eastwood.lint :as l])
(require '[clojure.java.io :as io])
(def opts {:enabled-linters [:no-ns-form-found]
           :cwd (.getCanonicalFile (io/file "."))})
(def warning-count (atom 0))
(def x (l/nss-in-dirs cpaths opts warning-count))
(pprint x)

)
