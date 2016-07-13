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

(ns org.domaindrivenarchitecture.pallet.crate.liferay-test
  (:require
    [clojure.test :refer :all]    
    [clojure.set :as cloj-set]
    [schema.core :as s]
    [schema.experimental.complete :as c]
    [org.domaindrivenarchitecture.pallet.crate.mysql :as mysql]
    [org.domaindrivenarchitecture.pallet.crate.liferay.release-model :as release-model]
    [org.domaindrivenarchitecture.pallet.crate.liferay :as sut]
    ))

(def partial-config 
 {:third-party-download-root-dir "some dir"
  :tomcat {:custom-config {:with-manager-webapps false}}})

(deftest config-test
  (testing 
    "test if the default config is valid"
    (is (sut/merge-config partial-config))))

(def release-definition
  (c/complete {:app ["name" "download-url"]} release-model/LiferayRelease))

(def db-definition
  (c/complete {} mysql/DbConfig))
  
(deftest test-liferay-schema
 (testing 
   "test wheter merged config are validated" 
     (is (thrown? clojure.lang.ExceptionInfo
                  (let [config (sut/merge-config {:an-unexpected-key nil})])))
     (is (sut/merge-config {:third-party-download-root-dir "download root"
                            :httpd {:vhosts
                                    {:default 
                                     {:domain-name "fqdn"
                                      :cert-manual
                                      {:domain-cert "cert"
                                       :domain-key "key"}}}}}))
     (is (sut/merge-config {:third-party-download-root-dir "download root"
                            :httpd {:vhosts
                                    {:default 
                                     {:domain-name "fqdn"
                                      :cert-manual {:domain-cert "cert"
                                                    :domain-key "key"}}}}
                            :tomcat {:java-vm-config {:xmx "123"}}}))
     (is (sut/merge-config {:third-party-download-root-dir "download root"
                            :tomcat {:java-vm-config {:xmx "123"}}}
                           true))
     ))

(deftest server-spec
  (testing 
    "test the server spec" 
      (is (map? sut/with-liferay))
      ))