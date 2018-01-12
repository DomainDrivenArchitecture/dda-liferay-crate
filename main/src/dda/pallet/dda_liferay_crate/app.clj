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

(ns dda.pallet.dda-liferay-crate.app
  (:require
    [schema.core :as s]
    [dda.cm.group :as group]
    [dda.config.commons.map-utils :as mu]
    [dda.pallet.commons.secret :as secret]
    [dda.pallet.dda-config-crate.infra :as config]
    [dda.pallet.dda-httpd-crate.app :as httpd]
    [dda.pallet.dda-mariadb-crate.app :as db]
    [dda.pallet.dda-tomcat-crate.app :as tomcat]
    [dda.pallet.dda-backup-crate.app :as backup]
    [dda.pallet.dda-user-crate.app :as user]
    [dda.pallet.commons.existing :as existing]
    [dda.pallet.commons.external-config :as ext-config]
    [dda.pallet.dda-liferay-crate.infra :as infra]
    [dda.pallet.dda-liferay-crate.domain :as domain]
    [dda.pallet.dda-liferay-crate.domain.liferay6 :as liferay6]
    [dda.pallet.dda-liferay-crate.domain.liferay7 :as liferay7]))

; ---------------------- schemas  ---------------------------
(def InfraResult infra/InfraResult)

(def with-liferay infra/with-liferay)

(def LiferayCrateAppConfig
 {:group-specific-config
   {s/Keyword (merge db/InfraResult
                     httpd/InfraResult
                     tomcat/InfraResult
                     infra/InfraResult)}})
                    ;backup/InfraResult)}})

; ----------------------- functions  --------------------------
(s/defn ^:always-validate load-targets :- existing/Targets
  [file-name :- s/Str]
  (existing/load-targets file-name))

(s/defn ^:always-validate load-domain :- domain/DomainConfig
  [file-name :- s/Str]
  (ext-config/parse-config file-name))

(s/defn resolve-secrets :- domain/DomainConfigResolved
  [domain-config :- domain/DomainConfig]
  (let [{:keys [db-root-passwd db-user-passwd backup]} domain-config
        {:keys [bucket-name gpg aws]} backup]
    (merge
      domain-config
      {:db-root-passwd (secret/resolve-secret db-root-passwd)
       :db-user-passwd (secret/resolve-secret db-user-passwd)}
      (when (contains? domain-config :backup)
        {:backup {:bucket-name bucket-name
                  :gpg {:gpg-public-key  (secret/resolve-secret (:gpg-public-key gpg))
                        :gpg-private-key (secret/resolve-secret (:gpg-private-key gpg))
                        :gpg-passphrase  (secret/resolve-secret (:gpg-passphrase gpg))}
                  :aws {:aws-access-key-id (secret/resolve-secret (:aws-access-key-id aws))}
                  :aws-secret-access-key (secret/resolve-secret (:aws-secret-access-key aws))}}))))

(s/defn ^:always-validate lr6-app-configuration :- LiferayCrateAppConfig
  "Generates the AppConfig from a smaller domain-config."
  [resolved-domain-config :- domain/DomainConfigResolved
   group-key :- s/Keyword]
  (mu/deep-merge (db/app-configuration-resolved
                   (liferay6/db-domain-configuration resolved-domain-config)
                   :group-key group-key)
                 (httpd/tomcat-app-configuration
                   (liferay6/httpd-domain-configuration resolved-domain-config)
                   :group-key group-key)
                 (tomcat/app-configuration
                   (liferay6/tomcat-domain-configuration resolved-domain-config)
                   :group-key group-key)
                 ;(backup/app-configuration)
                  ; (domain/backup-domain-configuration resolved-domain-config)
                   ;:group-key group-key)
                 {:group-specific-config
                  {group-key
                   (liferay6/infra-configuration resolved-domain-config)}}))

(s/defn ^:always-validate lr7-app-configuration :- LiferayCrateAppConfig
  "Generates the AppConfig from a smaller domain-config."
  [resolved-domain-config :- domain/DomainConfigResolved
   group-key :- s/Keyword]
  (mu/deep-merge (db/app-configuration-resolved
                   (liferay7/db-domain-configuration resolved-domain-config)
                   :group-key group-key)
                 (httpd/tomcat-app-configuration
                   (liferay7/httpd-domain-configuration resolved-domain-config)
                   :group-key group-key)
                 (tomcat/app-configuration
                   (liferay7/tomcat-domain-configuration resolved-domain-config)
                   :group-key group-key)
                 ;(backup/app-configuration)
                  ; (domain/backup-domain-configuration resolved-domain-config)
                   ;:group-key group-key)
                 {:group-specific-config
                  {group-key
                   (liferay7/infra-configuration resolved-domain-config)}}))

(s/defn ^:always-validate liferay-group-spec
 [app-config :- LiferayCrateAppConfig]
 (group/group-spec
   app-config [(config/with-config app-config)
               db/with-mariadb
               httpd/with-httpd
               tomcat/with-tomcat
               ;backup/with-backup
               with-liferay]))

(s/defn ^:always-validate app-configuration :- LiferayCrateAppConfig
  "Generates the AppConfig from a smaller domain-config."
  [resolved-domain-config :- domain/DomainConfigResolved
   & options]
  (let [{:keys [group-key] :or {group-key infra/facility}} options
        {:keys [liferay-version]} resolved-domain-config]
    (if (= liferay-version :LR6)
      (lr6-app-configuration resolved-domain-config group-key)
      (lr7-app-configuration resolved-domain-config group-key))))
