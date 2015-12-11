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
     [org.domaindrivenarchitecture.pallet.crate.liferay.db-replace-scripts :as db-replace-scripts]
    ))

(defn- configure-liferay-file
  "Create and upload a config file"
  [file-name content type]
  (let [owner (if (= type :liferay-config) "tomcat7" "root")
        mode (if (= type :sh) "744" "644")]
    (actions/remote-file
      file-name
      :owner owner
      :group owner
      :mode mode
      :force true
      :literal true
      :content 
      (string/join
        \newline
        content)))
  )

(def LIFERAY-HOME "/var/lib/liferay/")
(def LIFERAY-LIB (str LIFERAY-HOME "lib/"))
(def TOMCAT-HOME "/var/lib/tomcat7/")
(def TOMCAT-ROOT (str TOMCAT-HOME "webapps/ROOT/"))


(def liferay-default-download-source 
  "https://tech:flsdjll5Glgflajl@artifacts.meissa-gmbh.de/archiva/repository/internal/liferay/liferay-portal/6.2.1GA2/liferay-portal-6.2.1GA2.war"
  )


(def repo-download-source 
  "https://raw.githubusercontent.com/PolitAktiv/releases/master/liferay/3rd/6.2.1-ce-ga2/")


(defn create-liferay-directories
  []
    (actions/directory 
      (str LIFERAY-HOME "logs")
      :action :create
      :owner "tomcat7"
      :group "tomcat7"
      :mode "755"
      )
    (actions/directory 
      (str LIFERAY-HOME "data")
      :action :create
      :owner "tomcat7"
      :group "tomcat7"
      :mode "755"
      )
    (actions/directory 
      (str LIFERAY-HOME "deploy")
      :action :create
      :owner "tomcat7"
      :group "tomcat7"
      :mode "755"
      )
    (actions/directory
      LIFERAY-LIB
      :action :create
      :owner "tomcat7"
      :group "tomcat7"
      :mode "755"
      )
  )

; TODO: review mje 18.08: Das ist tomcat spezifisch und gehört hier raus.
(defn delete-tomcat-default-ROOT
  []
  (actions/directory
    TOMCAT-ROOT
    :action :delete
    :recursive true)
  )

(defn liferay-portal-into-tomcat
  "make liferay tomcat's ROOT webapp"
  [& {:keys [liferay-download-source]
      :or {liferay-download-source liferay-default-download-source}}]
  (actions/remote-directory 
    TOMCAT-ROOT
    :url liferay-download-source
    :unpack :unzip
    :recursive true
    :owner "tomcat7"
    :group "tomcat7")
  )

(defn liferay-dependencies-into-tomcat
  []
  "get dependency files" 
  (doseq [jar ["activation" "ccpp" "hsql" "jms" 
               "jta" "jtds" "junit" "jutf7" "mail" 
               "mysql" "persistence" "portal-service" 
               "portlet" "postgresql" "support-tomcat"]]
    (let [download-location (str repo-download-source jar ".jar")
          target-file (str LIFERAY-LIB jar ".jar")]
      (actions/remote-file 
        target-file
        :url download-location
        :owner "tomcat7"
        :group "tomcat7"
        :insecure true
        :mode 644)
      ))
)

(defn install-liferay
  [& {:keys [custom-build? liferay-download-source]
      :or {costom-build? false}}]
  "creates liferay directories, copies liferay webapp into tomcat and loads dependencies into tomcat"
  (create-liferay-directories)
  (delete-tomcat-default-ROOT)
  ; TODO review mje 24.09.: Found bug here - if we use build lr, we wont use lr download!
  ; besides - without parameter this is expectionally wrong.
  (if (not custom-build?)
    (liferay-portal-into-tomcat
      :liferay-download-source liferay-download-source)) 
  (liferay-dependencies-into-tomcat)
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
    (configure-liferay-file
      (if custom-build?
        "/var/lib/liferay/portal-ext.properties"
        "/var/lib/tomcat7/webapps/ROOT/WEB-INF/classes/portal-ext.properties")
      effective-portal-ext-properties 
      :liferay-config)
    (configure-liferay-file 
      "/var/lib/liferay/prodDataReplacements.sh"
      (db-replace-scripts/var-lib-liferay-prodDataReplacements-sh
        fqdn-to-be-replaced fqdn-replacement db-name db-user-name db-user-passwd)
      :sh))
  )