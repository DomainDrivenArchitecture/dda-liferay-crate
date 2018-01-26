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

(ns dda.pallet.dda-liferay-crate.domain.liferay
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

; --------------  standard configuration values   -----------------------
(def liferay-home-dir "/var/lib/liferay/")

; ---  functions to create other configs from the liferay domain config  --
(s/defn tomcat-domain-configuration
  [domain-config :- DomainConfigResolved liferay-version]
  (let [{:keys [tomcat-xmx-megabyte] :or {tomcat-xmx-megabyte 2560}} domain-config]
    {(case liferay-version
       :LR7 :lr-7x
       :LR6 :lr-6x)
     {:xmx-megabbyte tomcat-xmx-megabyte            ; e.g. 6072 or 2560
      :lr-home liferay-home-dir}}))

(s/defn liferay-infra-configuration :- infra/LiferayCrateConfig
  [domain-config :- DomainConfigResolved
   db-name :- s/Str
   liferay-version]
  (let [{:keys [fq-domain-name fqdn-to-be-replaced db-user-name db-user-passwd releases]} domain-config
        tomcat-user (case liferay-version
                             :LR7 "tomcat8"
                             :LR6 "tomcat7")
        add-dependencies (case liferay-version
                             :LR7 ["mariadb"
                                   "com.liferay.osgi.service.tracker.collections-2.0.3"
                                   "portal-kernel" "com.liferay.registry.api" "jms"]
                             :LR6 ["portal-service" "jtds" "junit"])
        add-config (case liferay-version
                             :LR7 {:repo-download-source "https://github.com/PolitAktiv/releases/releases/download/7.0.x/"
                                   :osgi {:download-url "https://github.com/PolitAktiv/releases/releases/download/7.0.x/osgi.zip"
                                          :dir liferay-home-dir
                                          :os-user tomcat-user}}
                             :LR6 {:repo-download-source "https://github.com/PolitAktiv/releases/releases/download/6.2.x/"})]
    (merge
      add-config
      {:fq-domain-name fq-domain-name
       :home-dir liferay-home-dir
       :lib-dir (str liferay-home-dir "lib/")
       :deploy-dir (str liferay-home-dir "deploy/")
       :release-dir (str liferay-home-dir "prepare-rollout/")
       :dependencies (vec (concat
                            ["activation" "ccpp" "hsql" "jms"
                             "jta" "jutf7" "mail"
                             "mysql" "persistence"
                             "portlet" "postgresql" "support-tomcat"]
                            add-dependencies))
       :releases
       (if (contains? domain-config :releases)
         releases
         ; else use default release
         [(liferay-config/default-release-config domain-config db-name liferay-version)])
       :tomcat {:tomcat-root-dir (str "/usr/share/" tomcat-user "/")
                :tomcat-webapps-dir (str "/var/lib/" tomcat-user "/webapps/")
                :tomcat-user tomcat-user
                :tomcat-service tomcat-user}
       :db {:db-name db-name
            :db-user-name db-user-name
            :db-user-passwd db-user-passwd}}
      (when (contains? domain-config :fqdn-to-be-replaced)
            {:fqdn-to-be-replaced fqdn-to-be-replaced}))))
