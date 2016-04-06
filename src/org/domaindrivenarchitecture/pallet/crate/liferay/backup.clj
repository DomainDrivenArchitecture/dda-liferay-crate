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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.backup
  (:require
    [org.domaindrivenarchitecture.pallet.crate.backup.backup-lib :as backup-lib]
    [org.domaindrivenarchitecture.pallet.crate.backup.restore-lib :as restore-lib]
    [org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as common-lib]
    ))

(defn liferay-source-backup-script-lines
  ""
  [instance-name db-name db-user-name db-pass]
  (into [] 
        (concat 
          common-lib/head
          common-lib/export-timestamp
          (common-lib/stop-app-server "tomcat7")
          (backup-lib/backup-files-tar 
            :root-dir "/var/lib/liferay/data/" 
            :subdir-to-save "document_library images"
            :app "liferay"
            :instance-name instance-name)
          (backup-lib/backup-files-tar 
            :root-dir "/etc/letsencrypt/"
            :subdir-to-save "accounts csr keys renewal"
            :app "letsencrypt"
            :instance-name instance-name)
          (backup-lib/backup-mysql 
            :db-user db-user-name 
            :db-pass db-pass
            :db-name db-name 
            :app "liferay"
            :instance-name instance-name)
          (common-lib/start-app-server "tomcat7")
          )
        ))

(defn liferay-source-transport-script-lines
  [instance-name generations]
  (backup-lib/source-transport-script-lines 
      :app-name "liferay"
      :instance-name instance-name
      :files-to-transport [:file-compressed :mysql] 
      :gens-stored-on-source-system generations)
  )

(defn liferay-restore-script-lines
  ""
  [instance-name fqdn db-name db-user-name db-pass]
  (into [] 
        (concat 
          common-lib/head
          restore-lib/restore-parameters
          restore-lib/restore-navigate-to-restore-location
          (restore-lib/restore-locate-restore-dumps)
          restore-lib/restore-head
          (common-lib/prefix
            "  "
            (common-lib/stop-app-server "tomcat7"))
          restore-lib/restore-db-head
          ["  # replace location in portal config"
           (str 
             "  sedHttps=\"s/<name>cdn.host.https<\\/name>"
             "<value>https:\\/\\/" fqdn "<\\/value>/"
             "<name>cdn.host.https<\\/name><value>https:\\/\\/"
             fqdn "<\\/value>/\"")
           (str "  sedHttp=\"s/<name>cdn.host.http<\\/name>"
                "<value>http:\\/\\/" fqdn "<\\/value>/"
                "<name>cdn.host.http<\\/name><value>http:\\/\\/"
                fqdn "<\\/value>/\"")
           "  sed -e \"$sedHttps\" ${most_recent_sql_dump} > output1.sql"
            "  sed -e \"$sedHttp\" output1.sql > output2.sql"
            "  "]
          (common-lib/prefix
            "  "
            (restore-lib/restore-mysql 
              :db-user db-user-name 
              :db-pass db-pass 
              :db-name db-name
              :dump-filename "output2.sql"
              :create-options "character set utf8"))
          ["  #db-restore postprocessing"
           (str "  mysql -hlocalhost -u" db-user-name " -p" db-pass 
                " -D" db-name 
                " -e \"update Company set webId = '"
                fqdn "', mx = '"
                fqdn "' where companyId = 10132;\"")
           (str "  mysql -hlocalhost -u" db-user-name " -p" db-pass 
                " -D" db-name 
                " -e \"update VirtualHost set hostname = '"
                fqdn "' where virtualHostId = 35337;\"")
           "  "]
          restore-lib/restore-db-tail
          restore-lib/restore-file-head
          (common-lib/prefix
            "  "
            (restore-lib/restore-tar
              :restore-target-dir "/var/lib/liferay/data"
              :file-type :file-compressed
              :new-owner "tomcat7"))
          (common-lib/prefix
            "  "
            (restore-lib/restore-tar
              :restore-target-dir "/etc/letsencrypt/"
              :file-type :file-compressed
              :new-owner "root"))
          restore-lib/restore-file-tail
          restore-lib/restore-tail
          )
        )
  )