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

(ns dda.pallet.dda-liferay-crate.domain.liferay7
  (:require
    [schema.core :as s]
    [dda.config.commons.map-utils :as mu]
    [dda.pallet.commons.secret :as secret]
    [dda.pallet.dda-liferay-crate.infra :as infra]
    [dda.pallet.dda-liferay-crate.domain.schema :as schema]
    [dda.pallet.dda-liferay-crate.domain.liferay7-config :as liferay-config]
    [dda.pallet.dda-liferay-crate.domain.backup :as backup]))

; ----- schemas for the high-level domain configuration ----------
(def DomainConfig schema/DomainConfig)

(def DomainConfigResolved schema/DomainConfigResolved)

; --------------  standard configuration values   -----------------------
(def liferay-home-dir "/var/lib/liferay/")

(def db-name "lportal")

(def tomcat-version "8")
(def tomcat-user "tomcat8")

; ---  functions to create other configs from the liferay domain config  --
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
  {:lr-7x {:xmx-megabbyte 2560            ; e.g. 6072 or 2560
           :lr-home liferay-home-dir}}) ; e.g. /var/lib/liferay

(s/defn ^:always-validate
  backup-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys []} domain-config]
    (backup/backup-domain-config domain-config)))

(s/defn ^:always-validate
  liferay-infra-configuration :- infra/LiferayCrateConfig
  [domain-config :- DomainConfigResolved]
  (let [{:keys [fq-domain-name db-user-name db-user-passwd]} domain-config]
    ;TODO replace hard coded values of tomcat ?
    {:fq-domain-name fq-domain-name
     :home-dir liferay-home-dir
     :lib-dir (str liferay-home-dir "lib/")
     :deploy-dir (str liferay-home-dir "deploy/")
     :repo-download-source "https://github.com/PolitAktiv/releases/releases/download/7.0.x/"
     :release-dir (str liferay-home-dir "prepare-rollout/")
     :dependencies ["activation" "ccpp" "hsql" "jms"
                    "jta" "jutf7" "mail"
                    "mysql" "persistence"
                    "portlet" "postgresql" "support-tomcat"
                    ; LR7 specifi
                    "mariadb"
                    "com.liferay.osgi.service.tracker.collections-2.0.3"
                    "portal-kernel" "com.liferay.registry.api" "jms"]
     :osgi {:download-url "https://github.com/PolitAktiv/releases/releases/download/7.0.x/osgi.zip"
            :dir liferay-home-dir
            :os-user "tomcat8"}
     :releases [(liferay-config/default-release-config domain-config db-name)]
     :tomcat {:tomcat-root-dir (str "/usr/share/tomcat8/")
              :tomcat-webapps-dir "/var/lib/tomcat8/webapps/"
              :tomcat-user tomcat-user
              :tomcat-service tomcat-user}
     :db {:db-name db-name
          :db-user-name db-user-name
          :db-user-passwd db-user-passwd}}))

(s/defn ^:always-validate infra-configuration :- infra/InfraResult
  [domain-config :- DomainConfigResolved]
  {infra/facility (liferay-infra-configuration domain-config)})
