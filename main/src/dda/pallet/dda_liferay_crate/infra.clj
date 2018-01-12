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

(ns dda.pallet.dda-liferay-crate.infra
  "Provides the configuration to be used by the liferay-crate namespaces. The configuration in this
  namespace is the most fundamental and allows for the most customization."
  (:require
    [dda.pallet.core.dda-crate :as dda-crate]
    [schema.core :as s]
    [dda.pallet.dda-liferay-crate.infra.schema :as schema]
    [dda.pallet.dda-liferay-crate.infra.liferay :as liferay]
    [dda.pallet.dda-liferay-crate.infra.osgi :as osgi]))

; -------------  facility and version  ------------------
(def facility :dda-liferay-crate)
(def version  [0 3 0])

; -------------  schemas  ------------------
(def LiferayCrateConfig schema/LiferayCrateConfig)

(def LiferayRelease schema/LiferayRelease)

(def InfraResult {facility LiferayCrateConfig})

; -------------  methods and definitions  ------------------
(s/defmethod dda-crate/dda-install facility
  [dda-crate config]
  (let [{:keys [osgi]} config]
    (liferay/install-liferay config)
    (when (contains? config :osgi)
      (osgi/install-osgi osgi))))

(s/defmethod dda-crate/dda-configure facility
  [dda-crate config]
  (liferay/configure-liferay config))

(def liferay-crate
  (dda-crate/make-dda-crate
   :facility facility
   :version version))

(def with-liferay
  (dda-crate/create-server-spec liferay-crate))
