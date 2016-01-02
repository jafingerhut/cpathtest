(defproject cpathtest "0.1.0-SNAPSHOT"
  :description "Test code related to classpaths and finding Clojure files in them, both .clj and .cljc"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles {:dev {:dependencies [;;[jonase/eastwood "0.2.3"]
                                  ;;[org.clojure/tools.namespace "0.2.9"]
                                  ;;[org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.namespace "0.3.0-alpha2"]
                                  ;;[org.clojure/tools.namespace "0.3.0-SNAPSHOT"]
                                  [org.clojure/java.classpath "0.2.3"]
                                  ]}}
  :test-paths [ "cp1" "cp2" ]
  )
