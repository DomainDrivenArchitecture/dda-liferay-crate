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

(ns dda.pallet.dda-liferay-crate.app-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as s]
    [dda.pallet.dda-liferay-crate.app :as sut]))


; --------------------------- Test configs ---------------------------
(def config-resolved
  "domainConfig resolved"
  {:fq-domain-name "example.de"
   :db-root-passwd "test1234"
   :db-user-name "dbtestuser"
   :db-user-passwd "test1234"
   :settings #{ :test}})

(def config-unresolved-min
  "domainConfig unresolved"
  {:fq-domain-name "example.de"
   :db-root-passwd {:plain "test1234"}
   :db-user-name "dbtestuser"
   :db-user-passwd {:plain "test1234"}
   :settings #{:test}})

(def config-unresolved
  "domainConfig unresolved"
  {:fq-domain-name "example.de"
   :db-root-passwd {:plain "test1234"}
   :db-user-name "dbtestuser"
   :db-user-passwd {:plain "test1234"}
   :settings #{:test}})


; -------------------------------- Tests ------------------------------
(deftest test-resolved-config
  (testing
    (is (= (-> config-resolved :db-root-passwd)
           (-> (sut/app-configuration config-resolved) :group-specific-config :dda-liferay-crate :dda-mariadb :root-passwd)))))

(deftest test-unresolved-config
  (testing
    (is (sut/resolve-secrets config-unresolved))
    (is (= (-> config-unresolved :db-root-passwd :plain)
           (-> (sut/app-configuration (sut/resolve-secrets config-unresolved)) :group-specific-config :dda-liferay-crate :dda-mariadb :root-passwd)))))
