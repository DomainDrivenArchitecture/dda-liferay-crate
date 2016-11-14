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
    ; pallet
    [pallet.api :as api]
    ; Generic Dependencies
    [org.domaindrivenarchitecture.pallet.core.dda-crate :as dda-crate]
    [org.domaindrivenarchitecture.pallet.crate.package :as package]
    [org.domaindrivenarchitecture.pallet.crate.mysql :as mysql]
    [org.domaindrivenarchitecture.config.commons.directory-model :as dir-model]
    [org.domaindrivenarchitecture.config.commons.map-utils :as map-utils]
    ; Liferay Dependecies
    [org.domaindrivenarchitecture.pallet.crate.liferay.db :as db]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app :as liferay-app]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as liferay-config]
    [org.domaindrivenarchitecture.pallet.crate.liferay.release-model :as release-model]
    ; Webserver Dependency
    [org.domaindrivenarchitecture.pallet.crate.httpd :as httpd]
    ; Backup Dependency
    [org.domaindrivenarchitecture.pallet.crate.backup-0-3 :as backup]
    ; Tomcat Dependency
    [org.domaindrivenarchitecture.pallet.crate.tomcat :as tomcat]
    ))

(def LiferayConfig
  "The configuration for liferay crate." 
  (merge
    {:db mysql/DbConfig 
     :tomcat tomcat/TomcatConfig
     :backup backup/BackupConfig
     :instance-name s/Str   
     :home-dir dir-model/NonRootDirectory
     :lib-dir dir-model/NonRootDirectory
     :deploy-dir dir-model/NonRootDirectory
     :third-party-download-root-dir s/Str
     (s/optional-key :httpd) httpd/HttpdConfig
     (s/optional-key :fqdn-to-be-replaced) s/Str}
    release-model/LiferayReleaseConfig))


(s/defn default-release-config
  "The default release configuration."
  [db-config :- mysql/DbConfig]
  (release-model/default-release (liferay-config/portal-ext-properties db-config)))

(defn default-config-standalone
  "Liferay Crate Default Configuration without a web tier."
  []
  (let [db-config {:root-passwd "test1234"
                   :db-name "lportal"
                   :user-name "liferay_user"
                   :user-passwd "test1234"}
        fqdn "localhost.localdomain"]
    {; Database Configuration
     :db db-config
     ; Tomcat Configuration
     :tomcat (tomcat/merge-config 
               {:server-xml-config
                {:shutdown-port "8005"
                 :start-ssl false
                 :executor-daemon "false"
                 :executor-max-threads "151"
                 :executor-min-spare-threads "10"
                 :connector-port "8009"
                 :connector-protocol "AJP/1.3"}
                :java-vm-config 
                {:xms "1024m"
                 :xmx "2048m"
                 :max-perm-size "512m"
                 :jdk6 true}
                :catalina-properties-lines liferay-config/etc-tomcat7-catalina-properties
                :root-xml-lines liferay-config/etc-tomcat7-Catalina-localhost-ROOT-xml
                })
     :backup {:backup-name "service-name"
              :script-path "/usr/lib/dda-backup/"
              :gens-stored-on-source-system 1
              :service-restart "tomcat7"
              :elements [{:type :file-compressed
                          :name "letsencrypt"
                          :root-dir "/etc/letsencrypt/"
                          :subdir-to-save "accounts csr keys renewal live"}
                         {:type :file-compressed
                          :name "liferay"
                          :root-dir "/var/lib/liferay/data/"
                          :subdir-to-save "document_library images"
                          :new-owner "tomcat7"}
                         {:type :mysql
                          :name "liferay"
                          :db-user-name "db-user-name" 
                          :db-user-passwd "db-pass"
                          :db-name "db-name"
                          :db-create-options "character set utf8"}]
              :backup-user {:name "dataBackupSource"
                            :encrypted-passwd "WIwn6jIUt2Rbc"}}
     ; Liferay Configuration
     :instance-name "default"   
     :home-dir "/var/lib/liferay/"
     :lib-dir "/var/lib/liferay/lib/"
     :deploy-dir "/var/lib/liferay/deploy/"
     :release-dir "/var/lib/liferay/prepare-rollout/"
     :releases [(default-release-config db-config)]}))

