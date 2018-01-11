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

(ns dda.pallet.dda-liferay-crate.infra.liferay-scripts
  (:require
    [clojure.string :as string]
    [schema.core :as s]
    [schema-tools.core :as st]
    [pallet.stevedore :as stevedore]
    [dda.config.commons.directory-model :as dir-model]
    [dda.pallet.dda-liferay-crate.infra.schema :as schema]))

; ---------------------------  deploy scripts  ---------------------------
(s/defn ^:always-validate do-deploy-script
  "Provides the do-deploy script content."
  [prepare-dir :- dir-model/NonRootDirectory
   deploy-dir :- dir-model/NonRootDirectory
   tomcat-dir] :- dir-model/NonRootDirectory

  (let [application-parts-hot ["hooks" "layouts" "portlets" "themes"]
        ;TODO ext muss hinzugefügt werden / oder ist schon?
        application-parts-full ["app" "hooks" "layouts" "portlets" "themes" "ext"]]
    (stevedore/with-script-language :pallet.stevedore.bash/bash
      (stevedore/with-source-line-comments false
        (stevedore/script
          ;(~lib/declare-arguments [release-dir hot-or-cold])
          ("if [ \"$#\" -eq 0 ]; then")
          (println "\"\"")
          (println "\"Usage is: prepare-rollout [release] [deployment-mode].\"")
          (println "\"  deployment-mode:      [hot|full] hot uses the liferay hot deployment mechanism for deploying portlets, themes, a.s.o.\"")
          (println "\"                                   full restarts tomcat and rolles out the liferay app itself, the configuration and portlets ...\"")
          (println "\"  Available Releases are:\"")
          (pipe (pipe ("find" ~prepare-dir "-mindepth 2 -type d") ("cut -d/ -f6")) ("sort -u"))
          (println "\"\"")
          ("exit 1")
          ("fi")
          ("if [ \"$#\" -ge 3 ]; then")
          (println "\"\"")
          (println "\"Please specify 2 parameters only!\"")
          (println "\"\"")
          ("exit 1")
          ("fi")
          (if (directory? (str ~prepare-dir @1))
            (if (= @2 "hot")
              (do
                (doseq [part ~application-parts-hot]
                  ("cp" (str ~prepare-dir @1 "/" @part "/*") ~deploy-dir))
                ("chown -R tomcat7" (str ~deploy-dir "*")))
              (do
                ("service tomcat7 stop")
                ("rm -rf" (str ~tomcat-dir "*"))
                (doseq [part ~application-parts-full]
                  ("cp" (str ~prepare-dir @1 "/" @part "/*") ~tomcat-dir))
                ("unzip" (str ~tomcat-dir "ROOT.war -d " ~tomcat-dir "ROOT/"))
                ("cp" (str ~prepare-dir @1 "/config/portal-ext.properties") (str ~tomcat-dir "ROOT/WEB-INF/classes/"))
                ("chown tomcat7" (str ~tomcat-dir "*"))
                ("service tomcat7 start")))
            (do
              (println "\"\"")
              (println "\"ERROR: Specified release does not exist or you don't have the permission for it! Please run again as root! For a list of the available releases, run this script without parameters in order to show the available releases!\" ;")
              (println "\"\""))))))))

(s/defn ^:always-validate remove-all-but-specified-versions
  "Removes all other Versions except the specifided Versions"
  [releases :- [schema/LiferayRelease]
   release-dir :- dir-model/NonRootDirectory]
  (let [versions (string/join "|" (map #(str (st/get-in % [:name]) (string/join "." (st/get-in % [:version]))) releases))]
    (stevedore/script
      (pipe (pipe ("ls" ~release-dir) ("grep" "-Ev" ~versions)) ("xargs" "-I {} rm -r" (str ~release-dir "{}"))))))

; -------------------- db replace scripts  ---------------------
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
