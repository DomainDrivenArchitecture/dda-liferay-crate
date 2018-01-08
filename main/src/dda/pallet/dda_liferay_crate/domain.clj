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
    [dda.pallet.dda-liferay-crate.domain.schema :as schema]
    [dda.pallet.dda-liferay-crate.domain.liferay-config :as liferay-config]
    [dda.pallet.dda-liferay-crate.domain.backup :as backup]))

; ----- schemas for the high-level domain configuration ----------
(def DomainConfig schema/DomainConfig)

(def DomainConfigResolved schema/DomainConfigResolved)

;  functions to create other domain configs from the liferay domain config
(s/defn ^:always-validate db-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys [db-root-passwd db-user-name db-user-passwd]} domain-config]
    {:root-passwd db-root-passwd
     :settings #{}
     :db [{:db-name backup/db-name
           :db-user-name db-user-name
           :db-user-passwd db-user-passwd}]}))

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

(s/defn ^:always-validate backup-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys []} domain-config]
    (backup/backup-domain-config domain-config)))

; deprecated
(s/defn default-infra-config
  [domain-config :- DomainConfigResolved]
  {:instance-name "default"
   :home-dir "/var/lib/liferay/"
   :lib-dir "/var/lib/liferay/lib/"
   :deploy-dir "/var/lib/liferay/deploy/"
   :release-dir "/var/lib/liferay/prepare-rollout/"
   :releases [(liferay-config/default-release-config domain-config)]})

(s/defn ^:always-validate liferay-infra-configuration :- infra/LiferayCrateConfig
  [domain-config :- DomainConfigResolved]
  (let [{:keys [fq-domain-name]} domain-config]
    ;TODO replace hard coded values of tomcat ?
    {:home-dir "/var/lib/liferay/"
     :lib-dir "/var/lib/liferay/lib/"
     :deploy-dir "/var/lib/liferay/deploy/"
     :repo-download-source "http://ufpr.dl.sourceforge.net/project/lportal/Liferay%20Portal/6.2.1%20GA2/liferay-portal-6.2-ce-ga2-20140319114139101.war"
     :release-dir "/var/lib/liferay/prepare-rollout/"
     :releases [(liferay-config/default-release-config domain-config)]
     :tomcat-root-dir "/usr/share/tomcat7/"
     :tomcat-webapps-dir "webapps/"}))

(s/defn ^:always-validate infra-configuration :- infra/InfraResult
  [domain-config :- DomainConfigResolved]
  {infra/facility (liferay-infra-configuration domain-config)})
