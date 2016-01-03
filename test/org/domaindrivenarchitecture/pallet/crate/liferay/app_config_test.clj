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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.web-test
  (:require
    [clojure.test :refer :all]
    [pallet.actions :as actions]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as sut]
    ))

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