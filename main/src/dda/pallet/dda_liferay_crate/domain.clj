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
    [dda.config.commons.map-utils :as mu]
    [dda.pallet.commons.secret :as secret]
    [dda.pallet.dda-liferay-crate.infra :as infra]
    [dda.pallet.dda-liferay-crate.domain.backup :as tomcat]
    [dda.pallet.dda-liferay-crate.domain.backup :as backup]
    [dda.pallet.dda-liferay-crate.domain.schema :as schema]
    [dda.pallet.dda-liferay-crate.domain.liferay6 :as liferay6]
    [dda.pallet.dda-liferay-crate.domain.liferay7 :as liferay7]))

; ----- schemas for the domain configuration ----------
(def DomainConfig schema/DomainConfig)

(def DomainConfigResolved schema/DomainConfigResolved)

; --------------  standard configuration values   -----------------------
(def db-name "lportal")

; -----------------  functions  -----------------------
(s/defn ^:always-validate
  db-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys [db-root-passwd db-user-name db-user-passwd]} domain-config]
    {:root-passwd db-root-passwd
     :settings #{}
     :db [{:db-name db-name
           :db-user-name db-user-name
           :db-user-passwd db-user-passwd
           :create-options "character set utf8"}]}))

(s/defn ^:always-validate
  httpd-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys [fq-domain-name google-id settings]} domain-config]
    (merge
      {:domain-name fq-domain-name}
      (when (contains? domain-config :google-id)
        {:google-id google-id})
      (when (contains? domain-config :settings)
        {:settings settings})
      {:alias [{:url "/quiz/" :path "/var/www/static/quiz/"}]
       :jk-unmount [{:path "/quiz/*" :worker "mod_jk_www"}]})))

(s/defn ^:always-validate
  tomcat-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys [liferay-version]} domain-config]
    (case liferay-version
      :LR6 (liferay6/tomcat-domain-configuration domain-config)
      :LR7 (liferay7/tomcat-domain-configuration domain-config))))

(s/defn ^:always-validate
  backup-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys []} domain-config]
    (backup/backup-domain-config domain-config)))

(s/defn ^:always-validate
  infra-configuration :- infra/InfraResult
  [domain-config :- DomainConfigResolved]
  (let [{:keys [liferay-version]} domain-config]
    (case liferay-version
      :LR6 {infra/facility (liferay6/liferay-infra-configuration domain-config db-name)}
      :LR7 {infra/facility (liferay7/liferay-infra-configuration domain-config db-name)})))
