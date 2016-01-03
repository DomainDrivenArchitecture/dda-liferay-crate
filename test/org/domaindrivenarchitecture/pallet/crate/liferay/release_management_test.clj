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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.web-test
  (:require
    [clojure.test :refer :all]    
    [clojure.set :as cloj-set]
    [org.domaindrivenarchitecture.pallet.crate.liferay.release-management :as sut]
    ))

(def all-supported-app-releases-input 
  (sorted-map
      :0.0.0 #{["resources-importer-web"               
               "liferay/marketplace/6.2/resources-importer-web-6.2.0.1-ce-ga1-20131101161303488.war"]
              ["CSV_User_Import-portlet"               
               "liferay/marketplace/6.2/CSV_User_Import-portlet-6.2.0.2.war"]
              ["kaleo-web"               
               "liferay/marketplace/6.2/kaleo-web-6.2.0.1.war"]
              ["social-bookmarks-hook"               
               "liferay/marketplace/6.2/social-bookmarks-hook-6.2.0.1-ce-ga1-20131203093610793.war"]
              ["social-comments-portlet"               
               "liferay/marketplace/6.2/social-comments-portlet-1.3.0.1.war"]
              ["web-form-portlet"               
               "liferay/marketplace/6.2/web-form-portlet-6.2.0.2-ce-ga1-20140120092742162.war"]
              ["politaktiv-community-newsletter-portlet"               
               "politaktiv/politaktiv-community-newsletter-portlet/6.2.0.1/politaktiv-community-newsletter-portlet-6.2.0.1.war"]
              ["politaktiv-community-select-portlet"               
               "politaktiv/politaktiv-community-select-portlet/6.2.1.4/politaktiv-community-select-portlet-6.2.1.4.war"]
              ["politaktiv-default-theme"               
               "politaktiv/politaktiv-default-theme/6.2.1.17/politaktiv-default-theme-6.2.1.17.war"]
              ["politaktiv-layouttpl"               
               "politaktiv/politaktiv-layouts/6.2.1.11/politaktiv-layouttpl-6.2.1.11.war"]
              ["politaktiv-aktuelles-meinungsbild-portlet"               
               "politaktiv/politaktiv-aktuelles-meinungsbild-portlet/6.2.0.3/politaktiv-aktuelles-meinungsbild-portlet-6.2.0.3.war"]
              ["politaktiv-map-portlet"               
               "politaktiv/politaktiv-map-portlet/6.2.1.6/politaktiv-map-portlet-6.2.1.6.war"]
              ["politaktiv-hook"               
               "politaktiv/politaktiv-hook-62/6.2.1.9/politaktiv-hook-6.2.1.9.war"]
              ["politaktiv-easy-participation-portlet"               
               "politaktiv/politaktiv-easy-participation-portlet/6.2.1.2/politaktiv-easy-participation-portlet-6.2.1.2.war"]
              ["open-graph-hook"               
               "politaktiv/open-graph-hook/6.2.1.2/open-graph-hook-6.2.1.2.war"]
              }
      :0.0.1 {
             :add #{["Ancud_ShortURL-portlet"               
                   "liferay/marketplace/6.2/Ancud_ShortURL-portlet-6.2.1.1.war"]
                   }
             :target "0.0.0"
             }
      :0.0.2 {
              :remove #{["politaktiv-default-theme"               
                         "politaktiv/politaktiv-default-theme/6.2.1.17/politaktiv-default-theme-6.2.1.17.war"]
                        }
              :add #{["politaktiv-default-theme"               
                      "politaktiv/politaktiv-default-theme/6.2.1.18/politaktiv-default-theme-6.2.1.18.war"]
                     }
              :target "0.0.1"
              }
      :0.0.3 {
              :remove #{["politaktiv-hook"               
                         "politaktiv/politaktiv-hook-62/6.2.1.9/politaktiv-hook-6.2.1.9.war"]
                        ["politaktiv-default-theme"               
                         "politaktiv/politaktiv-default-theme/6.2.1.18/politaktiv-default-theme-6.2.1.18.war"]}
              :add #{["politaktiv-hook"               
                      "politaktiv/politaktiv-hook-62/6.2.1.10/politaktiv-hook-6.2.1.10.war"]
                     ["social-share-privacy-heise-hook"               
                      "meissa/social-share-privacy-heise-hook/6.2.1.2/social-share-privacy-heise-hook-6.2.1.2.war"]
                     }
              :target "0.0.2"
              }))


