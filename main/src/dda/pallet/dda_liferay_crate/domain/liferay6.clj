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

(ns dda.pallet.dda-liferay-crate.domain.liferay6
  (:require
    [schema.core :as s]
    [dda.config.commons.map-utils :as mu]
    [dda.pallet.commons.secret :as secret]
    [dda.pallet.dda-liferay-crate.infra :as infra]
    [dda.pallet.dda-liferay-crate.domain.schema :as schema]
    [dda.pallet.dda-liferay-crate.domain.liferay6-config :as liferay-config]
    [dda.pallet.dda-liferay-crate.domain.backup :as backup]))

; ----- schemas for the high-level domain configuration ----------
(def DomainConfig schema/DomainConfig)

(def DomainConfigResolved schema/DomainConfigResolved)

; --------------  standard configuration values   -----------------------
(def liferay-home-dir "/var/lib/liferay/")

(def tomcat-user "tomcat7")

; ---  functions to create other configs from the liferay domain config  --
(s/defn tomcat-domain-configuration
  [domain-config :- DomainConfigResolved]
  {:lr-6x {:xmx-megabbyte 2560            ; e.g. 6072 or 2560
           :lr-home liferay-home-dir}})

(s/defn liferay-infra-configuration :- infra/LiferayCrateConfig
  [domain-config :- DomainConfigResolved
   db-name :- s/Str]
  (let [{:keys [fq-domain-name db-user-name db-user-passwd]} domain-config]
    {:fq-domain-name fq-domain-name
     :home-dir liferay-home-dir
     :lib-dir (str liferay-home-dir "lib/")
     :deploy-dir (str liferay-home-dir "deploy/")
     :repo-download-source "https://github.com/PolitAktiv/releases/releases/download/6.2.x/"
     :dependencies ["activation" "ccpp" "hsql" "jms"
                    "jta" "jutf7" "mail"
                    "mysql" "persistence"
                    "portlet" "postgresql" "support-tomcat"
                    "portal-service" "jtds" "junit"]
     :release-dir (str liferay-home-dir "prepare-rollout/")
     :releases [(liferay-config/default-release-config domain-config db-name)]
     :tomcat {:tomcat-root-dir (str "/usr/share/" tomcat-user "/")
              :tomcat-webapps-dir (str "/var/lib/" tomcat-user "/webapps/")
              :tomcat-user tomcat-user
              :tomcat-service tomcat-user}
     :db {:db-name db-name
          :db-user-name db-user-name
          :db-user-passwd db-user-passwd}}))
