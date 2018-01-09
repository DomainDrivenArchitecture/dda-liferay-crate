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
  (let [{:keys [home-dir
                lib-dir
                deploy-dir
                release-dir]} config]
    (liferay-dir (str home-dir "logs"))
    (liferay-dir (str home-dir "data"))
    (liferay-dir deploy-dir)
    (liferay-dir lib-dir)
    (liferay-dir release-dir :owner "root")))

(defn- liferay-config-file
  "Create and upload a config file"
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
  "Create and upload a config file"
  [file-name url & {:keys [owner mode]
                    :or {owner "tomcat7" mode "644"}}]
  (actions/remote-file
    file-name
    :owner owner
    :group owner
    :mode mode
    :insecure true
    :literal true
    :url url))

(s/defn liferay-dependencies-into-tomcat
  "get dependency files"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [lib-dir
                repo-download-source]} config]
    (doseq [jar ["activation" "ccpp" "hsql" "jms"
                 "jta" "jtds" "junit" "jutf7" "mail"
                 "mysql" "persistence" "portal-service"
                 "portlet" "postgresql" "support-tomcat"]]
      (let [download-location (str repo-download-source jar ".jar")
            target-file (str lib-dir jar ".jar")]
        (liferay-remote-file target-file download-location)))))

(s/defn ^:always-validate download-and-store-applications
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
                  (:config release))
        (:hooks :layouts :themes :portlets) (doseq [app (st/get-in release [key])]
                                              (let [app-name (subs (second app) (+ 1 (.lastIndexOf (second app) "/")))]
                                                (liferay-remote-file
                                                  (str dir app-name)
                                                  (second app)
                                                  :owner "root")))))))

(s/defn ^:always-validate install-do-rollout-script
  "Creates script for rolling liferay version. To be called by the admin connected to the server via ssh"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [home-dir deploy-dir release-dir tomcat]} config]
    (actions/remote-file
      (str home-dir "do-rollout.sh")
      :owner "root"
      :group "root"
      :mode "0744"
      :literal true
      :content (liferay-scripts/do-deploy-script release-dir deploy-dir (:tomcat-webapps-dir tomcat)))))

(s/defn ^:always-validate remove-all-but-specified-versions
  "Removes all other Versions except the specifided Versions"
  [releases :- [schema/LiferayRelease]
   release-dir :- dir-model/NonRootDirectory]
  (let [versions (string/join "|" (map #(str (st/get-in % [:name]) (string/join "." (st/get-in % [:version]))) releases))]))
    ;TODO (stevedore/script
    ;       (pipe (pipe ("ls" ~release-dir) ("grep -Ev" ~versions)) ("xargs -I {} rm -r" (str ~release-dir "{}")))))

(s/defn ^:always-validate release-name :- s/Str
  "get the release name"
  [release :- schema/LiferayRelease]
  (str (st/get-in release [:name]) "-" (string/join "." (st/get-in release [:version]))))

(s/defn ^:always-validate release-dir-name :- dir-model/NonRootDirectory
  "get the release dir name"
  [base-release-dir :- dir-model/NonRootDirectory
   release :- schema/LiferayRelease]
  (str base-release-dir (release-name release) "/"))

(s/defn ^:always-validate prepare-rollout
  "prepare the rollout of all liferay releases, i.e. download required libraries"
  [config :- schema/LiferayCrateConfig]
;  (let [base-release-dir (st/get-in release-config [:release-dir])
;        releases (st/get-in release-config [:releases])
  (let [{:keys [release-dir releases]} config]
    ; TODO: remove as soon as tomcat-crate is integrated
    (actions/package "tomcat7")
    (actions/exec-script*
      (remove-all-but-specified-versions releases release-dir))
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
  ;TODO from archiva (liferay-dependencies-into-tomcat config)
  (install-do-rollout-script config)
  (prepare-rollout config))

; ----------------  configure functions  -------------
(s/defn configure-liferay
  "dda liferay crate: configure routine"
  [config :- schema/LiferayCrateConfig]
  (let [{:keys [fq-domain-name home-dir db-name db-user-name db-user-passwd]}
        config fqdn-to-be-replaced "fqdn-to-be-replaced"] ;TODO resolve fqdn-to-be-replaced
    (liferay-config-file
      (str home-dir "prodDataReplacements.sh")
      (liferay-scripts/var-lib-liferay-prodDataReplacements-sh
        fqdn-to-be-replaced fq-domain-name db-name db-user-name db-user-passwd)
      :owner "root" :mode "744")))
