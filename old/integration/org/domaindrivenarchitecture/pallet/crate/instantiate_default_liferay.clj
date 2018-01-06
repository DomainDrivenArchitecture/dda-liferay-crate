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
(ns org.domaindrivenarchitecture.pallet.crate.instantiate-default-liferay
  (:require
      [pallet.api :as api]      
      [pallet.compute :as compute]
      [pallet.compute.node-list :as node-list]
      [org.domaindrivenarchitecture.pallet.crate.config :as config]
      [org.domaindrivenarchitecture.pallet.crate.init :as init]
      [org.domaindrivenarchitecture.pallet.crate.liferay :as liferay]
      [org.domaindrivenarchitecture.pallet.core.cli-helper :as cm-base])
  (:gen-class :main true))
 
(def minimal-config
  (let [db-user-passwd "user1234"
        db-name "lportal"
        db-user-name "liferay_user"
        db-config {:root-passwd "root1234"
                   :db-name db-name
                   :user-name db-user-name
                   :user-passwd db-user-passwd}]
    {:host-name "test" 
     :domain-name "meissa-gmbh.de" 
     :pallet-cm-user-name "initial"
     :pallet-cm-user-password "test1234"
     :additional-config 
     {:dda-liferay 
      {:third-party-download-root-dir "https://raw.githubusercontent.com/PolitAktiv/releases/master/liferay/3rd/6.2.1-ce-ga2/"
       :httpd 
       {:vhosts
        {:default
         {:domain-name "test.meissa-gmbh.de"
          :google-id "xxxxxxxxxxxxxxxxxxxxx"
          :cert-letsencrypt {:letsencrypt-mail "admin@meissa-gmbh.de"}}}}
       }}}
    ))

(def config
  {:node-specific-config
   {:default-instance minimal-config}})

(def provider
  (compute/instantiate-provider
    "node-list"
    :node-list [["liferay" "liferay-group" "185.48.118.79" :ubuntu :id :default-instance]]))

(def liferay-group
  (api/group-spec
    "liferay-group"
    :extends 
    [(config/with-config config)
     init/with-init
     liferay/with-liferay]))

(defn init-node
  [& {:keys [id group-spec]}]
  (cm-base/execute-init-node 
    :id id 
    :group-spec group-spec
    :config config
    :provider provider))

(defn -main
  [group-spec & {:keys [phase]
                 :or {phase '(:settings :configure)}}]
  (cm-base/execute-main 
    group-spec 
    provider 
    :phase phase))