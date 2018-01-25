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
    [clojure.string :as string]
    [schema.core :as s]
    [schema-tools.core :as st]
    [pallet.actions :as actions]
    [pallet.stevedore :as stevedore]
    [dda.config.commons.directory-model :as dir-model]
    [dda.pallet.dda-liferay-crate.infra.schema :as schema]
    [dda.pallet.dda-liferay-crate.infra.liferay-scripts :as liferay-scripts]))

; ----------------  general installation functions  -------------
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

(defn- liferay-config-file
  "Install a file with the specified content"
  [file-name content  & {:keys [owner mode]
                         :or {owner "tomcat7" mode "644"}}]
  (actions/remote-file
    file-name
    :owner owner
    :group owner
    :mode mode
    :literal true
    :content (string/join \newline content)))

(defn- liferay-remote-file
  "Install a file  from a URL"
  [file-name download-url & {:keys [owner mode]
                             :or {owner "tomcat7" mode "644"}}]
  (actions/remote-file
    file-name
    :owner owner
    :group owner
    :mode mode
    :insecure true
    :literal true
    :url download-url))

(defn liferay-remote-file-unzip
  "Unzip and install files from a zip from a URL"
  [target-dir download-url owner group]
  (actions/remote-directory
    target-dir
    :url download-url
    :unpack :unzip
    :recursive true
    :owner owner
    :group group))

; ----------------  liferay specific installation functions  -------------
(s/defn create-liferay-directories
  "Create folders required for liferay"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [home-dir lib-dir deploy-dir release-dir tomcat]} config
        {:keys [tomcat-user]} tomcat]
    (liferay-dir (str home-dir "logs") :owner tomcat-user)
    (liferay-dir (str home-dir "data") :owner tomcat-user)
    (liferay-dir deploy-dir :owner tomcat-user)
    (liferay-dir lib-dir :owner tomcat-user)
    (liferay-dir release-dir :owner "root")))

(s/defn liferay-dependencies-into-tomcat
  "get dependency files"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [lib-dir dependencies repo-download-source tomcat]} config
        {:keys [tomcat-user]} tomcat]
    (doseq [jar dependencies]
      (let [download-location (str repo-download-source jar ".jar")
            target-file (str lib-dir jar ".jar")]
        (liferay-remote-file target-file download-location :owner tomcat-user)))))

(s/defn download-and-store-applications
  "download and store liferay applications in given directory"
  [release-dir :- dir-model/NonRootDirectory
   release :- schema/LiferayRelease
   key :- s/Keyword]
  (when
    (contains? release key)
    (let [dir (str release-dir (name key) "/")]
      (liferay-dir dir :owner "root")
      (case key
        :app (let [app (st/get-in release [:app])]
               (liferay-remote-file
                 (str dir (first app) ".war")
                 (second app)
                 :owner "root"))
        :config (liferay-config-file
                  (str dir "portal-ext.properties")
                  (:config release)
                  :owner "root")
        (:hooks :layouts :themes :portlets) (doseq [app (st/get-in release [key])]
                                              (let [app-name (subs (second app) (+ 1 (.lastIndexOf (second app) "/")))]
                                                (liferay-remote-file
                                                  (str dir app-name)
                                                  (second app)
                                                  :owner "root")))))))

(s/defn install-do-rollout-script
  "Creates script for rolling liferay version. To be called by the admin connected to the server via ssh"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [home-dir deploy-dir release-dir tomcat]} config
        {:keys [tomcat-webapps-dir tomcat-user tomcat-service]} tomcat]
    (actions/remote-file
      (str home-dir "do-rollout.sh")
      :owner "root"
      :group "root"
      :mode "0744"
      :literal true
      :content (liferay-scripts/do-deploy-script release-dir deploy-dir tomcat-webapps-dir tomcat-user tomcat-service))))
(s/defn release-name :- s/Str
  "get the release name"
  [release :- schema/LiferayRelease]
  (str (st/get-in release [:name]) "-" (string/join "." (st/get-in release [:version]))))

(s/defn release-dir-name :- dir-model/NonRootDirectory
  "get the release dir name"
  [base-release-dir :- dir-model/NonRootDirectory
   release :- schema/LiferayRelease]
  (str base-release-dir (release-name release) "/"))

(s/defn prepare-rollout
  "prepare the rollout of all liferay releases, i.e. download required libraries"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [release-dir releases]} config]
    (actions/exec-script*
      (liferay-scripts/remove-all-but-specified-versions releases release-dir))
    (doseq [release releases]
      (let [release-subdir (release-dir-name release-dir release)]
        (actions/plan-when-not
          (stevedore/script (directory? ~release-subdir))
          (liferay-dir release-subdir :owner "root")
          (download-and-store-applications release-subdir release :app)
          (download-and-store-applications release-subdir release :config)
          (download-and-store-applications release-subdir release :hooks)
          (download-and-store-applications release-subdir release :layouts)
          (download-and-store-applications release-subdir release :themes)
          (download-and-store-applications release-subdir release :portlets)
          (download-and-store-applications release-subdir release :ext))))))

(s/defn install-liferay
  "dda liferay crate: install routine, creates liferay directories,
  copies liferay webapp into tomcat and loads dependencies into tomcat"
  [config :- schema/LiferayCrateConfig]
  (create-liferay-directories config)
  (liferay-dependencies-into-tomcat config)
  (install-do-rollout-script config)
  (prepare-rollout config))

; ----------------  configure functions  -------------
(s/defn configure-liferay
  "dda liferay crate: configure routine"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [fq-domain-name home-dir db-name db-user-name db-user-passwd tomcat fqdn-to-be-replaced]} config
        {:keys [tomcat-user]} tomcat]
    (liferay-config-file
      (str home-dir "prodDataReplacements.sh")
      (liferay-scripts/var-lib-liferay-prodDataReplacements-sh
        fqdn-to-be-replaced fq-domain-name db-name db-user-name db-user-passwd tomcat-user)
      :owner "root" :mode "744")))