(def all-supported-app-releases-output
  (sorted-map
      :0.0.0 #{["resources-importer-web"               
               "liferay/marketplace/6.2/resources-importer-web-6.2.0.1-ce-ga1-20131101161303488.war"]
              ["CSV_User_Import-portlet"               
               "liferay/marketplace/6.2/CSV_User_Import-portlet-6.2.0.2.war"]
              ["kaleo-web"               
               "liferay/marketplace/6.2/kaleo-web-6.2.0.1.war"]
              ["social-bookmarks-hook"               
               "liferay/marketplace/6.2/social-bookmarks-hook-6.2.0.1-ce-ga1-20131203093610793.war"]
              ["social-comments-portlet"               
               "liferay/marketplace/6.2/social-comments-portlet-1.3.0.1.war"]
              ["web-form-portlet"               
               "liferay/marketplace/6.2/web-form-portlet-6.2.0.2-ce-ga1-20140120092742162.war"]
              ["politaktiv-community-newsletter-portlet"               
               "politaktiv/politaktiv-community-newsletter-portlet/6.2.0.1/politaktiv-community-newsletter-portlet-6.2.0.1.war"]
              ["politaktiv-community-select-portlet"               
               "politaktiv/politaktiv-community-select-portlet/6.2.1.4/politaktiv-community-select-portlet-6.2.1.4.war"]
              ["politaktiv-default-theme"               
               "politaktiv/politaktiv-default-theme/6.2.1.17/politaktiv-default-theme-6.2.1.17.war"]
              ["politaktiv-layouttpl"               
               "politaktiv/politaktiv-layouts/6.2.1.11/politaktiv-layouttpl-6.2.1.11.war"]
              ["politaktiv-aktuelles-meinungsbild-portlet"               
               "politaktiv/politaktiv-aktuelles-meinungsbild-portlet/6.2.0.3/politaktiv-aktuelles-meinungsbild-portlet-6.2.0.3.war"]
              ["politaktiv-map-portlet"               
               "politaktiv/politaktiv-map-portlet/6.2.1.6/politaktiv-map-portlet-6.2.1.6.war"]
              ["politaktiv-hook"               
               "politaktiv/politaktiv-hook-62/6.2.1.9/politaktiv-hook-6.2.1.9.war"]
              ["politaktiv-easy-participation-portlet"               
               "politaktiv/politaktiv-easy-participation-portlet/6.2.1.2/politaktiv-easy-participation-portlet-6.2.1.2.war"]
              ["open-graph-hook"               
               "politaktiv/open-graph-hook/6.2.1.2/open-graph-hook-6.2.1.2.war"]
              }
      :0.0.1 #{ ["resources-importer-web"               
               "liferay/marketplace/6.2/resources-importer-web-6.2.0.1-ce-ga1-20131101161303488.war"]
              ["CSV_User_Import-portlet"               
               "liferay/marketplace/6.2/CSV_User_Import-portlet-6.2.0.2.war"]
              ["kaleo-web"               
               "liferay/marketplace/6.2/kaleo-web-6.2.0.1.war"]
              ["social-bookmarks-hook"               
               "liferay/marketplace/6.2/social-bookmarks-hook-6.2.0.1-ce-ga1-20131203093610793.war"]
              ["social-comments-portlet"               
               "liferay/marketplace/6.2/social-comments-portlet-1.3.0.1.war"]
              ["web-form-portlet"               
               "liferay/marketplace/6.2/web-form-portlet-6.2.0.2-ce-ga1-20140120092742162.war"]
              ["politaktiv-community-newsletter-portlet"               
               "politaktiv/politaktiv-community-newsletter-portlet/6.2.0.1/politaktiv-community-newsletter-portlet-6.2.0.1.war"]
              ["politaktiv-community-select-portlet"               
               "politaktiv/politaktiv-community-select-portlet/6.2.1.4/politaktiv-community-select-portlet-6.2.1.4.war"]
              ["politaktiv-default-theme"               
               "politaktiv/politaktiv-default-theme/6.2.1.17/politaktiv-default-theme-6.2.1.17.war"]
              ["politaktiv-layouttpl"               
               "politaktiv/politaktiv-layouts/6.2.1.11/politaktiv-layouttpl-6.2.1.11.war"]
              ["politaktiv-aktuelles-meinungsbild-portlet"               
               "politaktiv/politaktiv-aktuelles-meinungsbild-portlet/6.2.0.3/politaktiv-aktuelles-meinungsbild-portlet-6.2.0.3.war"]
              ["politaktiv-map-portlet"               
               "politaktiv/politaktiv-map-portlet/6.2.1.6/politaktiv-map-portlet-6.2.1.6.war"]
              ["politaktiv-hook"               
               "politaktiv/politaktiv-hook-62/6.2.1.9/politaktiv-hook-6.2.1.9.war"]
              ["politaktiv-easy-participation-portlet"               
               "politaktiv/politaktiv-easy-participation-portlet/6.2.1.2/politaktiv-easy-participation-portlet-6.2.1.2.war"]
              ["open-graph-hook"               
               "politaktiv/open-graph-hook/6.2.1.2/open-graph-hook-6.2.1.2.war"]
              ["Ancud_ShortURL-portlet"               
               "liferay/marketplace/6.2/Ancud_ShortURL-portlet-6.2.1.1.war"]}
      :0.0.2 #{ ["resources-importer-web"               
                 "liferay/marketplace/6.2/resources-importer-web-6.2.0.1-ce-ga1-20131101161303488.war"]
               ["CSV_User_Import-portlet"               
                "liferay/marketplace/6.2/CSV_User_Import-portlet-6.2.0.2.war"]
               ["kaleo-web"               
                "liferay/marketplace/6.2/kaleo-web-6.2.0.1.war"]
               ["social-bookmarks-hook"               
                "liferay/marketplace/6.2/social-bookmarks-hook-6.2.0.1-ce-ga1-20131203093610793.war"]
               ["social-comments-portlet"               
                "liferay/marketplace/6.2/social-comments-portlet-1.3.0.1.war"]
               ["web-form-portlet"               
                "liferay/marketplace/6.2/web-form-portlet-6.2.0.2-ce-ga1-20140120092742162.war"]
               ["politaktiv-community-newsletter-portlet"               
                "politaktiv/politaktiv-community-newsletter-portlet/6.2.0.1/politaktiv-community-newsletter-portlet-6.2.0.1.war"]
               ["politaktiv-community-select-portlet"               
                "politaktiv/politaktiv-community-select-portlet/6.2.1.4/politaktiv-community-select-portlet-6.2.1.4.war"]
               ["politaktiv-default-theme"               
                "politaktiv/politaktiv-default-theme/6.2.1.18/politaktiv-default-theme-6.2.1.18.war"]
               ["politaktiv-layouttpl"               
                "politaktiv/politaktiv-layouts/6.2.1.11/politaktiv-layouttpl-6.2.1.11.war"]
               ["politaktiv-aktuelles-meinungsbild-portlet"               
                "politaktiv/politaktiv-aktuelles-meinungsbild-portlet/6.2.0.3/politaktiv-aktuelles-meinungsbild-portlet-6.2.0.3.war"]
               ["politaktiv-map-portlet"               
                "politaktiv/politaktiv-map-portlet/6.2.1.6/politaktiv-map-portlet-6.2.1.6.war"]
               ["politaktiv-hook"               
                "politaktiv/politaktiv-hook-62/6.2.1.9/politaktiv-hook-6.2.1.9.war"]
               ["politaktiv-easy-participation-portlet"               
                "politaktiv/politaktiv-easy-participation-portlet/6.2.1.2/politaktiv-easy-participation-portlet-6.2.1.2.war"]
               ["open-graph-hook"               
                "politaktiv/open-graph-hook/6.2.1.2/open-graph-hook-6.2.1.2.war"]
               ["Ancud_ShortURL-portlet"               
                "liferay/marketplace/6.2/Ancud_ShortURL-portlet-6.2.1.1.war"]}
      :0.0.3 #{ ["resources-importer-web"               
                 "liferay/marketplace/6.2/resources-importer-web-6.2.0.1-ce-ga1-20131101161303488.war"]
               ["CSV_User_Import-portlet"               
                "liferay/marketplace/6.2/CSV_User_Import-portlet-6.2.0.2.war"]
               ["kaleo-web"               
                "liferay/marketplace/6.2/kaleo-web-6.2.0.1.war"]
               ["social-bookmarks-hook"               
                "liferay/marketplace/6.2/social-bookmarks-hook-6.2.0.1-ce-ga1-20131203093610793.war"]
               ["social-comments-portlet"               
                "liferay/marketplace/6.2/social-comments-portlet-1.3.0.1.war"]
               ["web-form-portlet"               
                "liferay/marketplace/6.2/web-form-portlet-6.2.0.2-ce-ga1-20140120092742162.war"]
               ["politaktiv-community-newsletter-portlet"               
                "politaktiv/politaktiv-community-newsletter-portlet/6.2.0.1/politaktiv-community-newsletter-portlet-6.2.0.1.war"]
               ["politaktiv-community-select-portlet"               
                "politaktiv/politaktiv-community-select-portlet/6.2.1.4/politaktiv-community-select-portlet-6.2.1.4.war"]
               ["politaktiv-layouttpl"               
                "politaktiv/politaktiv-layouts/6.2.1.11/politaktiv-layouttpl-6.2.1.11.war"]
               ["politaktiv-aktuelles-meinungsbild-portlet"               
                "politaktiv/politaktiv-aktuelles-meinungsbild-portlet/6.2.0.3/politaktiv-aktuelles-meinungsbild-portlet-6.2.0.3.war"]
               ["politaktiv-map-portlet"               
                "politaktiv/politaktiv-map-portlet/6.2.1.6/politaktiv-map-portlet-6.2.1.6.war"]
               ["politaktiv-hook"               
                "politaktiv/politaktiv-hook-62/6.2.1.10/politaktiv-hook-6.2.1.10.war"]
               ["politaktiv-easy-participation-portlet"               
                "politaktiv/politaktiv-easy-participation-portlet/6.2.1.2/politaktiv-easy-participation-portlet-6.2.1.2.war"]
               ["open-graph-hook"               
                "politaktiv/open-graph-hook/6.2.1.2/open-graph-hook-6.2.1.2.war"]
               ["Ancud_ShortURL-portlet"               
                "liferay/marketplace/6.2/Ancud_ShortURL-portlet-6.2.1.1.war"]
               ["social-share-privacy-heise-hook"               
                "meissa/social-share-privacy-heise-hook/6.2.1.2/social-share-privacy-heise-hook-6.2.1.2.war"]}            
              ))




