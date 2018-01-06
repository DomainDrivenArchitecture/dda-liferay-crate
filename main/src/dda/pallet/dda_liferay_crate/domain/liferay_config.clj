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
(ns dda.pallet.dda-liferay-crate.domain.liferay-config
  (:require
    [schema.core :as s]
    [dda.pallet.dda-liferay-crate.domain.schema :as schema]))

; ----------  auxiliary functions for creating infra configuration  ------
(s/defn portal-ext-properties
  "creates the default portal-ext.properties for mySql."
  [domain-config :- schema/DomainConfigResolved]
  (let [{:keys [db-name db-user-name db-user-passwd]} domain-config]
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
     (str "jdbc.default.url=jdbc:mysql://localhost:3306/" :db-name
          "?useUnicode=true&characterEncoding=UTF-8&useFastDateParsing=false")
     (str "jdbc.default.username=" :db-user-name)
     (str "jdbc.default.password=" :db-user-passwd)
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
     ""]))

(s/defn default-release
  "The default release configuration."
  [portal-ext-lines]
 {:name "LiferayCE"
  :version [6 2 1]
  :app ["ROOT" "http://ufpr.dl.sourceforge.net/project/lportal/Liferay%20Portal/6.2.1%20GA2/liferay-portal-6.2-ce-ga2-20140319114139101.war"]
  :config portal-ext-lines})

(s/defn default-release-config
  "The default release configuration."
  [domain-config :- schema/DomainConfigResolved]
  (default-release (portal-ext-properties domain-config)))