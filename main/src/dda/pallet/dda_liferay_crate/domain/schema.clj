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
(ns dda.pallet.dda-liferay-crate.domain.schema
  (:require
    [schema.core :as s]
    [dda.pallet.commons.secret :as secret]))

(def DomainConfig
  "The high-level domain configuration for the liferay-crate."
  {:fq-domain-name s/Str
   (s/optional-key :google-id) s/Str
   :db-root-passwd secret/Secret
   :db-user-name s/Str
   :db-user-passwd secret/Secret
   ;if :test is specified in :settings, snakeoil certificates will be used
   :settings (hash-set (s/enum :test))
   (s/optional-key :backup) {:bucket-name s/Str
                             :gpg {:gpg-public-key  secret/Secret
                                   :gpg-private-key secret/Secret
                                   :gpg-passphrase  secret/Secret}
                             :aws {:aws-access-key-id secret/Secret
                                   :aws-secret-access-key secret/Secret}}})

(def DomainConfigResolved
  "The high-level domain configuration for the liferay-crate with secrets resolved."
  {:fq-domain-name s/Str
   (s/optional-key :google-id) s/Str
   :db-root-passwd s/Str
   :db-user-name s/Str
   :db-user-passwd s/Str
   :settings (hash-set (s/enum :test))
   (s/optional-key :backup) {:bucket-name s/Str
                             :gpg {:gpg-public-key s/Str
                                   :gpg-private-key s/Str
                                   :gpg-passphrase s/Str}
                             :aws {:aws-access-key-id s/Str
                                   :aws-secret-access-key s/Str}}})
