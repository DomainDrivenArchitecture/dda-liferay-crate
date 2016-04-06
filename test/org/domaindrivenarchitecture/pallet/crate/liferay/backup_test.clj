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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.backup-test
  (:require
    [clojure.test :refer :all]
    [pallet.actions :as actions]
    [org.domaindrivenarchitecture.pallet.crate.liferay.backup :as sut]
    ))

(deftest backupscript
  (testing 
    "script content"
    (is (= ["#!/bin/bash"
           ""
           "#timestamp from server to variable"
           "export timestamp=`date +%Y-%m-%d_%H-%M-%S`"
           ""
           "#stop appserver"
           "service tomcat7 stop"
           ""
           "#backup the files"
           "cd /var/lib/liferay/data/"
           "tar cvzf /home/dataBackupSource/transport-outgoing/liferay_default-instance_file_${timestamp}.tgz document_library images"
           "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/default-instance_liferay_file_${timestamp}.tgz"
           ""
           "#backup the files"
           "cd /etc/letsencrypt/"
           "tar cvzf /home/dataBackupSource/transport-outgoing/letsencrypt_default-instance_file_${timestamp}.tgz accounts csr keys renewal"
           "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/default-instance_letsencrypt_file_${timestamp}.tgz"
           ""
           "#backup db"
           "mysqldump --no-create-db=true -h localhost -u db-user-name -pdb-pass db-name > /home/dataBackupSource/transport-outgoing/liferay_default-instance_mysql_${timestamp}.sql"
           "chown dataBackupSource:dataBackupSource /home/dataBackupSource/transport-outgoing/default-instance_liferay_mysql_${timestamp}.sql"
           ""
           "#start appserver"
           "service tomcat7 start"
           ""]
           (sut/liferay-source-backup-script-lines "default-instance" "db-name" "db-user-name" "db-pass")))
    ))

(deftest restore-script
  (testing 
    "script content"
    (is (= ["#!/bin/bash"
            ""
            "if [ -z \"$1\" ]; then"
            "  echo \"\""
            "  echo \"usage:\""
            "  echo \"restore.sh [file_name_prefix]\""
            "  echo \"  file_name_prefix: mandantory, the file name prefix for the files to restore like 'liferay_pa-prod'.\""
            "  echo \"\""
            "  echo \"Example 'restore.sh pa-prod' will use the newest backup-files with the pattern iferay_pa-prod_mysql_* and iferay_pa-prod_file_*\""
            "  exit 1"
            "fi"
            ""
            "# cd to restore location"
            "cd /home/dataBackupSource/restore"
            ""
            "# Get the dumps"
            "most_recent_liferay_sql_dump=$(ls -d -t1 $1liferay_mysql_* | head -n1)"
            "most_recent_liferay_file_dump=$(ls -d -t1 $1liferay_file_* | head -n1)"
            "most_recent_letsencrypt_file_dump=$(ls -d -t1 $1letsencrypt_file_* | head -n1)"
            ""
            "echo \"using this inputs:\""
            "echo \"$most_recent_liferay_sql_dump\""
            "echo \"$most_recent_liferay_file_dump\""
            "echo \"$most_recent_letsencrypt_file_dump\""
            ""
            "if [ \"$most_recent_liferay_sql_dump\" ] && [ \"$most_recent_liferay_file_dump\" ] && [ \"$most_recent_letsencrypt_file_dump\" ]; then"
            "  echo \"starting restore\""
            "  "
            "  #stop appserver"
            "  service tomcat7 stop"
            "  "
            "  # ------------- restore db --------------"
            "  echo \"db restore ...\""
            "  "
            "  # replace location in portal config"
            "  sedHttps=\"s/<name>cdn.host.https<\\/name><value>https:\\/\\/fqdn<\\/value>/<name>cdn.host.https<\\/name><value>https:\\/\\/fqdn<\\/value>/\""
            "  sedHttp=\"s/<name>cdn.host.http<\\/name><value>http:\\/\\/fqdn<\\/value>/<name>cdn.host.http<\\/name><value>http:\\/\\/fqdn<\\/value>/\""
            "  sed -e \"$sedHttps\" ${most_recent_liferay_sql_dump} > output1.sql"
            "  sed -e \"$sedHttp\" output1.sql > output2.sql"
            "  "
            "  mysql -hlocalhost -udb-user-name -pdb-pass -e \"drop database db-name\";"
            "  mysql -hlocalhost -udb-user-name -pdb-pass -e \"create database db-name character set utf8\";"
            "  mysql -hlocalhost -udb-user-name -pdb-pass db-name < output2.sql"
            "  "
            "  #db-restore postprocessing"
            "  mysql -hlocalhost -udb-user-name -pdb-pass -Ddb-name -e \"update Company set webId = 'fqdn', mx = 'fqdn' where companyId = 10132;\""
            "  mysql -hlocalhost -udb-user-name -pdb-pass -Ddb-name -e \"update VirtualHost set hostname = 'fqdn' where virtualHostId = 35337;\""
            "  "
            "  echo \"finished db restore\""
            "  "
            "  # ------------- restore file --------------"
            "  echo \"file restore ...\""
            "  "
            "  rm -r /var/lib/liferay/data/*"
            "  tar -xzf ${most_recent_liferay_file_dump} -C /var/lib/liferay/data"
            "  chown -R tomcat7:tomcat7 /var/lib/liferay/data"
            "  "
            "  rm -r /etc/letsencrypt/*"
            "  tar -xzf ${most_recent_letsencrypt_file_dump} -C /etc/letsencrypt/"
            "  chown -R root:root /etc/letsencrypt/"
            "  "
            "  echo \"finished file restore.\""
            "  "
            "  echo \"finished restore successfull, pls. start the appserver.\""
            "fi"
            ""]
           (sut/liferay-restore-script-lines "default-instance" "fqdn" "db-name" "db-user-name" "db-pass")))
    ))
