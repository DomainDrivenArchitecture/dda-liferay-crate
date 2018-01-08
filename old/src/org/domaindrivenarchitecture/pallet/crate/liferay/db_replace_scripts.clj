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


(ns org.domaindrivenarchitecture.pallet.crate.liferay.db-replace-scripts)

(def var-lib-liferay-rsync-sh
  ["#!/bin/bash"
   ""
   "####"
   "# politaktiv script that sync latest files from produktiv"
   "# requirements: rsync & valid ssh key on produktiv server"
   "####"
   "LOGFILE=/var/log/syncBackups.log"
   ""
   "echo \"$(date): syncing this files:\" >> $LOGFILE"
   "echo \"$(ssh portalbackup@83.169.4.222 'find /home/portalbackup/portal/ -ctime -1 -type f')\" >> $LOGFILE"
   ""
   "#only sync files one day old"
   "ssh portalbackup@83.169.4.222 'cd /home/portalbackup/portal/"
   "find . -ctime -1 -type f'  \\"
   "| rsync \\"
   "        --timeout=300 \\"
   "        --recursive \\"
   "        --times \\"
   "        --stats \\"
   "        --progress \\"
   "        --rsh \"ssh\" \\"
   "        --files-from=- \\"
   "        --log-file=$LOGFILE \\"
   "         portalbackup@83.169.4.222:/home/portalbackup/portal/ \\"
   "        /var/lib/liferay/synced_prodbackup"])


(def var-lib-liferay-resetTermsOfUse-sh
  ["#!/bin/bash"
   ""
   (str "mysql lportal_staging --batch --skip-column-names"
        " -hlocalhost -uprod -p2iojl343sv84k -Dlportal"
        " -e \"UPDATE User_ SET agreedToTermsOfUse=0 WHERE agreedToTermsOfUse=1\"")
   "echo \"...Nutzungsbedingungen muessen nun von allen Buergern wieder bestaetigt werden!\""])


(defn var-lib-liferay-prodDataReplacements-sh
  [fqdn-to-be-replaced fqdn-replacement db-name db-user-name db-user-passwd]
  ["#!/bin/bash"
   ""
   "# politaktiv script that replaces the data with productive copy"
   ""
   "# tomcat stop"
   "service tomcat7 stop"
   ""
   "# Ort der Backups"
   "cd /home/dataBackupSource/restore"
   ""
   "# als root auf dev:"
   (str "mysql -hlocalhost -u" db-user-name
        " -p" db-user-passwd
        " -e \"drop database " db-name "\";")
   ""
   "# SQL"
   "most_recent_sql_dump=$(ls -t1 ./liferay_pa-prod_mysql_* | head -n1)"
   ""
   "# nach cdn.http und cdn.https suchen"
   (str "sedHttps=\"s/<name>cdn.host.https<\\/name><value>"
        "https:\\/\\/" fqdn-to-be-replaced "<\\/value>"
        "/<name>cdn.host.https<\\/name><value>"
        "https:\\/\\/" fqdn-replacement "<\\/value>/\"")
   (str "sedHttp=\"s/<name>cdn.host.http<\\/name><value>"
        "http:\\/\\/" fqdn-to-be-replaced "<\\/value>"
        "/<name>cdn.host.http<\\/name><value>"
        "http:\\/\\/" fqdn-replacement "<\\/value>/\"")
   "sed -e \"$sedHttps\" ${most_recent_sql_dump} > output1.sql"
   "sed -e \"$sedHttp\" output1.sql > output2.sql"
   ""
   "# Datenbank laden und anpassen"
   (str "mysql -hlocalhost -u" db-user-name
        " -p" db-user-passwd
        " -e \"create database " db-name
        " character set utf8\";")
   (str "mysql -hlocalhost -u" db-user-name
       " -p" db-user-passwd " " db-name " < output2.sql")
   (str "mysql -hlocalhost -u" db-user-name
       " -p" db-user-passwd
       " -D" db-name
       " -e \"update Company set "
       "webId = 'intermediate.intra.politaktiv.org', mx = 'intermediate.intra.politaktiv.org' "
       "where companyId = 10132;\"")
   (str "mysql -hlocalhost -u" db-user-name
       " -p" db-user-passwd
       " -D" db-name
       " -e \"update VirtualHost set hostname = 'intermediate.intra.politaktiv.org'"
       " where virtualHostId = 35337;\"")
   "echo \"db finished\""
   ""
   "# File"
   "most_recent_file_dump=$(ls -t1 ./liferay_pa-prod_file_* | head -n1)"
   "rm -r /var/lib/liferay/data/*"
   "tar -xzf ${most_recent_file_dump} -C /var/lib/liferay/data"
   "chown -R tomcat7:tomcat7 /var/lib/liferay/data"
   "echo \"file finished\""])
