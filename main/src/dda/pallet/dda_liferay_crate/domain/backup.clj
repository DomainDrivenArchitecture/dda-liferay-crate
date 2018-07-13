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
(ns dda.pallet.dda-liferay-crate.domain.backup
  (:require
    [schema.core :as s]))

(def os-user
  {:hashed-password "kpwejjj0r04u09rg90rfj"})

(defn backup-domain-config
  [domain-config
   db-name
   tomcat-user]
  (let [{:keys [backup db-root-passwd settings]} domain-config
        {:keys [bucket-name gpg aws duplicity-push]} backup]
    (merge
      {:backup-name      "liferay"
       :backup-user      os-user
       :service-restart  tomcat-user                        ;service-name is identical to user for tomcat
       :local-management {:gens-stored-on-source-system 1}
       :backup-elements
                         (remove nil? [
                                       {:type              :mysql
                                        :name              "liferay_db"
                                        :db-user-name      "root"
                                        :db-user-passwd    db-root-passwd
                                        :db-name           db-name
                                        ;leading space required for db-create-options
                                        :db-create-options " character set utf8"}
                                       {:type        :file-compressed
                                        :name        "var_lib_liferay_data"
                                        :backup-path ["/var/lib/liferay/data/"]
                                        :new-owner   tomcat-user}
                                       (when (not (contains? settings :test))
                                         {:type                       :file-compressed
                                          :name                       "letsencrypt"
                                          :backup-path                ["/etc/letsencrypt/accounts/" "/etc/letsencrypt/csr/" "/etc/letsencrypt/keys/"
                                          "/etc/letsencrypt/renewal/" "/etc/letsencrypt/live/"]})])}
      (when (:backup domain-config)
        {:transport-management {:duplicity-push
                                {:root-password (:root-password duplicity-push)
                                 :public-key    (:gpg-public-key gpg)
                                 :private-key   (:gpg-private-key gpg)
                                 :passphrase    (:gpg-passphrase gpg)
                                 :target-s3     {:aws-access-key-id     (:aws-access-key-id aws)
                                                 :aws-secret-access-key (:aws-secret-access-key aws)
                                                 :bucket-name           bucket-name
                                                 :directory-name        "liferay"}}}}))))

