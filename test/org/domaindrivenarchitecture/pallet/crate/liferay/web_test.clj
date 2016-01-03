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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.web-test
  (:require
    [clojure.test :refer :all]
    [pallet.actions :as actions]
    [org.domaindrivenarchitecture.pallet.crate.liferay.web :as sut]
    ))

(def ^:private vhost-debit
  ["<VirtualHost *:443>"
   "  ServerName intermediate.intra.politaktiv.org"
   "  ServerAdmin admin@politaktiv.org"
   "  "
   "  Alias /quiz/ \"/var/www/static/quiz/\""
   "  "
   "  JkMount /* mod_jk_www"
   "  JkUnMount /quiz/* mod_jk_www"
   "  "
   "  Alias /googlexyz.html \"/var/www/static/google/googlexyz.html\""
   "  JkUnMount /googlexyz.html mod_jk_www"
   "  "
   "  ErrorDocument 503 /error/503.html"
   "  Alias /error \"/var/www/static/error\""
   "  JkUnMount /error/* mod_jk_www"
   "  "
   "  ErrorLog \"/var/log/apache2/error.log\""
   "  LogLevel warn"
   "  CustomLog \"/var/log/apache2/ssl-access.log\" combined"
   "  "
   "  GnuTLSEnable on"
   "  GnuTLSCacheTimeout 300"
   "  GnuTLSPriorities SECURE:!VERS-SSL3.0:!MD5:!DHE-RSA:!DHE-DSS:!AES-256-CBC:%COMPAT"
   "  GnuTLSExportCertificates on"
   "  "
   "  GnuTLSCertificateFile /etc/apache2/ssl.crt/intermediate.intra.politaktiv.org.certs"
   "  GnuTLSKeyFile /etc/apache2/ssl.key/intermediate.intra.politaktiv.org.key"
   "  "
   "</VirtualHost>"])



(deftest liferay-vhost
  (testing 
    "test the good case"
    (is (= vhost-debit
           (sut/liferay-vhost
             :domain-name "intermediate.intra.politaktiv.org"
             :server-admin-email "admin@politaktiv.org"
             :app-port "8080"
             :google-id "xyz")))
    )
  )
