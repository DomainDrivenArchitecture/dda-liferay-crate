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
  "setup.wizard.enabled=true"
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
             {:db {:db-name "my-db-name"
                   :user-name "my_db_user_name" 
                   :user-passwd "my_db_user_passwd"}})))
    )
  )

(deftest do-deploy-script
  (testing 
    "test the good case"
    (is (script= 
          (string/join
            \newline
            ["if [ \"$#\" -eq 0 ]; then"
             "echo \"\"" 
             "echo \"Usage is: prepare-rollout [release] [deployment-mode].\""
             "echo \"  deployment-mode:      [hot|full] hot uses the liferay hot deployment mechanism for deploying portlets, themes, a.s.o.\""
             "echo \"                                   full restarts tomcat and rolles out the liferay app itself, the configuration and portlets ...\""
             "echo \"  Available Releases are:\""
             "find /var/lib/liferay/prepare-rollout/ -mindepth 2 -type d | cut -d/ -f6 | sort -u"
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
             "unzip /var/lib/tomcat7/webapps/ROOT.war -d /var/lib/tomcat7/webapps/ROOT/"
             "cp /var/lib/liferay/prepare-rollout/${1}/config/portal-ext.properties /var/lib/tomcat7/webapps/ROOT/WEB-INF/classes/"
             "chown tomcat7 /var/lib/tomcat7/webapps/*"
             "service tomcat7 start"
             "fi"
             "else"
             "echo \"\"" 
             "echo \"ERROR: Specified release does not exist or you don't have the permission for it! Please run again as root! For a list of the available releases, run this script without parameters in order to show the available releases!\" ;"
             "echo \"\""
             "fi"]) 
            (sut/do-deploy-script "/var/lib/liferay/prepare-rollout/" "/var/lib/liferay/deploy/" "/var/lib/tomcat7/webapps/")))
    )
  (testing 
    "test the corner cases"
    (is
      (thrown? clojure.lang.ExceptionInfo
               (sut/do-deploy-script "/xyz/" "/xyz/" "/")))
    (is
      (thrown? clojure.lang.ExceptionInfo
               (sut/do-deploy-script "/xyz/" "/xyz/" nil)))
    )
  )

(run-tests)