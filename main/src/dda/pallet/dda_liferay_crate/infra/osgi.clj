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

(ns dda.pallet.dda-liferay-crate.infra.osgi
  (:require
    [clojure.string :as string]
    [schema.core :as s]
    [pallet.actions :as actions]
    [dda.config.commons.directory-model :as dir-model]))

(def OsgiConfig
  {:download-url s/Str
   :dir dir-model/NonRootDirectory
   :os-user s/Str})

(s/defn install-osgi
  [config :- OsgiConfig]
  (let [{:keys [download-url dir os-user]} config]
    (actions/remote-directory
      dir
      :url download-url
      :unpack :unzip
      :recursive true
      :owner "root"
      :group os-user)))
