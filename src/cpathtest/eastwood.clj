(ns cpathtest.eastwood
  (:import (java.io File))
  (:require [clojure.string :as str]
            [cpathtest.util :as util]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.file :as file]))


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


(defn clojure-files [dir-name-str]
;;  (:clojure-files
   (#'dir/find-files [dir-name-str] nil)
;;   )
  )


(defn fname-nss [dir-name-strs]
  (let [files-by-dir (into {} (for [dir-name-str dir-name-strs]
                                [dir-name-str (clojure-files dir-name-str)]))
        fd-by-dir (util/map-vals (fn [files]
                                   (#'file/files-and-deps files nil))
                                 files-by-dir)]
    (into
     {}
     (for [[dir fd] fd-by-dir,
           [f namespace] (:filemap fd)
           :let [dir-with-sep (str dir File/separator)
                 fname (util/remove-prefix (str f) dir-with-sep)
                 desired-ns (filename-to-ns fname)
                 desired-fname (ns-to-filename namespace)]]
       [fname {:dir dir, :namespace namespace,
               :recommended-fname desired-fname,
               :recommended-namespace desired-ns,
               :mismatch (not= fname desired-fname)}]))))
