; Copyright (c) meissa GmbH. All rights reserved.
; You must not remove this notice, or any other, from this software.
;
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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.web
  (:require
    [httpd.crate.apache2 :as apache2]
    [httpd.crate.vhost :as vhost]
    [httpd.crate.config :as httpd-config]
    [httpd.crate.basic-auth :as auth]
    [httpd.crate.mod-gnutls :as gnutls]
    [httpd.crate.mod-jk :as jk]
    [httpd.crate.google-siteownership-verification :as google]
    [httpd.crate.common :as httpd-common]
    [httpd.crate.mod-rewrite :as rewrite]
    [httpd.crate.webserver-maintainance :as maintainance]
    ))

(defn liferay-vhost
  [& {:keys [domain-name
             letsencrypt
             server-admin-email
             app-port
             google-id]}]
  (into 
    []
    (concat
      (vhost/vhost-head 
        :listening-port "443"
        :domain-name domain-name 
        :server-admin-email server-admin-email)
      (httpd-common/prefix 
        "  " 
        (into 
          []
          (concat
            ["Alias /quiz/ \"/var/www/static/quiz/\""
             ""]
            (jk/vhost-jk-mount :path "/*")
            (jk/vhost-jk-unmount :path "/quiz/*")
            [""]
            (google/vhost-ownership-verification 
              :id google-id
              :consider-jk true)
            (maintainance/vhost-service-unavailable-error-page
              :consider-jk true)
            (vhost/vhost-log 
              :error-name "error.log"
              :log-name "ssl-access.log"
              :log-format "combined")
            (if letsencrypt
              (gnutls/vhost-gnutls-letsencrypt domain-name)
              (gnutls/vhost-gnutls domain-name))
            )))
      vhost/vhost-tail
      )
    )
  )

(defn install-webserver
  []
  (apache2/install-apache2-action)
  (apache2/install-apachetop-action)
  (gnutls/install-mod-gnutls)
  (jk/install-mod-jk)
  (rewrite/install-mod-rewrite))

(defn configure-webserver
  [& {:keys [name
             letsencrypt
             domain-name 
             domain-cert 
             domain-key 
             ca-cert
             user-credentials
             app-port ;todo: this is not used...
             google-id
             maintainance-page-content]}]
  
  (apache2/config-apache2-production-grade
    :security 
    httpd-config/security)
  
  (if-not letsencrypt
	  (gnutls/configure-gnutls-credentials
	    :domain-name domain-name
	    :domain-cert domain-cert
	    :domain-key domain-key
	    :ca-cert ca-cert))
  
  (jk/configure-mod-jk-worker)
  
  (google/configure-ownership-verification :id google-id)
    
  (apache2/configure-and-enable-vhost
    "000-default"
    (vhost/vhost-conf-default-redirect-to-https-only
      :domain-name domain-name
      :server-admin-email (str "admin@" domain-name)))
  
  (apache2/configure-and-enable-vhost
    "000-default-ssl"
    (liferay-vhost
      :letsencrypt letsencrypt
      :domain-name domain-name
      :server-admin-email (str "admin@" domain-name)
      :google-id google-id))
  
    (maintainance/write-maintainance-file :content maintainance-page-content)
  
  )

(defn configure-webserver-local
  []
	; configure jk worker
  (jk/configure-mod-jk-worker)
  ; enable apache2 vhost for localhast and asign jk worker
	(apache2/configure-and-enable-vhost
     "000-default"
     (into [] (concat (vhost/vhost-head :domain-name "localhost")
                      (jk/vhost-jk-mount :path "/*")
                      (vhost/vhost-log :error-name "error.log" :log-name "ssl-access.log" :log-format "combined")
                      vhost/vhost-tail)))
 )