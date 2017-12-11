; Copyright (c) meissa GmbH. All rights reserved.
; You must not remove this notice, or any other, from this software.
;
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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.db
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as string]
    [pallet.api :as api]
    [pallet.actions :as actions]
    [pallet.crate :as crate]
    [pallet.crate.service :as service]
    [pallet.stevedore :as stevedore]
    [org.domaindrivenarchitecture.pallet.crate.mysql :as mysql]
    ))

(defn install-database
  [db-root-passwd]
  (mysql/install-mysql :db-root-password db-root-passwd)
  (actions/service "mysql" :action :restart)
)

(defn install-db-instance
  [& {:keys [db-root-passwd 
             db-name 
             db-user-name 
             db-user-passwd]}]
  (mysql/create-database 
    :db-user-name "root"
    :db-passwd db-root-passwd
    :db-name db-name
    :create-options "CHARACTER SET utf8")
  (mysql/grant 
    :db-root-user-name "root"
    :db-root-passwd db-root-passwd 
    :db-user-name db-user-name
    :db-user-passwd db-user-passwd
    :grant-level (str "`" db-name "`.*" ))
  (mysql/mysql-script 
    :db-user-name "root" 
    :db-passwd db-root-passwd
    :sql-script "flush privileges;")
  )