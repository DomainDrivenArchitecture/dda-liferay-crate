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

(ns org.domaindrivenarchitecture.pallet.crate.liferay
  (:require
      ; Generic Dependencies
      [org.domaindrivenarchitecture.pallet.crate.basecrate :refer :all]
      [org.domaindrivenarchitecture.pallet.crate.upgrade :as upgrade]
      ; Liferay Dependecies
      [org.domaindrivenarchitecture.pallet.crate.liferay.db :as db]
      [org.domaindrivenarchitecture.pallet.crate.liferay.release-management :as release]
      [org.domaindrivenarchitecture.pallet.crate.liferay.web :as web]
      [org.domaindrivenarchitecture.pallet.crate.liferay.app :as liferay-app]
      [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as liferay-config]
      [org.domaindrivenarchitecture.pallet.crate.liferay.backup :as liferay-backup]
      ; Backup Dependency
      [org.domaindrivenarchitecture.pallet.crate.backup :as backup]
      ; Tomcat Dependency
      [org.domaindrivenarchitecture.pallet.crate.tomcat.app :as tomcat-app]
      [org.domaindrivenarchitecture.pallet.crate.tomcat.app-config :as tomcat-config]
      ))

; Crate Version
(def version [0 1 2])

; Liferay Crate Default Configuration
(def default-liferay-config
  {
   ; Database Configuration
   :db-user-passwd "test1234"
   :db-root-passwd "test1234"
   :db-name "lportal"
   :db-user-name "liferay_user"
   ; Webserver Configuration
   :maintainance-page-content "<h1>Webserver Maintainance Mode</h1>"
   ; Tomcat Configuration
   :Xmx "1024m"
   :Xms "256m"
   :MaxPermSize "512m"
   ; Liferay Download Source
   :liferay-download-source "http://sourceforge.net/projects/lportal/files/Liferay%20Portal/6.2.1%20GA2/liferay-portal-6.2-ce-ga2-20140319114139101.war"
   ; Liferay Configuration
   :instance-name "liferay-local"   
   })


; Liferay Backup: Install Routine
(defn install-backup
  [app-name config]
  (backup/install-backup-environment :app-name app-name))


; Liferay App: Install Routine
(defn install 
  [app-name nodeconfig]
  (let [config (merge default-liferay-config nodeconfig)]
    ; Upgrade
    (upgrade/upgrade-all-packages)
    ; Database
    (db/install-database (:db-root-passwd config))
    (db/install-db-instance
      :db-root-passwd (:db-root-passwd config)
      :db-name (:db-name config)
      :db-user-name (:db-user-name config)
      :db-user-passwd (:db-user-passwd config))
    ; Webserver + Tomcat
    (web/install-webserver)
    (tomcat-app/install-tomcat7 :custom-java-version :6)
    ; Liferay Package
    (liferay-app/install-liferay 
      (:repo-download-source config)
      :custom-build? (contains? config :build-version)
      :liferay-download-source (:liferay-download-source config))
    ; Release Management
    (release/install-release-management)
    ))


; Liferay Backup: Configure Routine
(defn configure-backup
  [app-name config]
  (let [db-user-passwd (:db-user-passwd config)
        db-user-name (:db-user-name config)
        db-name (:db-name config)
        fqdn (-> config :fqdn)
        instance-name (-> config :instance-name)
        available-releases (-> config :available-releases)
        all-releases (-> config :all-releases)
        plugin-blacklist (-> config :plugin-blacklist)]
    (backup/install-backup-app-instance 
      :app-name app-name 
      :instance-name instance-name
      :backup-lines
      (liferay-backup/liferay-source-backup-script-lines instance-name db-user-passwd)
      :source-transport-lines
      (liferay-backup/liferay-source-transport-script-lines instance-name 1)
      :restore-lines
      (liferay-backup/liferay-restore-script-lines 
        instance-name 
        fqdn
        db-name
        db-user-name
        db-user-passwd)
      )))

; Liferay: Configure Routine
(defn configure
  [app-name nodeconfig]
  (let [config (merge default-liferay-config nodeconfig)]
    ; Webserver
    (if (:ca-cert config)
      (web/configure-webserver  ; use https
		     :name (:instance-name config)
		     :domain-name (:fqdn config)
		     :domain-cert (:domain-cert config)
		     :domain-key (:domain-key config)
		     :ca-cert (:ca-cert config)
		     :app-port "8009"
		     :google-id (:google-id config)
		     :maintainance-page-content (:maintainance-page-content config))
      (web/configure-webserver-local))  ; don't use https for a local instance
    
    ; Tomcat
    (tomcat-app/configure-tomcat7
      :lines-catalina-properties liferay-config/etc-tomcat7-catalina-properties
      :lines-ROOT-xml liferay-config/etc-tomcat7-Catalina-localhost-ROOT-xml
      :lines-etc-default-tomcat7 (tomcat-config/default-tomcat7 
                                   :Xmx (:Xmx config)
                                   :Xms (:Xms config)
                                   :MaxPermSize (:MaxPermSize config)
                                   :jdk6 (:jdk6 config))
      :lines-server-xml liferay-config/etc-tomcat7-server-xml
      :lines-setenv-sh (tomcat-config/setenv-sh
                         :Xmx (:Xmx config)
                         :Xms (:Xms config)
                         :MaxPermSize (:MaxPermSize config)
                         :jdk6 (:jdk6 config))
      )
    
    ; Liferay
    (liferay-app/configure-liferay
      false
      :db-name (:db-name config)
      :db-user-name (:db-user-name config)
      :db-user-passwd (:db-user-passwd config)
      :portal-ext-properties (:portal-ext-properties-content config)
      :fqdn-to-be-replaced (:fqdn-to-be-replaced config)
      :fqdn-replacement (:fqdn config))
    )
  )


; Pallet Server Specs >>liferay<<
(defversionedplan installplan-liferay
  (ver-notinstalled?) install)

(defversionedplan configureplan-liferay
  (ver-always?) configure configure-instance)

(def liferay-crate 
  "DDA Liferay crate"
  (create-versioned-crate
    :dda-liferay version installplan-liferay configureplan-liferay))

(def with-liferay
  "Pallet server-spec for liferay"
  (create-server-spec liferay-crate))


; Pallet Server Specs >>liferay-backup<<
(def liferay-backup-crate 
  "Backup Scripts for liferay"
  (create-versioned-crate
    :dda-liferay-backup version install-backup configure-backup))

(def with-liferay-backup
  "Pallet server-spec for liferay backup"
  (create-server-spec liferay-backup-crate))
