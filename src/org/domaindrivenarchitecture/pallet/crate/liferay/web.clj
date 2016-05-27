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
    ; TODO: review jem: 2016_05_25: pls move all httpd.* dependencies to dda-httpd-crate    
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
    [org.domaindrivenarchitecture.pallet.crate.httpd :as httpd]
    [schema-tools.core :as st]
    [schema.core :as s]
    ))


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