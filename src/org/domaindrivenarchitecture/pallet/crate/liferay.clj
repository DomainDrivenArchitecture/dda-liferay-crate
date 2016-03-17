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
    [schema.core :as s]
    [schema-tools.core :as st]
    ; pallet
    [pallet.api :as api]
    ; Generic Dependencies
    [org.domaindrivenarchitecture.pallet.crate.basecrate :refer :all]
    [org.domaindrivenarchitecture.pallet.crate.versions :as versions]
    [org.domaindrivenarchitecture.pallet.crate.upgrade :as upgrade]
    [org.domaindrivenarchitecture.pallet.crate.config :as config]
    ; Liferay Dependecies
    [org.domaindrivenarchitecture.pallet.crate.liferay.db :as db]
    [org.domaindrivenarchitecture.pallet.crate.liferay.web :as web]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app :as liferay-app]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as liferay-config]
    [org.domaindrivenarchitecture.pallet.crate.liferay.backup :as liferay-backup]
    [org.domaindrivenarchitecture.pallet.crate.liferay.schema :as schema]
    ; Backup Dependency
    [org.domaindrivenarchitecture.pallet.crate.backup :as backup]
    ; Tomcat Dependency
    [org.domaindrivenarchitecture.pallet.crate.tomcat.app :as tomcat-app]
    [org.domaindrivenarchitecture.pallet.crate.tomcat.app-config :as tomcat-config]
    ))

; Crate Version
(def version [0 2 0])

(def LiferayConfig
  "The configuration for liferay release feature." 
  (merge
    {:db {:root-passwd s/Str
          :db-name s/Str
          :user-name s/Str
          :user-passwd s/Str}
     :httpd {:fqdn s/Str
             :domain-cert s/Str
             :domain-key s/Str
             (s/optional-key :ca-cert) s/Str
             (s/optional-key :app-port) s/Str
             (s/optional-key :google-id) s/Str
             (s/optional-key :maintainance-page-content) [s/Str]}
     :tomcat {:Xmx s/Str
              :Xms s/Str
              :MaxPermSize s/Str
              :home-dir schema/NonRootDirectory
              :webapps-dir schema/NonRootDirectory}
     ; Liferay Configuration
     :instance-name s/Str   
     :home-dir schema/NonRootDirectory
     :lib-dir schema/NonRootDirectory
     :deploy-dir schema/NonRootDirectory
     :third-party-download-root-dir s/Str
     (s/optional-key :fqdn-to-be-replaced) s/Str}
    schema/LiferayReleaseConfig))


(def default-release
  "The default release configuration."
 {:name "LiferayCE"
  :version [6 2 1]
  :application ["ROOT" "http://sourceforge.net/projects/lportal/files/Liferay%20Portal/6.2.1%20GA2/liferay-portal-6.2-ce-ga2-20140319114139101.war"]
  :hooks []
  :layouts []
  :themes []
  :portlets []})

(def default-liferay-config
  "Liferay Crate Default Configuration"
  {; Database Configuration
   :db {:root-passwd "test1234"
        :db-name "lportal"
        :user-name "liferay_user"
        :user-passwd "test1234"}
   ; Webserver Configuration
   :httpd {:fqdn "localhost.localdomain"
           :app-port "8009"
           :maintainance-page-content ["<h1>Webserver Maintainance Mode</h1>"]}
   ; Tomcat Configuration
   :tomcat {:Xmx "1024m"
            :Xms "256m"
            :MaxPermSize "512m"
            :home-dir "/var/lib/tomcat7/"
            :webapps-dir "/var/lib/tomcat7/webapps/"}
   ; Liferay Configuration
   :instance-name "default"   
   :home-dir "/var/lib/liferay/"
   :lib-dir "/var/lib/liferay/lib/"
   :deploy-dir "/var/lib/liferay/deploy/"
   :release-dir "/var/lib/liferay/prepare-rollout/"
   :releases [default-release]})

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(s/defn ^:always-validate merge-config :- LiferayConfig
  "merges the partial config with default config & ensures that resulting config is valid."
  [partial-config]
  (deep-merge default-liferay-config partial-config))

(defn prepare-rollout
  "Liferay: rollout preparation"
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    (liferay-app/prepare-rollout (st/select-schema config schema/LiferayReleaseConfig))
  ))

; Liferay Backup: Install Routine
(defn install-backup
  [app-name partial-config]
  (backup/install-backup-environment :app-name app-name))


