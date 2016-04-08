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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.app
   (:require
     [clojure.string :as string]
     [schema.core :as s]
     [schema-tools.core :as st]
     [pallet.actions :as actions]
     [schema.experimental.complete :as c]
     [org.domaindrivenarchitecture.config.commons.directory-model :as dir-model]
     [org.domaindrivenarchitecture.pallet.crate.liferay.release-model :as schema]
     [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as app-config]
     [org.domaindrivenarchitecture.pallet.crate.liferay.db-replace-scripts :as db-replace-scripts]
     [pallet.stevedore :as stevedore]
     [pallet.script.scriptlib :as lib]
     [pallet.stevedore.bash :as bash]))

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
    :content (string/join \newline content))
  )

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
    :url url)
  )

(defn- liferay-dir
  "Create and upload a config file"
  [dir-path & {:keys [owner mode]
            :or {owner "tomcat7"
                 mode "755"}}]
  (actions/directory 
      dir-path
      :action :create
      :recursive true
      :owner owner
      :group owner
      :mode mode)
  )

(defn create-liferay-directories
  [liferay-home-dir liferay-lib-dir liferay-release-dir liferay-deploy-dir]
  (liferay-dir (str liferay-home-dir "logs"))
  (liferay-dir (str liferay-home-dir "data"))
  (liferay-dir liferay-deploy-dir)
  (liferay-dir liferay-lib-dir)
  (liferay-dir liferay-release-dir :owner "root")
  )

; TODO: review mje 18.08: Das ist tomcat spezifisch und gehört hier raus.
(defn delete-tomcat-default-ROOT
  [tomcat-root-dir]
  (actions/directory
    tomcat-root-dir
    :action :delete
    :recursive true)
  )

(defn liferay-portal-into-tomcat
  "make liferay tomcat's ROOT webapp"
  [tomcat-root-dir liferay-download-source]
  (actions/remote-directory 
    tomcat-root-dir
    :url liferay-download-source
    :unpack :unzip
    :recursive true
    :owner "tomcat7"
    :group "tomcat7")
  )

(defn liferay-dependencies-into-tomcat
  [liferay-lib-dir repo-download-source]
  "get dependency files" 
  (doseq [jar ["activation" "ccpp" "hsql" "jms" 
               "jta" "jtds" "junit" "jutf7" "mail" 
               "mysql" "persistence" "portal-service" 
               "portlet" "postgresql" "support-tomcat"]]
    (let [download-location (str repo-download-source jar ".jar")
          target-file (str liferay-lib-dir jar ".jar")]
      (liferay-remote-file target-file download-location)))
  )

(s/defn ^:allwas-validate release-name :- s/Str
  "get the release dir name"
  [release :- schema/LiferayRelease ]
  (str (st/get-in release [:name]) "-" (string/join "." (st/get-in release [:version]))))

(s/defn ^:allwas-validate release-dir :- dir-model/NonRootDirectory
  "get the release dir name"
  [base-release-dir :- dir-model/NonRootDirectory
   release :- schema/LiferayRelease ]
  (str base-release-dir (release-name release) "/"))

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
                  (st/get-in release [:config]))
        (doseq [app (st/get-in release [key])]
          (liferay-remote-file 
            (str dir (first app) ".war") 
            (second app)
            :owner "root"))))
    ))

(s/defn ^:always-validate install-do-rollout-script
  "Creates script for rolling liferay version. To be called by the admin connected to the server via ssh"
  [liferay-home :- dir-model/NonRootDirectory
   prepare-dir :- dir-model/NonRootDirectory 
   deploy-dir :- dir-model/NonRootDirectory
   tomcat-dir :- dir-model/NonRootDirectory]
  (actions/remote-file
    (str liferay-home "do-rollout.sh")
    :owner "root"
    :group "root"
    :mode "0744"
    :literal true
    :content (app-config/do-deploy-script prepare-dir deploy-dir tomcat-dir)
    ))

(s/defn ^:allwas-validate remove-all-but-specified-versions
  "Removes all other Versions except the specifided Versions"
  [releases :- [schema/LiferayRelease] 
   release-dir :- dir-model/NonRootDirectory]
  (let [versions (string/join "|" (map #(str (st/get-in % [:name]) (string/join "." (st/get-in % [:version]))) releases))]
    (stevedore/script 
      (pipe (pipe ("ls" ~release-dir) ("grep -Ev" ~versions)) ("xargs -I {} rm -r" (str ~release-dir "{}")))
      )))

(s/defn ^:always-validate prepare-rollout 
  "prepare the rollout of all releases"
  [release-config :- schema/LiferayReleaseConfig]
  (let [base-release-dir (st/get-in release-config [:release-dir])
        releases (st/get-in release-config [:releases])]
    (actions/exec-script*
      (remove-all-but-specified-versions releases base-release-dir))
    (doseq [release releases]
      (let [release-dir (release-dir base-release-dir release)]
        (actions/plan-when-not
          (stevedore/script (directory? ~release-dir)) 
          (liferay-dir release-dir :owner "root")
          (download-and-store-applications release-dir release :app)
          (download-and-store-applications release-dir release :config)
          (download-and-store-applications release-dir release :hooks)
          (download-and-store-applications release-dir release :layouts)
          (download-and-store-applications release-dir release :themes)
          (download-and-store-applications release-dir release :portlets)
          )))
    ))


(s/defn install-liferay
  [tomcat-root-dir tomcat-webapps-dir liferay-home-dir 
   liferay-lib-dir liferay-deploy-dir repo-download-source 
   liferay-release-config :- schema/LiferayReleaseConfig]
  "creates liferay directories, copies liferay webapp into tomcat and loads dependencies into tomcat"
  (create-liferay-directories liferay-home-dir liferay-lib-dir (st/get-in liferay-release-config [:release-dir]) liferay-deploy-dir)
  (liferay-dependencies-into-tomcat liferay-lib-dir repo-download-source)
  (liferay-dependencies-into-tomcat liferay-lib-dir repo-download-source)
  (install-do-rollout-script liferay-home-dir (st/get-in liferay-release-config [:release-dir]) liferay-deploy-dir tomcat-webapps-dir)
  )

(defn configure-liferay
  [custom-build? & {:keys [db-name db-user-name db-user-passwd
                    fqdn-to-be-replaced fqdn-replacement]}]    
    (liferay-config-file 
      "/var/lib/liferay/prodDataReplacements.sh"
      (db-replace-scripts/var-lib-liferay-prodDataReplacements-sh
        fqdn-to-be-replaced fqdn-replacement db-name db-user-name db-user-passwd)
      :owner "root" :mode "744"))