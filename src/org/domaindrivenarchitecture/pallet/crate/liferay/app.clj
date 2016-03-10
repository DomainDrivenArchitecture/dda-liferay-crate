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
     [pallet.actions :as actions]
     [org.domaindrivenarchitecture.pallet.crate.liferay.app-config :as liferay-config]
     [org.domaindrivenarchitecture.pallet.crate.liferay.db-replace-scripts :as db-replace-scripts]))

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
  [liferay-home-dir liferay-lib-dir liferay-release-dir]
  (liferay-dir (str liferay-home-dir "logs"))
  (liferay-dir (str liferay-home-dir "data"))
  (liferay-dir (str liferay-home-dir "deploy"))
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

(defn install-liferay
  [tomcat-root-dir liferay-home-dir liferay-lib-dir liferay-release-dir repo-download-source]
  "creates liferay directories, copies liferay webapp into tomcat and loads dependencies into tomcat"
  (create-liferay-directories liferay-home-dir liferay-lib-dir liferay-release-dir)
  ;; TODO: review mje 2016_03_10: this should go to tomcat crate
  (delete-tomcat-default-ROOT tomcat-root-dir)
  (liferay-dependencies-into-tomcat liferay-lib-dir repo-download-source)
  )

(defn configure-liferay
  [custom-build? & {:keys [db-name db-user-name db-user-passwd
                    portal-ext-properties fqdn-to-be-replaced fqdn-replacement]}]
  (let [effective-portal-ext-properties 
        (if (empty? portal-ext-properties) 
          (liferay-config/var-lib-tomcat7-webapps-ROOT-WEB-INF-classes-portal-ext-properties 
            :db-name db-name
            :db-user-name db-user-name
            :db-user-passwd db-user-passwd)
           portal-ext-properties)]
    
    (liferay-config-file
      (if custom-build?
        "/var/lib/liferay/portal-ext.properties"
        "/var/lib/tomcat7/webapps/ROOT/WEB-INF/classes/portal-ext.properties")
       effective-portal-ext-properties)
    
    (liferay-config-file 
      "/var/lib/liferay/prodDataReplacements.sh"
      (db-replace-scripts/var-lib-liferay-prodDataReplacements-sh
        fqdn-to-be-replaced fqdn-replacement db-name db-user-name db-user-passwd)
      :owner "root" :mode "744"))
  )