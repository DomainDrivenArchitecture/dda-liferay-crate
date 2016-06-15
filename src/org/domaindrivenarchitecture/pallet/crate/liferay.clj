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
    [org.domaindrivenarchitecture.pallet.crate.basecrate :refer :all]
    [org.domaindrivenarchitecture.pallet.crate.versions :as versions]
    [org.domaindrivenarchitecture.pallet.crate.upgrade :as upgrade]
    [org.domaindrivenarchitecture.pallet.crate.config-0-3 :as config]
    [org.domaindrivenarchitecture.pallet.crate.mysql :as mysql]
    [org.domaindrivenarchitecture.config.commons.directory-model :as dir-model]
    [org.domaindrivenarchitecture.config.commons.map-utils :as map-utils]
    ; Liferay Dependecies
    [org.domaindrivenarchitecture.pallet.crate.liferay.db :as db]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app :as liferay-app]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as liferay-config]
    [org.domaindrivenarchitecture.pallet.crate.liferay.release-model :as schema]
    ; Webserver Dependency
    [org.domaindrivenarchitecture.pallet.crate.httpd :as httpd]
    ; Backup Dependency
    [org.domaindrivenarchitecture.pallet.crate.backup :as backup]
    ; Tomcat Dependency
    [org.domaindrivenarchitecture.pallet.crate.tomcat :as tomcat]
    ; TODO: review jem 2016_06_15: remove access to tomcats internals
    [org.domaindrivenarchitecture.pallet.crate.tomcat.app :as tomcat-app]
    [org.domaindrivenarchitecture.pallet.crate.tomcat.app-config :as tomcat-config]
    ))

; Crate Version
(def version [0 2 0])

(defn- app-in-vec? 
  "Returns wheather a liferay app with specified name is in vector apps"
  [apps name]
  (if (empty? apps)
    false
    (if (= (first (last apps)) name)
      true
      (app-in-vec? (pop apps) name))
    ))

(defn- merge-apps
  "Merge two vector of apps from right to left. Duplicate apps (same name) are ignored and the
right-most app wins."
  [p1 p2] 
  (apply conj 
         (vec (keep #(if-not (app-in-vec? p2 (first %)) %) p1))
         p2))

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
     (s/optional-key :fqdn-to-be-replaced) s/Str
     (s/optional-key :disabled-tiers) [s/Keyword]}
    schema/LiferayReleaseConfig))


(s/defn default-release
  "The default release configuration."
  [db-config :- mysql/DbConfig]
 {:name "LiferayCE"
  :version [6 2 1]
  :app ["ROOT" "http://iweb.dl.sourceforge.net/project/lportal/Liferay%20Portal/6.2.1%20GA2/liferay-portal-6.2-ce-ga2-20140319114139101.war"]
  :config (liferay-config/portal-ext-properties db-config)})

(defn default-liferay-config-without-httpd
  "Liferay Crate Default Configuration"
  []
  (let [db-config {:root-passwd "test1234"
                   :db-name "lportal"
                   :user-name "liferay_user"
                   :user-passwd "test1234"}
        fqdn "localhost.localdomain"]
    {; Database Configuration
     :db db-config
     ; Tomcat Configuration
     :tomcat {:xmx "1024m"
              :xms "256m"
              :max-perm-size "512m"
              :home-dir "/var/lib/tomcat7/"
              :webapps-dir "/var/lib/tomcat7/webapps/"}
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
     :releases [(default-release db-config)]}))

(defn default-liferay-config
  "Liferay Crate Default Configuration"
  []
  (let [fqdn "localhost.localdomain"]
    (merge 
      (default-liferay-config-without-httpd)
      ; Webserver Configuration
      {:httpd httpd/default-config})
  ))

(s/defn ^:always-validate merge-config :- LiferayConfig
  "merges the partial config with default config & ensures that resulting config is valid."
  ([partial-config]
    (map-utils/deep-merge (default-liferay-config) partial-config))
  ([partial-config disable-httpd]
    (map-utils/deep-merge (default-liferay-config-without-httpd) partial-config)))

(s/defn ^:always-validate merge-releases :- schema/LiferayRelease
  "Merges multiple liferay releases into a combined one. All non-app keys are from the right-most
release. Apps are merged from right to left. Duplicate apps (same name) are ignored and the
right-most app wins." 
 [& vals]
 (apply merge-with 
        (fn [& args] 
          (if (and (every? vector? args) (vector? (ffirst args)))
            (apply merge-apps args)
            (last args))
          ) 
        vals)
 )

(defn prepare-rollout
  "Liferay: rollout preparation"
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    (liferay-app/prepare-rollout
      (map-utils/filter-for-target-schema schema/LiferayReleaseConfig config))
  ))

; Liferay Backup: Install Routine
(defn install-backup
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    (backup/install app-name (get-in config [:backup]))
    ))

(defn update-0-1-to-0-2
  "updates the installation."
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    (liferay-app/install-do-rollout-script 
      (get-in config [:home-dir])
      (get-in config [:release-dir]) 
      (get-in config [:deploy-dir])
      (get-in config [:tomcat :webapps-dir]))
    (when (get-in config [:httpd])
      (httpd/install (get-in config [:httpd])))
    ))
    

; Liferay App: Install Routine
(defn install 
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    ; Upgrade
    (upgrade/upgrade-all-packages)
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
    (tomcat-app/install-tomcat7 :custom-java-version :6)
    ; Liferay Package
    (liferay-app/install-liferay 
      (get-in config [:tomcat :home-dir])
      (get-in config [:tomcat :webapps-dir])
      (get-in config [:home-dir])
      (get-in config [:lib-dir])
      (get-in config [:deploy-dir])
      (get-in config [:third-party-download-root-dir])
      (map-utils/filter-for-target-schema schema/LiferayReleaseConfig config))
    ; backup
    (backup/install app-name (get-in config [:backup]))
    ; do the initial rollout
    (prepare-rollout app-name partial-config)
    ))

(defn configure-backup
  "Liferay Backup: Configure Routine"
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    (backup/configure app-name (get-in config [:backup]))
    ))

(defn configure
  "Liferay: Configure Routine"
  [app-name partial-config]
  (let [config (merge-config partial-config)]
    ; Webserver
    (when (get-in config [:httpd])
      (httpd/configure (get-in config [:httpd])))
    
    ; Tomcat
    (tomcat-app/configure-tomcat7
      :lines-catalina-properties liferay-config/etc-tomcat7-catalina-properties
      :lines-ROOT-xml liferay-config/etc-tomcat7-Catalina-localhost-ROOT-xml
      :lines-etc-default-tomcat7 (tomcat-config/default-tomcat7 
                                   :Xmx (get-in config [:tomcat :Xmx])
                                   :xms (get-in config [:tomcat :xms])
                                   :MaxPermSize (get-in config [:tomcat :MaxPermSize])
                                   :jdk6 true)
      :lines-server-xml liferay-config/etc-tomcat7-server-xml
      :lines-setenv-sh (tomcat-config/setenv-sh
                         :Xmx (get-in config [:tomcat :Xmx])
                         :xms (get-in config [:tomcat :xms])
                         :MaxPermSize (get-in config [:tomcat :MaxPermSize])
                         :jdk6 true)
      )
    
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
