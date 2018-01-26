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
(ns dda.pallet.dda-liferay-crate.domain.schema
  (:require
    [schema.core :as s]
    [dda.config.commons.version-model :as version]
    [dda.pallet.commons.secret :as secret]))

(def LiferayVersion (s/enum :LR6 :LR7))

(def LiferayApp
  "Represents a liferay application (portlet, theme or the portal itself)."
  [(s/one s/Str "name") (s/one s/Str "url")])

(def LiferayRelease
  "LiferayRelease contains a release name with specification of versioned apps."
  {:name s/Str
   :version version/Version
   (s/optional-key :app) LiferayApp
   (s/optional-key :config) [s/Str]
   (s/optional-key :hooks) [LiferayApp]
   (s/optional-key :layouts) [LiferayApp]
   (s/optional-key :themes) [LiferayApp]
   (s/optional-key :portlets) [LiferayApp]
   (s/optional-key :ext) [LiferayApp]})

(def DomainConfig
  "The high-level domain configuration for the liferay-crate."
  {:liferay-version LiferayVersion
   :fq-domain-name s/Str
   (s/optional-key :fqdn-to-be-replaced) s/Str
   (s/optional-key :google-id) s/Str
   (s/optional-key :tomcat-xmx-megabyte) s/Int
   :db-root-passwd secret/Secret
   :db-user-name s/Str
   :db-user-passwd secret/Secret
   ;if :test is specified in :settings, snakeoil certificates will be used
   :settings (hash-set (s/enum :test))
   (s/optional-key :releases) [LiferayRelease]
   (s/optional-key :backup) {:bucket-name s/Str
                             :gpg {:gpg-public-key  secret/Secret
                                   :gpg-private-key secret/Secret
                                   :gpg-passphrase  secret/Secret}
                             :aws {:aws-access-key-id secret/Secret
                                   :aws-secret-access-key secret/Secret}}})

(def DomainConfigResolved
  "The high-level domain configuration for the liferay-crate with secrets resolved."
  (secret/create-resolved-schema DomainConfig))
