(defproject org.domaindrivenarchitecture/dda-liferay-crate "0.2.6"
  :description "dda-liferay-crate"
  :url "https://www.domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :pallet {:source-paths ["src"]}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [prismatic/schema "1.1.3"]
                 [com.palletops/pallet "0.8.12"]
                 [com.palletops/stevedore "0.8.0-beta.7"]
                 [org.domaindrivenarchitecture/dda-config-commons "0.1.7"]
                 [org.domaindrivenarchitecture/dda-pallet-commons "0.2.1"]
                 [org.domaindrivenarchitecture/dda-pallet "0.2.0"]
                 [org.domaindrivenarchitecture/dda-backup-crate "0.4.0"]
                 [org.domaindrivenarchitecture/dda-tomcat-crate "0.1.5"]
                 [org.domaindrivenarchitecture/dda-mysql-crate "0.1.4"]                 
                 [org.domaindrivenarchitecture/dda-httpd-crate "0.1.3"]]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:dev
             {:source-paths ["src" "integration"]
              :dependencies
              [[org.clojure/test.check "0.9.0"]               
               [org.clojure/tools.cli "0.3.5"]
               [com.palletops/stevedore "0.8.0-beta.7"]
               [org.domaindrivenarchitecture/dda-pallet-commons "0.2.1" :classifier "tests"]
               [com.palletops/pallet "0.8.12" :classifier "tests"]
               [org.domaindrivenarchitecture/pallet-aws "0.2.8-SNAPSHOT"]
               [org.slf4j/jcl-over-slf4j "1.7.21"]]
              :plugins
              [[lein-sub "0.3.0"]]}
              :leiningen/reply
               {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.21"]]
                :exclusions [commons-logging]}}
  :local-repo-classpath true
  :classifiers {:tests {:source-paths ^:replace ["test" "integration"]
                        :resource-paths ^:replace []}})