(deftest recreate-releases-map
  (testing 
    "test the good case" 
    
    
    ; no changes
    
    (let [new-set  (all-supported-app-releases-output :0.0.0)
          test-set ((sut/create-all-supported-app-releases-full-mapping all-supported-app-releases-input) :0.0.0)]
      ;sets are equal if both A/B and B/A is {}
      (is (and (empty? (cloj-set/difference new-set test-set))
               (empty? (cloj-set/difference test-set new-set)))))
    
    ;-------------------------;
    ;add plugin
    
    (let [new-set  (all-supported-app-releases-output :0.0.1)
          test-set ((sut/create-all-supported-app-releases-full-mapping all-supported-app-releases-input) :0.0.1)]
      ;sets are equal if both A/B and B/A is {}
      (is (and (empty? (cloj-set/difference new-set test-set))
               (empty? (cloj-set/difference test-set new-set)))))

    
    ;-------------------------;
    ;add and remove
    (let [new-set  (all-supported-app-releases-output :0.0.2)
          test-set ((sut/create-all-supported-app-releases-full-mapping all-supported-app-releases-input) :0.0.2)]
      ;sets are equal if both A/B and B/A is {}
      (is (and (empty? (cloj-set/difference new-set test-set))
               (empty? (cloj-set/difference test-set new-set)))))
    
   ) 
    ;-------------------------;
    ;add and remove several plugins at the same time
    
    (let [new-set  (all-supported-app-releases-output :0.0.3)
          test-set ((sut/create-all-supported-app-releases-full-mapping all-supported-app-releases-input) :0.0.3)]
      ;sets are equal if both A/B and B/A is {}
      (is (and (empty? (cloj-set/difference new-set test-set))
               (empty? (cloj-set/difference test-set new-set)))))
    
    
    
   
  )