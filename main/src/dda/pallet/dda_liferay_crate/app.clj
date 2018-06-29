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
    [dda.config.commons.map-utils :as mu]
    [dda.pallet.commons.secret :as secret]
    [dda.pallet.core.app :as core-app]
    [dda.pallet.dda-config-crate.infra :as config]
    [dda.pallet.dda-httpd-crate.app :as httpd]
    [dda.pallet.dda-mariadb-crate.app :as db]
    [dda.pallet.dda-tomcat-crate.app :as tomcat]
    [dda.pallet.dda-user-crate.app :as user]
    [dda.pallet.dda-backup-crate.app :as backup]
    [dda.pallet.dda-liferay-crate.domain :as domain]
    [dda.pallet.dda-liferay-crate.infra :as infra]))

(def with-liferay infra/with-liferay)

(def LiferayDomainConfig domain/LiferayDomainConfig)

(def LiferayDomainConfigResolved domain/LiferayDomainConfigResolved)

(def InfraResult infra/InfraResult)

(def LiferayAppConfig
 {:group-specific-config
   {s/Keyword (merge db/InfraResult
                     httpd/InfraResult
                     tomcat/InfraResult
                     infra/InfraResult
                     backup/InfraResult)}})

(s/defn ^:always-validate
  app-configuration-resolved :- LiferayAppConfig
  [resolved-domain-config :- domain/LiferayDomainConfigResolved
   & options]
  (let [{:keys [group-key] :or {group-key infra/facility}} options]
    (mu/deep-merge
     (db/app-configuration-resolved
       (domain/db-domain-configuration resolved-domain-config) :group-key group-key)
     (httpd/tomcat-app-configuration
       (domain/httpd-domain-configuration resolved-domain-config) :group-key group-key)
     (tomcat/app-configuration
       (domain/tomcat-domain-configuration resolved-domain-config) :group-key group-key)
     (backup/app-configuration
       (domain/backup-domain-configuration resolved-domain-config) :group-key group-key)
     {:group-specific-config
      {group-key
       (domain/infra-configuration resolved-domain-config)}})))

(s/defn ^:always-validate
  app-configuration :- LiferayAppConfig
  [domain-config :- LiferayDomainConfig
   & options]
  (let [resolved-domain-config (secret/resolve-secrets domain-config LiferayDomainConfig)]
    (do
      (print resolved-domain-config)
      (apply app-configuration-resolved resolved-domain-config options))))

(s/defmethod ^:always-validate
 core-app/group-spec infra/facility
 [crate-app
  domain-config :- LiferayDomainConfigResolved]
 (let [app-config (app-configuration-resolved domain-config)]
   (core-app/pallet-group-spec
     app-config [(config/with-config app-config)
                 db/with-mariadb
                 httpd/with-httpd
                 tomcat/with-tomcat
                 user/with-user
                 backup/with-backup
                 with-liferay])))

(def crate-app (core-app/make-dda-crate-app
                  :facility infra/facility
                  :domain-schema LiferayDomainConfig
                  :domain-schema-resolved LiferayDomainConfigResolved
                  :default-domain-file "liferay.edn"))
