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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.release-model-test
  (:require
    [clojure.test :refer :all]    
    [clojure.set :as cloj-set]
    [schema.core :as s]
    [schema.experimental.complete :as c]
    [org.domaindrivenarchitecture.pallet.crate.mysql :as mysql]
    [org.domaindrivenarchitecture.pallet.crate.liferay.release-model :as sut]
    ))

(def release-definition
  (c/complete {:app ["name" "download-url"]} sut/LiferayRelease))
 
 (deftest test-liferay-release-schema
  (testing 
    "test the default release definition" 
      (is (s/validate
            sut/LiferayRelease
            (sut/default-release [])))
      (is (s/validate
            sut/LiferayReleaseConfig
            {:release-dir "/prepare-rollout/"
             :releases [release-definition]}))
      ))
  
(def release-test-a
  {:name "release-test-a"
   :version [1 2 3]
   :app ["name" "url"]
   :portlets [ ["portlet1" "url1a"] ["portlet2" "url2a"] ]
   })
 
(def release-test-b
 {:name "release-test-b"
  :version [1 2 4]
  :app ["name" "url"]
  :portlets [ ["portlet1" "url1b"] ["portlet3" "url3b"] ]
  })

(def release-test-merged-ab
 {:name "release-test-b"
  :version [1 2 4]
  :app ["name" "url"]
  :portlets [ ["portlet2" "url2a"] ["portlet1" "url1b"] ["portlet3" "url3b"]  ]
  })


(deftest release-tests-valid
  (testing 
    "test if test releases are valid"
    (is (s/validate sut/LiferayRelease release-test-a))
    (is (s/validate sut/LiferayRelease release-test-b))
    (is (s/validate sut/LiferayRelease release-test-merged-ab))
    ))
 
(deftest merge-releaseapps
  (testing
    "merging releases"
    
    (is (= 
          (sut/merge-releases release-test-a release-test-b) 
          release-test-merged-ab))
    
    (is (= 
          (sut/merge-releases release-test-a release-test-merged-ab) 
          release-test-merged-ab))
    
    (is (= 
          (sut/merge-releases release-test-a release-test-merged-ab release-test-merged-ab release-test-merged-ab) 
          release-test-merged-ab))
    ))