; Liferay App: Install Routine
(defn install 
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    ; Upgrade
    (upgrade/upgrade-all-packages)
    ; Database
    (db/install-database (st/get-in config [:db :root-passwd]))
    (db/install-db-instance
      :db-root-passwd (st/get-in config [:db :root-passwd])
      :db-name (st/get-in config [:db :db-name])
      :db-user-name (st/get-in config [:db :user-name])
      :db-user-passwd (st/get-in config [:db :user-passwd]))
    ; Webserver + Tomcat
    (web/install-webserver)
    (tomcat-app/install-tomcat7 :custom-java-version :6)
    ; Liferay Package
    (liferay-app/install-liferay 
      (st/get-in config [:tomcat :home-dir])
      (st/get-in config [:tomcat :webapps-dir])
      (st/get-in config [:home-dir])
      (st/get-in config [:lib-dir])
      (st/get-in config [:deploy-dir])
      (st/get-in config [:third-party-download-root-dir])
      (st/select-schema config schema/LiferayReleaseConfig))
    ; Release Management
;    (release/install-release-management)
    ; do the initial rollout
    (prepare-rollout app-name partial-config)
    ))

(defn configure-backup
  "Liferay Backup: Configure Routine"
  [app-name partial-config]
  (let [config (merge-config partial-config)
        db-user-passwd (st/get-in config [:db :user-passwd])
        db-user-name (st/get-in config [:db :user-named])
        db-name (st/get-in config [:db :db-named])
        instance-name (st/get-in config [:instance-name])
        fqdn (st/get-in config [:httpd :fqdn])
        available-releases (st/get-in config [:releases])]
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

(defn configure
  "Liferay: Configure Routine"
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    ; Webserver
    ; TODO: review mje: if should reside in webserver ns & tested
    (if (st/get-in config [:httpd :domain-key])
      (web/configure-webserver  ; use https
		     :name (st/get-in config [:instance-name])
		     :domain-name (st/get-in config [:httpd :fqdn])
		     :domain-cert (st/get-in config [:httpd :domain-cert])
		     :domain-key (st/get-in config [:httpd :domain-key])
		     :ca-cert (st/get-in config [:httpd :ca-cert])
		     :app-port (st/get-in config [:httpd :app-port])
		     :google-id (st/get-in config [:httpd :google-id])
		     :maintainance-page-content (st/get-in config [:httpd :maintainance-page-content]))
      (web/configure-webserver-local))
    
    ; Tomcat
    (tomcat-app/configure-tomcat7
      :lines-catalina-properties liferay-config/etc-tomcat7-catalina-properties
      :lines-ROOT-xml liferay-config/etc-tomcat7-Catalina-localhost-ROOT-xml
      :lines-etc-default-tomcat7 (tomcat-config/default-tomcat7 
                                   :Xmx (st/get-in config [:tomcat :Xmx])
                                   :Xms (st/get-in config [:tomcat :Xms])
                                   :MaxPermSize (st/get-in config [:tomcat :MaxPermSize])
                                   :jdk6 true)
      :lines-server-xml liferay-config/etc-tomcat7-server-xml
      :lines-setenv-sh (tomcat-config/setenv-sh
                         :Xmx (st/get-in config [:tomcat :Xmx])
                         :Xms (st/get-in config [:tomcat :Xms])
                         :MaxPermSize (st/get-in config [:tomcat :MaxPermSize])
                         :jdk6 true)
      )
    
    ; Liferay
    (liferay-app/configure-liferay
      false
      :db-name (st/get-in config [:db :db-name])
      :db-user-name (st/get-in config [:db :user-name])
      :db-user-passwd (st/get-in config [:db :user-passwd])
      :portal-ext-properties (st/get-in config [:portal-ext-properties-content])
      :fqdn-to-be-replaced (st/get-in config [:fqdn-to-be-replaced])
      :fqdn-replacement (st/get-in config [:httpd :fqdn]))
    ))

; Pallet Server Specs >>liferay<<
(defversionedplan installplan-liferay
  (versions/ver-notinstalled?) install)

(defversionedplan configureplan-liferay
  (versions/ver-always?) configure)

(def liferay-crate 
  "DDA Liferay crate"
  (create-versioned-crate
    :dda-liferay version installplan-liferay configureplan-liferay))

(def with-liferay
  "Pallet server-spec for liferay"
  (api/server-spec
    :phases {
             :configure (api/plan-fn (create-configure-plan liferay-crate))
             :install (api/plan-fn (create-install-plan liferay-crate))
             :prepare-rollout (api/plan-fn 
                                (prepare-rollout
                                  (name (:facility liferay-crate))  
                                  (config/get-nodespecific-additional-config (:facility liferay-crate))
                                  ))}
    ))


; Pallet Server Specs >>liferay-backup<<
(def liferay-backup-crate 
  "Backup Scripts for liferay"
  (create-versioned-crate
    :dda-liferay-backup version install-backup configure-backup))

(def with-liferay-backup
  "Pallet server-spec for liferay backup"
  (create-server-spec liferay-backup-crate))
