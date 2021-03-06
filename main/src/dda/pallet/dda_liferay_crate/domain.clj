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

(ns dda.pallet.dda-liferay-crate.domain
  (:require
    [schema.core :as s]
    [dda.pallet.dda-liferay-crate.infra :as infra]
    [dda.pallet.dda-liferay-crate.domain.backup :as backup]
    [dda.pallet.dda-liferay-crate.domain.schema :as schema]
    [dda.pallet.dda-liferay-crate.domain.liferay :as liferay]))


; ----- schemas for the domain configuration ----------
(def LiferayDomainConfig schema/LiferayDomainConfig)

(def LiferayDomainConfigResolved schema/LiferayDomainConfigResolved)

; --------------  standard configuration values   -----------------------
(def db-name "lportal")

; -----------------  functions  -----------------------
(s/defn ^:always-validate
  db-domain-configuration
  [domain-config :- LiferayDomainConfigResolved]
  (let [{:keys [db-root-passwd db-user-name db-user-passwd]} domain-config]
    {:root-passwd db-root-passwd
     :settings #{}
     :db [{:db-name db-name
           :db-user-name db-user-name
           :db-user-passwd db-user-passwd
           :create-options "character set utf8"}]}))

(s/defn ^:always-validate
  httpd-domain-configuration
  [domain-config :- LiferayDomainConfigResolved]
  (let [{:keys [fq-domain-name google-id settings]} domain-config]
    {:tomcat
     (merge
      {:domain-name fq-domain-name}
      (when (contains? domain-config :google-id)
        {:google-id google-id})
      (when (contains? domain-config :settings)
        {:settings settings})
      {:alias [{:url "/quiz/" :path "/var/www/static/quiz/"}]
       :jk-unmount [{:path "/quiz/*" :worker "mod_jk_www"}]})}))

(s/defn ^:always-validate
  tomcat-domain-configuration
  [domain-config :- LiferayDomainConfigResolved]
  (let [{:keys [liferay-version]} domain-config]
      (liferay/tomcat-domain-configuration domain-config liferay-version)))


(s/defn ^:always-validate
  tomcat-user
  [domain-config :- LiferayDomainConfigResolved]
  (let [{:keys [liferay-version]} domain-config]
    (case liferay-version
      :LR6 "tomcat7"
      :LR7 "tomcat8")))

(s/defn ^:always-validate
  backup-domain-configuration
  [domain-config :- LiferayDomainConfigResolved]
  (backup/backup-domain-config domain-config db-name (tomcat-user domain-config)))

(s/defn ^:always-validate
  infra-configuration :- infra/InfraResult
  [domain-config :- LiferayDomainConfigResolved]
  (let [{:keys [liferay-version]} domain-config]
    {infra/facility (liferay/liferay-infra-configuration domain-config db-name liferay-version)}))