(defn default-config
  "Liferay Crate Default Configuration"
  []
  (let [fqdn "localhost.localdomain"]
    (merge 
      (default-config-standalone)
      ; Webserver Configuration
      {:httpd httpd/default-config}
      {:tomcat (tomcat/merge-config 
                 {:server-xml-config
                  {:shutdown-port "8005"
                   :start-ssl true
                   :executor-daemon "false"
                   :executor-max-threads "151"
                   :executor-min-spare-threads "10"
                   :connector-port "8009"
                   :connector-protocol "AJP/1.3"}
                  :java-vm-config 
                  {:xms "768m"
                   :xmx "1024m"
                   :max-perm-size "512m"
                   :jdk6 true}
                  :catalina-properties-lines liferay-config/etc-tomcat7-catalina-properties
                  :root-xml-lines liferay-config/etc-tomcat7-Catalina-localhost-ROOT-xml
                  })
       })
  ))

(defn install
  "Installs full liferay."
  [app-name config]
  ; Upgrade
  (package/update-and-upgrade)
  ; Database
  (db/install-database (get-in config [:db :root-passwd]))
  (db/install-db-instance
    :db-root-passwd (get-in config [:db :root-passwd])
    :db-name (get-in config [:db :db-name])
    :db-user-name (get-in config [:db :user-name])
    :db-user-passwd (get-in config [:db :user-passwd]))
  ; Webserver + Tomcat
  (when (get-in config [:httpd])
    (httpd/install (get-in config [:httpd])))
  (tomcat/install (get-in config [:tomcat]))
  ; Liferay Package
  (liferay-app/install-liferay 
    (get-in config [:tomcat :tomcat-home-location])
    (get-in config [:tomcat :webapps-location])
    (get-in config [:home-dir])
    (get-in config [:lib-dir])
    (get-in config [:deploy-dir])
    (get-in config [:third-party-download-root-dir])
    (map-utils/filter-for-target-schema release-model/LiferayReleaseConfig config))
  ; backup
  (backup/install app-name (get-in config [:backup]))
  ; do the initial rollout
  (liferay-app/prepare-rollout 
    (map-utils/filter-for-target-schema release-model/LiferayReleaseConfig config))
  )

(defmethod dda-crate/dda-install 
  :dda-liferay [dda-crate partial-effective-config]
  (let [config (dda-crate/merge-config dda-crate partial-effective-config)]
    (install (name (get-in dda-crate [:facility])) config)))

(defn configure
  "Liferay: Configure Routine"
  [app-name config]
  ; Webserver
  (when (get-in config [:httpd])
    (httpd/configure (get-in config [:httpd])))
  
  ; Tomcat
  (tomcat/configure (get-in config [:tomcat]))
    
  ; Liferay
  (liferay-app/configure-liferay
    false
    :db-name (get-in config [:db :db-name])
    :db-user-name (get-in config [:db :user-name])
    :db-user-passwd (get-in config [:db :user-passwd])
    :fqdn-to-be-replaced (get-in config [:fqdn-to-be-replaced])
    :fqdn-replacement (get-in config [:httpd :fqdn]))
  ; Config
  (backup/configure app-name (get-in config [:backup]))  
  )

(defmethod dda-crate/dda-configure 
  :dda-liferay [dda-crate partial-effective-config]
  (let [config (dda-crate/merge-config dda-crate partial-effective-config)]
    (configure (name (get-in dda-crate [:facility])) config)))

(defn prepare-rollout
  "Liferay: rollout preparation"
  [config]
  (liferay-app/prepare-rollout
    (map-utils/filter-for-target-schema release-model/LiferayReleaseConfig config))
  )

(defmethod dda-crate/dda-app-rollout 
  :dda-liferay [dda-crate partial-effective-config]
  (let [config (dda-crate/merge-config dda-crate partial-effective-config)]
    (prepare-rollout config)))

(def dda-liferay-crate 
  (dda-crate/make-dda-crate
    :facility :dda-liferay
    :version [0 2 2]
    :config-schema LiferayConfig
    :config-default (default-config)
    ))

(def with-liferay
  (dda-crate/create-server-spec dda-liferay-crate))

(def dda-liferay-crate-standalone 
  (dda-crate/make-dda-crate
    :facility :dda-liferay
    :version [0 2 2]
    :config-schema LiferayConfig
    :config-default (default-config-standalone)
    ))

(def with-liferay-standalone
  (dda-crate/create-server-spec dda-liferay-crate-standalone))

(s/defn ^:always-validate merge-config :- LiferayConfig
  "merges the partial config with default config & ensures that resulting config is valid."
  ([partial-config]
    (dda-crate/merge-config dda-liferay-crate partial-config))
  ([partial-config standalone]
    (dda-crate/merge-config dda-liferay-crate-standalone partial-config)))
   
(s/defn merge-releases
 "Merges multiple liferay releases into a combined one. All non-app keys are from the right-most
 release. Apps are merged from right to left. Duplicate apps (same name) are ignored and the
 right-most app wins." 
 [& vals]
 (apply release-model/merge-releases vals))


