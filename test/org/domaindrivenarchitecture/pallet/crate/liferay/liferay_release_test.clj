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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.liferay-release-test
  (:require
    [clojure.test :refer :all]    
    [clojure.set :as cloj-set]
    [schema.core :as s]
    [schema.experimental.complete :as c]
    [org.domaindrivenarchitecture.pallet.crate.liferay.liferay-release :as sut]
    ))

(def default-release-definition
  (c/complete {:application ["name" "download-url"]} sut/LiferayRelease))
 
 (deftest defaults
  (testing 
    "test the default release definition" 
      (is (s/validate
            sut/LiferayRelease
            sut/default-release))
      (is (s/validate
            sut/LiferayRelease
            default-release-definition))))
  
