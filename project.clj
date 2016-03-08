(defproject org.domaindrivenarchitecture/dda-liferay-crate "0.2.0-SNAPSHOT"
  :description "dda-liferay-crate"
  :url "https://www.domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :pallet {:source-paths ["src"]}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.palletops/pallet "0.8.11"]
                 [prismatic/schema "1.0.5"]
                 [metosin/schema-tools "0.7.0"]
                 [org.domaindrivenarchitecture/dda-basic-crate "0.3.0-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-collected-crate "0.3.0-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-backup-crate "0.2.2-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-tomcat-crate "0.1.1-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-mysql-crate "0.1.1-SNAPSHOT"]
                 [org.domaindrivenarchitecture/httpd "0.2.1-SNAPSHOT"]]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:dev
             {:dependencies
              [[com.palletops/pallet "0.8.10" :classifier "tests"]
               [org.clojure/test.check "0.9.0"]]
              :plugins
              [[com.palletops/pallet-lein "0.8.0-alpha.1"]]}
              :leiningen/reply
               {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.2"]]
                :exclusions [commons-logging]}}
  :local-repo-classpath true
  :classifiers {:tests {:source-paths ^:replace ["test"]
                        :resource-paths ^:replace []}})
