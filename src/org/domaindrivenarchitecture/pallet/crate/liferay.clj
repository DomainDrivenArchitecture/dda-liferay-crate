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
      [pallet.actions :as actions]
      [pallet.api :as api]
      [pallet.crate :as crate]
      [pallet.crate.service :as service]
      [pallet.stevedore :as stevedore]
      [org.domaindrivenarchitecture.pallet.crate.dda-base :as dda-base]
      [org.domaindrivenarchitecture.pallet.crate.liferay.db :as db]
      [org.domaindrivenarchitecture.pallet.crate.backup :as backup]
      [org.domaindrivenarchitecture.pallet.crate.liferay.backup :as lr-backup]
      [org.domaindrivenarchitecture.pallet.crate.config :as config]
      [org.domaindrivenarchitecture.pallet.crate.liferay.release-management :as release]
      [org.domaindrivenarchitecture.pallet.crate.liferay.web :as web]
      [org.domaindrivenarchitecture.pallet.crate.liferay.app :as liferay-app]
      [org.domaindrivenarchitecture.pallet.crate.tomcat.app :as tomcat-app]
      [org.domaindrivenarchitecture.pallet.crate.tomcat.app-config :as tomcat-config]
      [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as liferay-config]
      [org.domaindrivenarchitecture.pallet.crate.upgrade :as upgrade]
      ))

(def facility :dda-liferay)

(defn install-backup
  [app-name config]
  (backup/install-backup-environment 
    :app-name app-name)
  )

(defn install
  [app-name config]
  (let [db-root-passwd (:db-root-passwd config)
        db-user-passwd (:db-user-passwd config)]
    (upgrade/upgrade-all-packages)
    (db/install-database db-root-passwd)
    (db/install-db-instance
            :db-root-passwd db-root-passwd
            :db-name "lportal"
            :db-user-name "liferay_user"
            :db-user-passwd db-user-passwd)
    (web/install-webserver)
    (tomcat-app/install-tomcat7 
      :custom-java-version :6)
    (liferay-app/install-liferay :custom-build? (contains? config :build-version)
                                 :liferay-download-source (:liferay-download-source config))
    (install-backup app-name config)
    (release/install-release-management)
    ))

(defn configure-backup
  [app-name config]
  (let [db-user-passwd (:db-user-passwd config)
        db-user-name (:db-user-name config)
        db-name (:db-name config)
        fqdn (-> config :fqdn)
        instance-name (-> config :instance-name)
        available-releases (-> config :available-releases)
        all-releases (-> config :all-releases)
        plugin-blacklist (-> config :plugin-blacklist)
        ]
    (backup/install-backup-app-instance 
      :app-name app-name 
      :instance-name instance-name
      :backup-lines
      (lr-backup/liferay-source-backup-script-lines instance-name db-user-passwd)
      :source-transport-lines
      (lr-backup/liferay-source-transport-script-lines instance-name 1)
      :restore-lines
      (lr-backup/politaktiv-liferay-restore-script-lines 
        instance-name 
        fqdn
        db-name
        db-user-name
        db-user-passwd)
      )
    )
  )

(defn configure
  [app-name config]
  (let [db-user-passwd (-> config :db-user-passwd)
        fqdn (-> config :fqdn)
        instance-name (-> config :instance-name)
        domain-cert (-> config :domain-cert)
        domain-key (-> config :domain-key)
        ca-cert (-> config :ca-cert)
        google-id (-> config :google-id)
        maintainance-page-content (-> config :maintainance-page-content)
        portal-ext-properties-content (-> config :portal-ext-properties-content)
        Xmx (-> config :Xmx)
        Xms (-> config :Xms)
        MaxPermSize (-> config :MaxPermSize)
        jdk6 (-> config :jdk6)
        fqdn-to-be-replaced (-> config :fqdn-to-be-replaced)]
    (web/configure-webserver 
                  :name instance-name
                  :domain-name fqdn
                  :domain-cert domain-cert
                  :domain-key domain-key
                  :ca-cert ca-cert
                  :app-port "8009"
                  :google-id google-id
                  :maintainance-page-content maintainance-page-content)
    (tomcat-app/configure-tomcat7
      :lines-catalina-properties liferay-config/etc-tomcat7-catalina-properties
      :lines-ROOT-xml liferay-config/etc-tomcat7-Catalina-localhost-ROOT-xml
      :lines-etc-default-tomcat7 (tomcat-config/default-tomcat7 
                                   :Xmx Xmx
                                   :Xms Xms
                                   :MaxPermSize MaxPermSize
                                   :jdk6 jdk6)
      :lines-server-xml liferay-config/etc-tomcat7-server-xml
      :lines-setenv-sh (tomcat-config/setenv-sh
                         :Xmx Xmx 
                         :Xms Xms
                         :MaxPermSize MaxPermSize
                         :jdk6 jdk6)
      )
    (liferay-app/configure-liferay
      false
      :db-name "lportal"
      :db-user-name "liferay_user"
      :db-user-passwd db-user-passwd
      :portal-ext-properties portal-ext-properties-content
      :fqdn-to-be-replaced fqdn-to-be-replaced
      :fqdn-replacement fqdn)
    (configure-backup app-name config)
    )
  )

(def ^:dynamic with-liferay
  (api/server-spec
    :phases 
    {:install
     (api/plan-fn
       (dda-base/install-with-instances
         (name facility)
         (config/get-nodespecific-additional-config facility)
         install))
     :configure
     (api/plan-fn
       (dda-base/configure-with-instances
         (name facility)
         (config/get-nodespecific-additional-config facility)
         configure))
    }))



(def ^:dynamic with-liferay-backup
  (api/server-spec
    :phases 
    {:install
     (api/plan-fn
       (dda-base/install-with-instances
         (name facility)
         (config/get-nodespecific-additional-config facility)
         install-backup))
     :configure
     (api/plan-fn
       (dda-base/configure-with-instances
         (name facility)
         (config/get-nodespecific-additional-config facility)
         configure-backup))
    }))
