(defproject org.domaindrivenarchitecture/dda-liferay-crate "0.2.2-SNAPSHOT"
  :description "dda-liferay-crate"
  :url "https://www.domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :pallet {:source-paths ["src"]}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [prismatic/schema "1.1.1"]
                 [metosin/schema-tools "0.9.0"]
                 [com.palletops/pallet "0.8.12"]
                 [com.palletops/stevedore "0.8.0-beta.7"]
                 [com.palletops/git-crate "0.8.0-alpha.2" :exclusions [org.clojure/clojure]]
                 [org.domaindrivenarchitecture/dda-config-commons "0.1.2-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-config-crate "0.3.4-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-basic-crate "0.3.3-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-collected-crate "0.3.3-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-backup-crate "0.3.3-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-tomcat-crate "0.1.4-SNAPSHOT"]
                 [org.domaindrivenarchitecture/dda-mysql-crate "0.1.3-SNAPSHOT"]
                 [org.domaindrivenarchitecture/httpd "0.2.2"]]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:dev
             {:dependencies
              [[org.clojure/test.check "0.9.0"]
               [com.palletops/pallet "0.8.12" :classifier "tests"]
               [org.domaindrivenarchitecture/dda-pallet-commons "0.1.3-SNAPSHOT" :classifier "tests"]]
              :plugins
              [[com.palletops/pallet-lein "0.8.0-alpha.1"]]}
              :leiningen/reply
               {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.21"]]
                :exclusions [commons-logging]}}
  :local-repo-classpath true
  :classifiers {:tests {:source-paths ^:replace ["test"]
                        :resource-paths ^:replace []}})
