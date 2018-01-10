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

(ns dda.pallet.dda-liferay-crate.domain-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as s]
    [dda.pallet.dda-liferay-crate.domain :as sut]))

; --------------------------- Test configs ---------------------------
(def config-simple
  "domainConfigResolved"
  {:liferay-version :LR6
   :fq-domain-name "liferay.example.de"
   :db-root-passwd "test1234"
   :db-user-name "dbtestuser"
   :db-user-passwd "test1234"
   :settings #{ :test}})

(def config-full
  "domainConfigResolved"
  {:liferay-version :LR6
   :fq-domain-name "liferay.example.de"
   :google-id "xxxxxxxxxxxxxxxxxxxxx"
   :db-root-passwd "test1234"
   :db-user-name "dbtestuser"
   :db-user-passwd "test1234"
   :settings #{ :test}})

; -------------------------------- Tests ------------------------------})
(deftest test-httpd-configuration
  (testing
    "test the httpd config creation"
    (is (thrown? Exception (sut/httpd-domain-configuration {})))
    (is (= "liferay.example.de"
          (:domain-name (sut/httpd-domain-configuration config-simple))))
    (is (= "xxxxxxxxxxxxxxxxxxxxx"
          (:google-id (sut/httpd-domain-configuration config-full))))))

(deftest test-infra-configuration
  (testing
    "test the infra config creation"
    (is (thrown? Exception (sut/infra-configuration {})))
    (is (= "liferay.example.de"
           (:fq-domain-name (:dda-liferay-crate (sut/infra-configuration config-full)))))))
