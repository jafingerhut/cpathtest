(ns cpathtest.create-files
  (:import (java.io File))
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cpathtest.util :as util]))


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
        (util/die ".mkdirs %s failed.  Aborting.\n"  dir)))
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
