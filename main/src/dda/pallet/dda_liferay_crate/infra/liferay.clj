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
(ns dda.pallet.dda-liferay-crate.infra.liferay
  (:require
   [schema.core :as s]
   [pallet.actions :as actions]
   [dda.pallet.dda-liferay-crate.infra.schema :as schema]))

; ----------------  functions for the installation   -------------
(defn- liferay-dir
  "Create a single folder"
  [dir-path & {:keys [owner mode]
               :or {owner "tomcat7"
                    mode "755"}}]
  (actions/directory
      dir-path
      :action :create
      :recursive true
      :owner owner
      :group owner
      :mode mode))

(s/defn create-liferay-directories
  "Create folders required for liferay"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [liferay-home-dir
                liferay-lib-dir
                liferay-deploy-dir
                repo-download-source]} config]
    ;[liferay-home-dir liferay-lib-dir liferay-release-dir liferay-deploy-dir]
    (liferay-dir (str liferay-home-dir "logs"))
    (liferay-dir (str liferay-home-dir "data"))
    (liferay-dir liferay-deploy-dir)
    (liferay-dir liferay-lib-dir)))
;    (liferay-dir liferay-release-dir :owner "root")))

(s/defn install-liferay
  "dda liferay crate: install routine, creates liferay directories,
  copies liferay webapp into tomcat and loads dependencies into tomcat"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [tomcat-root-dir
                tomcat-webapps-dir
                liferay-home-dir
                liferay-lib-dir
                liferay-deploy-dir
                repo-download-source]} config]
    (create-liferay-directories liferay-home-dir liferay-lib-dir))) ;(st/get-in liferay-release-config [:release-dir]) liferay-deploy-dir)))
;    (liferay-dependencies-into-tomcat liferay-lib-dir repo-download-source)
;    (install-do-rollout-script liferay-home-dir (st/get-in liferay-release-config [:release-dir]) liferay-deploy-dir tomcat-webapps-dir)))

; ----------------  functions for the configuration   -------------
(s/defn configure-liferay
  [config :- schema/LiferayCrateConfig]
  "dda liferay crate: configure routine")
  ;TODO
