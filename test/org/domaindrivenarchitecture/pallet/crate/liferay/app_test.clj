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


(ns org.domaindrivenarchitecture.pallet.crate.liferay.app-test
  (:require
    [clojure.test :refer :all]
    [clojure.java.io :as io]    
    [clojure.string :as string]
    [pallet.build-actions :as build-actions]
    [schema.core :as s]
    [schema.experimental.complete :as c]
    [pallet.stevedore :as stevedore]
    [pallet.actions :as actions]
    [org.domaindrivenarchitecture.pallet.commons.stevedore-test]
    [org.domaindrivenarchitecture.pallet.commons.plan-test-utils :as tu]
    [org.domaindrivenarchitecture.pallet.crate.liferay.release-model :as schema]
    [org.domaindrivenarchitecture.pallet.crate.liferay.app :as sut]))

(deftest test-release-name
  "test the release name generation"
  []
  (testing 
    (is 
      (= 
        "test-0.2.0"
        (sut/release-name (c/complete {:name "test" :version [0 2 0]} schema/LiferayRelease))
        ))
    (is 
      (= 
        "test-0.2.0.1"
        (sut/release-name (c/complete {:name "test" :version [0 2 0 1]} schema/LiferayRelease))
        ))
    ))

(deftest test-release-dir
  "test the release name generation"
  []
  (testing 
    (is 
      (= 
        "dir/test-0.2.0/"
        (sut/release-dir "dir/"  (c/complete {:name "test" :version [0 2 0]} schema/LiferayRelease))
        ))
    ))

(deftest test-good-download-and-store
  "test the good case"
  []
  (testing 
    "one app"
    (is 
      (.contains 
        (tu/extract-nth-action-command
          (build-actions/build-actions
            build-actions/ubuntu-session         
            (sut/download-and-store-applications "/somedir/"
                                                 (c/complete {:app ["appId" "http://url/app.6.0.1.war"]} schema/LiferayRelease) 
                                                 :app))
             1)
        "app.6.0.1.war"
        )))
  (testing 
    "multi app"
    (let [actions (build-actions/build-actions
            build-actions/ubuntu-session         
            (sut/download-and-store-applications "/somedir/" 
                                                 (c/complete
                                                   {:portlets [["appId1" "http://url/app1.6.0.1.war"]
                                                    ["appId2" "http://urls/app2.6.0.2.war"]]}
                                                   schema/LiferayRelease) 
                                                 :portlets))]
      (is 
        (.contains 
          (tu/extract-nth-action-command actions 1)
          "app1.6.0.1.war"
          ))
      (is 
        (.contains 
          (tu/extract-nth-action-command actions 2)
          "app2.6.0.2.war"
          ))
    ))
  )

(deftest remove-all-but-specified-versions
  (testing 
    "test the good case"
    (and (is (script= 
               (string/join
                 \newline
                 ["ls /var/lib/liferay/prepare-rollout/ | grep -Ev test0.2.0 | xargs -I {} rm -r /var/lib/liferay/prepare-rollout/{}"])
               (stevedore/with-script-language :pallet.stevedore.bash/bash
                 (stevedore/with-source-line-comments false  
                   (sut/remove-all-but-specified-versions 
                     [(c/complete {:name "test" :version [0 2 0]} schema/LiferayRelease)] 
                     "/var/lib/liferay/prepare-rollout/" )))
               ))
         "Testing for a list with mutliple elements and different directory"
         (is (script=
               (string/join 
                 \newline
                 ["ls /var/lib/liferay/prepare-rollout/test/ | grep -Ev test1.0.0|test-2.0.0 | xargs -I {} rm -r /var/lib/liferay/prepare-rollout/test/{}"])
               (stevedore/with-script-language :pallet.stevedore.bash/bash
                 (stevedore/with-source-line-comments false              
                   (sut/remove-all-but-specified-versions 
                       [(c/complete {:name "test" :version [1 0 0]} schema/LiferayRelease) , (c/complete {:name "test-" :version [2 0 0]} schema/LiferayRelease)] 
                       "/var/lib/liferay/prepare-rollout/test/" )))
               ))
         )
    ))

(deftest integration-prepare-rollout
  (testing 
    "test the good case"
    (let [plan (build-actions/build-actions
                 build-actions/ubuntu-session         
                 (sut/prepare-rollout
                   {:release-dir "/somedir/"
                    :releases [(c/complete {:config ["123" "2" "3"]} schema/LiferayRelease)]}))]
      (is 
        (.contains
           (first 
             (tu/extract-action-summary-containing
               "portal-ext.properties"
               (tu/extract-actions-meta
                 (tu/extract-actions
                   (tu/extract-node-values plan)))))
            "123"
          ))
      )))
