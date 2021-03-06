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
(ns dda.pallet.dda-liferay-crate.infra.schema
  (:require
   [schema.core :as s]
   [dda.config.commons.version-model :as version]
   [dda.config.commons.directory-model :as dir-model]
   [dda.pallet.dda-liferay-crate.infra.osgi :as osgi]))

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

(def LiferayCrateConfig
  "The infra config schema."
  {:fq-domain-name s/Str
   (s/optional-key :fqdn-to-be-replaced) s/Str
   :home-dir dir-model/NonRootDirectory
   :lib-dir dir-model/NonRootDirectory
   :deploy-dir dir-model/NonRootDirectory
   (s/optional-key :osgi) osgi/OsgiConfig
   :repo-download-source s/Str
   :dependencies [s/Str]
   :release-dir dir-model/NonRootDirectory
   :releases [LiferayRelease]
   :tomcat {:tomcat-root-dir s/Str
            :tomcat-webapps-dir s/Str
            :tomcat-user s/Str
            :tomcat-service s/Str}
   :db {:db-name s/Str
        :db-user-name s/Str
        :db-user-passwd s/Str}})
