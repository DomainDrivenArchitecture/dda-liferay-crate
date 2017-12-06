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
    [dda.pallet.dda-liferay-crate.domain.backup :as backup]))

(def DomainConfig
  "The high-level domain configuration for the liferay-crate."
  {:fq-domain-name s/Str
   :db-root-passwd secret/Secret
   :db-user-name s/Str
   :db-user-passwd secret/Secret
   ;if :test is specified in :settings, snakeoil certificates will be used
   :settings (hash-set (s/enum :test))
   (s/optional-key :backup) {:bucket-name s/Str
                             :gpg {:gpg-public-key  secret/Secret
                                   :gpg-private-key secret/Secret
                                   :gpg-passphrase  secret/Secret}
                             :aws {:aws-access-key-id secret/Secret
                                   :aws-secret-access-key secret/Secret}}})

(def DomainConfigResolved
  "The high-level domain configuration for the liferay-crate with passwords resolved."
  {:fq-domain-name s/Str
   :db-root-passwd s/Str
   :db-user-name s/Str
   :db-user-passwd s/Str
   :settings (hash-set (s/enum :test))
   (s/optional-key :backup) {:bucket-name s/Str
                             :gpg {:gpg-public-key s/Str
                                   :gpg-private-key s/Str
                                   :gpg-passphrase s/Str}
                             :aws {:aws-access-key-id s/Str
                                   :aws-secret-access-key s/Str}}})

(s/defn ^:always-validate db-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys [db-root-passwd db-user-name db-user-passwd]} domain-config]
    {:root-passwd db-root-passwd
     :settings #{}
     :db [{:db-name backup/db-name
           :db-user-name db-user-name
           :db-user-passwd db-user-passwd}]}))

(s/defn ^:always-validate httpd-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys [fq-domain-name settings]} domain-config]
    {:domain-name fq-domain-name
     :settings (clojure.set/union
                 #{:with-php}
                 settings)}))

(s/defn ^:always-validate backup-domain-configuration
  [domain-config :- DomainConfigResolved]
  (let [{:keys []} domain-config]
    (backup/backup-domain-config domain-config)))

(s/defn ^:always-validate liferay-infra-configuration :- infra/LiferayCrateConfig
  [domain-config :- DomainConfigResolved]
  (let [{:keys [fq-domain-name]} domain-config]
    {:fq-domain-name fq-domain-name}))

(s/defn ^:always-validate infra-configuration :- infra/InfraResult
  [domain-config :- DomainConfigResolved]
  {infra/facility (liferay-infra-configuration domain-config)})
