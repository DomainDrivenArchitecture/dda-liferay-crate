; Licensed to the Apache Software Foundation (ASF) under one
; or more contributor license agreements. See the NOTICE file
; distributed with this work for additional information
; regarding copyright ownership. The ASF licenses this file
; to you under the Apache License, Version 2.0 (the
; "License"); you may not use this file except in compliance
; with the License. You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns org.domaindrivenarchitecture.pallet.crate.liferay.app-config-test
  (:require
    [clojure.test :refer :all]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [pallet.actions :as actions]
    [pallet.stevedore :as stevedore]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as sut]
    ))

(defn source-comment-re-str []
  (str "(?sm) *# " (.getName (io/file *file*)) ":\\d+\n?"))

;;; a test method that adds a check for source line comment
(defmethod assert-expr 'script= [msg form]
  (let [[_ expected expr] form]
    `(let [re# (re-pattern ~(source-comment-re-str))
           expected# (-> ~expected string/trim)
           actual# (-> ~expr (string/replace re# "") string/trim)]
       (if (= expected# actual#)
         (do-report
          {:type :pass :message ~msg :expected expected# :actual actual#})
         (do-report
          {:type :fail :message ~msg :expected expected# :actual actual#})))))


(def ^:private portal-ext-properties-with-all-keys
  ["#"
  "# Techbase"
  "#"
  "liferay.home=/var/lib/liferay"
  "setup.wizard.enabled=false"
  "index.on.startup=false"
  "#"
  "# MySQL"
  "#"
  "jdbc.default.driverClassName=com.mysql.jdbc.Driver"
  "jdbc.default.url=jdbc:mysql://localhost:3306/my-db-name?useUnicode=true&characterEncoding=UTF-8&useFastDateParsing=false"
  "jdbc.default.username=my_db_user_name"
  "jdbc.default.password=my_db_user_passwd"
  "#"
  "# C3PO" 
  "#"
  "#jdbc.default.acquireIncrement=2"
  "#jdbc.default.idleConnectionTestPeriod=60"
  "#jdbc.default.maxIdleTime=3600"
  "#jdbc.default.maxPoolSize=100"
  "#jdbc.default.minPoolSize=40"
  ""
  "#"
  "# Timeouts"
  "#"
  "com.liferay.util.Http.timeout=1000"
  "session.timeout=120"
  ""]
  )

(def ^:private portal-ext-properties-with-no-key
  ["#"
  "# Techbase"
  "#"
  "liferay.home=/var/lib/liferay"
  "setup.wizard.enabled=false"
  "index.on.startup=false"
  ""
  "#"
  "# Timeouts"
  "#"
  "com.liferay.util.Http.timeout=1000"
  "session.timeout=120"
  ""]
  )

(def ^:private portal-ext-properties-with-unsufficient-keys
  ["#"
  "# Techbase"
  "#"
  "liferay.home=/var/lib/liferay"
  "setup.wizard.enabled=false"
  "index.on.startup=false"
  ""
  "#"
  "# Timeouts"
  "#"
  "com.liferay.util.Http.timeout=1000"
  "session.timeout=120"
  ""]
  )

(deftest portal-ext-properties
  (testing 
    "test the good case"
    (is (= portal-ext-properties-with-all-keys
           (sut/var-lib-tomcat7-webapps-ROOT-WEB-INF-classes-portal-ext-properties 
             :db-name "my-db-name"
             :db-user-name "my_db_user_name" 
             :db-user-passwd "my_db_user_passwd")))
    (is (= portal-ext-properties-with-no-key
           (sut/var-lib-tomcat7-webapps-ROOT-WEB-INF-classes-portal-ext-properties )))
    (is (= portal-ext-properties-with-unsufficient-keys
           (sut/var-lib-tomcat7-webapps-ROOT-WEB-INF-classes-portal-ext-properties 
             :db-name "my-db-name")))
    )
  )

(deftest test-test
  (testing 
    "test the good case"
    (is (script= 
          "echo \"Available releases are:\""
          (stevedore/with-script-language :pallet.stevedore.bash/bash
            (stevedore/script 
              (println "\"Available releases are:\"")
  ))))))

(deftest do-deploy-script
  (testing 
    "test the good case"
    (is (script= 
          (string/join
            \newline
            ["if [ \"$#\" -eq 0 ]; then"
             "echo \"\"" 
             "echo \"Available Releases are:\""
             "find portal-release-instance/ -type d | sort | cut -f2 -d'/'"
             "echo \"\""
             "echo \"Please use the release you want to deploy as a parameter for this script\""
             "echo \"\""
             "exit 1"
             "fi"
             "if [ \"$#\" -ge 3 ]; then"
             "echo \"\"" 
             "echo \"Please specify 2 parameters only!\""
             "echo \"\""
             "exit 1"
             "fi"
             "if [ -d /var/lib/liferay/prepare-rollout/${1} ]; then"
             "if [ \"${2}\" == \"hot\" ]; then"
             "for part in app hooks layouts portlets themes; do"
             "cp /var/lib/liferay/prepare-rollout/${1}/${part}/* /var/lib/liferay/deploy/"
             "done"
             "chown tomcat7 /var/lib/liferay/deploy/*"
             "else"
             "service tomcat7 stop"
             "rm -rf /var/lib/tomcat7/webapps/*"
             "for part in app hooks layouts portlets themes; do"
             "cp /var/lib/liferay/prepare-rollout/${1}/${part}/* /var/lib/tomcat7/webapps/"
             "done"
             "chown tomcat7 /var/lib/tomcat7/webapps/*"
             "fi"
             "else"
             "echo \"\"" 
             "echo \"ERROR: Specified release does not exist or you don't have the permission for it! Please run again as root! For a list of the available releases, run this script without parameters in order to show the available releases!\" ;"
             "echo \"\""
             "fi"]) 
            (sut/deploy-script)))
  ))