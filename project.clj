(defproject org.domaindrivenarchitecture/dda-liferay-crate "0.1.0-SNAPSHOT"
  :description "dda-liferay-crate"
  :url "https://www.domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :pallet {:source-paths ["src"]}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.palletops/pallet "0.8.0-RC.11"]
                 [com.palletops/pallet "0.8.0-RC.11" :classifier "tests"]
                 [com.palletops/stevedore "0.8.0-beta.7"]
                 [ch.qos.logback/logback-classic "1.0.9"]
                 [org.domaindrivenarchitecture.org/dda-config-crate "0.2.0"]
                 [org.domaindrivenarchitecture/dda-basic-crate "0.1.2"]
                 [org.domaindrivenarchitecture/dda-collected-crate "0.1.0-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-backup-crate "0.2.1"]
                 [org.domaindrivenarchitecture/dda-tomcat-crate "0.1.0-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-mysql-crate "0.1.0-SNAPSHOT"]
                 [org.domaindrivenarchitecture/httpd "0.2.0"]]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:dev
             {:dependencies
              [[com.palletops/pallet "0.8.0-RC.11" :classifier "tests"]
               ]
              :plugins
              [[com.palletops/pallet-lein "0.8.0-alpha.1"]]}
              :leiningen/reply
               {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.2"]]
                :exclusions [commons-logging]}}
  :local-repo-classpath true
  :classifiers {:tests {:source-paths ^:replace ["test"]
                        :resource-paths ^:replace []}})
