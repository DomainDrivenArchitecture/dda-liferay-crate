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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.release-management
   (:require
    [pallet.actions :as actions]
    [pallet.stevedore :as stevedore]
    [clojure.set :as cloj-set]
    [clojure.string :as cloj-str]
    ))
(require '[pallet.stevedore :refer [script with-script-language]])
(require 'pallet.stevedore.bash) ;; for bash output

; paths
(def repo-base-path
  "https://github.com/PolitAktiv/releases/raw/master/"
  )
(def releases-local-path
  "/var/lib/liferay/portal-release-instance/"
  )


; useful functions concerning plugins:
; a plugin is a vector with two entries: the plugin name and its relative path
; plugins are part of a release-definition, a release definitons holds several plugins

(defn get-plugin-war-file-name
  {:deprecated "0.1.2"}
  [plugin]
  (subs (get plugin 1) ( + (.lastIndexOf (get plugin 1) "/" ) 1)) 
  )

(defn get-plugin-name
  {:deprecated "0.1.2"}
  [plugin]
  (get plugin 0)
  )

(defn get-plugin-download-path
  {:deprecated "0.1.2"}
  [plugin]
  (get plugin 1)
  )

(defn get-version-difference-new-plugins
  {:deprecated "0.1.2"}
  [all-supported-app-releases
   & {:keys [oldVersion newVersion]}]
  (into [] (cloj-set/difference (get all-supported-app-releases newVersion) (get all-supported-app-releases oldVersion)))
  )
  
(defn get-version-difference-dismissed-plugins
  {:deprecated "0.1.2"}
    [all-supported-app-releases
     & {:keys [oldVersion newVersion]}]
  (into [] (cloj-set/difference (get all-supported-app-releases oldVersion) (get all-supported-app-releases newVersion)))
  )

(defn- contains-plugin? [plugin coll]
  {:deprecated "0.1.2"}
  (boolean (some #(= plugin %) coll)))


; 
; plugin management
; 

(defn- create-release
  "creates a new set of plugins (=release)"
  {:deprecated "0.1.2"}
  [new-release-version-key-string release-definitions]
  (let [release-key (keyword new-release-version-key-string)
        new-release-version (release-definitions release-key)]
  (if (not (or (contains? new-release-version :add)
               (contains? new-release-version :remove)
               (contains? new-release-version :target)))
    ;if no key is given, the existing release-version is a base-version
    ;thus, return this release-version
    (release-definitions release-key)
    ;else create new release, based on previous releases
    (let [base-release (create-release (new-release-version :target) release-definitions)]
      (cloj-set/difference (cloj-set/union base-release (new-release-version :add)) (new-release-version :remove))
      )
  )))

(defn- get-supported-app-releases-for-build
  "returns a set of app-releases that a liferay-build supports"
  {:deprecated "0.1.2"}
  [build-version build-releases]
  (let [current-release (build-releases (keyword build-version))]
    ; if a build has a set of supported app-releases defined ...
    (if (contains? (build-releases (keyword build-version)) :supported-app-releases)
      ; ... return it ...
      (set (map keyword (into [] (current-release :supported-app-releases)))) 
      ; ... else return the app-releases of the the build-release (target) that the current release references
      (get-supported-app-releases-for-build (current-release :target) build-releases)
      )))


(defn create-all-supported-app-releases-full-mapping
  [all-supported-app-releases]
  "builds all releases, propagating the version changes"
  {:deprecated "0.1.2"}
  (zipmap  (into [] (keys all-supported-app-releases)) 
           (map #(create-release % all-supported-app-releases) (into [] (keys all-supported-app-releases)))
  ))

(defn- get-available-releases-build-specific
  "only makes those releases available, who fit to the current build"
  {:deprecated "0.1.2"}
  [build-version build-releases available-app-releases]
  (let [supported-app-releases (get-supported-app-releases-for-build build-version build-releases)]
    (cloj-set/intersection supported-app-releases (into #{} available-app-releases))
  ))

;
; blacklist
;

(defn without-blacklist
  "creates a collection of plugins that are not contained in the blacklist"
  {:deprecated "0.1.2"}
  [set-of-plugins blacklist]
  (if (empty? blacklist)
    ;if the blacklist is empty, just return the set of plugins
    set-of-plugins
    ;begin recursion
    (if (empty? set-of-plugins) ;recursivly goes through the set of plugins, creating an output list
      {} 
      (if (contains-plugin? (get-plugin-name (first set-of-plugins)) blacklist)   ; if the current plugin is contained in the blacklist ... 
        (without-blacklist (rest set-of-plugins) blacklist)                       ; ... don't append anything to the output and go on with the recursion
        (cons (first set-of-plugins)                                              ; else: append the current plugin to the output ...
              (without-blacklist (rest set-of-plugins) blacklist))))              ; ... and go on with the recursion
  ))


(defn fetch-available-app-releases
  [& {:keys [available-app-releases all-supported-app-releases plugin-blacklist]}]
  "Makes the addressed system download the plugins defined as available"
  {:deprecated "0.1.2"}
  (doseq [release available-app-releases]
    (let [release-download-directory (str releases-local-path (name release))]
      (actions/plan-when-not 
        (stevedore/script (file-exists? (release-download-directory)))
        (do
          (actions/directory
            release-download-directory
            :action :create
            :group "pallet"
            :owner "pallet"
            :mode "755"
            )
          (doseq [plugin (without-blacklist (get all-supported-app-releases release) plugin-blacklist)]
            (let [download-location 
                  (str repo-base-path (get-plugin-download-path plugin))
                  target-file-location 
                  (str release-download-directory "/" (get-plugin-war-file-name plugin))]
              (actions/remote-file 
                target-file-location
                :url download-location
                :insecure true)
              )))
      )))
  )

(defn delete-deprecated-releases
  [& {:keys [available-app-releases all-supported-app-releases]}]
  "Makes the addressed system remove release instances which are not defined as available (any more)"
  {:deprecated "0.1.2"}
  (let [release-keys-available (set available-app-releases)
        release-keys-all (set (keys all-supported-app-releases))
        ; perspektivisch könnten wir hier die existierenden releases vom Zielsystem lesen .. dat aber erst in einem zweiten schritt machen.
        release-keys-depricated (cloj-set/difference release-keys-all release-keys-available)]
    (doseq [release-key release-keys-depricated]
      (let [dir-deprecated (str releases-local-path (name release-key))]
        (actions/plan-when
          (stevedore/script (directory? (dir-deprecated)))
          (actions/directory
            dir-deprecated
            :action :delete
          ))   
        ))
    ))
 

;Stevedore Testing
(defn install-script-do-deploy
  "deploys a do-deploy script on the target platform"
  []
  (actions/plan-when-not 
      (stevedore/script (file-exists? "/var/lib/liferay/do-deploy.sh"))
      (actions/remote-file "/var/lib/liferay/do-deploy.sh"
                           :literal true
                           :owner "tomcat7" 
                           :group "pallet" 
                           :mode "0744"
                           :content )
  (with-script-language :pallet.stevedore.bash/bash
    (script (defn params [hot-or-cold-deploy? version]
               (if (= ("\"$#\"") 0)
                 (println "Available releases are:"
                          ;da müssen wir nochmal drüber nachdenken ob die Schleife hier wirklich Sinn macht
                          (doseq [x @(pipe (pipe ("find portal-release-instance/ -type d") ("sort")) ("cut -f2 -d0"))]
                            (println @x))
                          (println "\"Please use the release you want to deploy as a parameter for this script\""))))))))

(defn dsl-test
  ""
  []
   (with-script-language :pallet.stevedore.bash/bash
    (script (defn params [p1 version]
               (cond (= ("\"$#\"") 0)
                 (do (println "\"Available releases are:\"")
                          ;da müssen wir nochmal drüber nachdenken ob die Schleife hier wirklich Sinn macht
                          (doseq [x @(pipe (pipe ("find portal-release-instance/ -type d") ("sort")) ("cut -f2 -d0"))]
                            (println @x))
                          (println "\"Please use the release you want to deploy as a parameter for this script\""))
                 )))))

(defn dsl-test2
  ""
  []
  (with-script-language :pallet.stevedore.bash/bash
    (script (defn foo [x] (println @x)))))

(defn dsl-test3
  ""
  []
  (with-script-language :pallet.stevedore.bash/bash
    (script (do (defn foo [x y] (println @x))))))


(defn install-script-do-deploy-deprecated
  [& hot-deploy]
  "Creates script for deploying one specific release. To be called by the admin connected to the server via ssh" 
  {:deprecated "0.1.2"}
  (if hot-deploy
    (actions/plan-when-not 
      (stevedore/script (file-exists? "/var/lib/liferay/do-deploy.sh"))
      (actions/remote-file "/var/lib/liferay/do-deploy.sh"
                           :literal true
                           :owner "tomcat7" 
                           :group "pallet" 
                           :mode "0744"
                           :content 
                           (cloj-str/join
                             \newline
                             ["#!/bin/bash"                           
                              "if [ \"$#\" -eq 0 ]; then"
                              "   echo \"\" ;" 
                              "   echo \"Available Releases are:\" ;"
                              "   find portal-release-instance/ -type d | sort | cut -f2 -d'/' ;"
                              "   echo \"\" ; "
                              "   echo \"Please use the release you want to deploy as a parameter for this script\" ;"
                              "   echo \"\" ; "
                              "fi"
                              "if [ \"$#\" -eq 1 ]; then"
                              "   if [ -d /var/lib/liferay/portal-release-instance/${1} ]; then"
                              "      cp /var/lib/liferay/portal-release-instance/${1}/* /var/lib/liferay/deploy ;"
                              "      chown tomcat7 /var/lib/liferay/deploy/* ;"
                              "   else"
                              "      echo \"\";" 
                              "      echo \"ERROR: Specified release does not exist or you don't have the permission for it! Please run again as root! For a list of the available releases, run this script without parameters in order to show the available releases!\" ;"
                              "      echo \"\"; "
                              "   fi"
                              "fi"
                              "if [ \"$#\" -ge 2 ]; then"
                              "   echo \"\";" 
                              "   echo \"Please specify 1 release parameter only!\" ;"
                              "   echo \"\" ; "
                              "fi"
                              ""]
        )) 
    )
    ;überarbeitetes sh-Skript
    (actions/plan-when-not 
      (stevedore/script (file-exists? "/var/lib/liferay/do-deploy.sh"))
      (actions/remote-file "/var/lib/liferay/do-deploy.sh"
                           :literal true
                           :owner "tomcat7" 
                           :group "pallet" 
                           :mode "0744"
                           :content 
                           (cloj-str/join
                             \newline
                             ["#!/bin/bash"                           
                              "if [ \"$#\" -eq 0 ]; then"
                              "   echo \"\" ;" 
                              "   echo \"Available Releases are:\" ;"
                              "   find portal-release-instance/ -type d | sort | cut -f2 -d'/' ;"
                              "   echo \"\" ; "
                              "   echo \"Please use the release you want to deploy as a parameter for this script\" ;"
                              "   echo \"\" ; "
                              "fi"
                              "if [ \"$#\" -eq 1 ]; then"
                              "   if [ -d /var/lib/liferay/portal-release-instance/${1} ]; then"
                              ;Tomcat stoppen
                              "      service tomcat7 stop;"        
                              ;alles in /var/lib/tomcat7/webapps löschen
                              "      rm -rf /var/lib/tomcat7/webapps;"
                              ;nach /var/lib/tomcat7/webapps kopieren
                              "      cp /var/lib/liferay/portal-release-instance/${1}/* /var/lib/tomcat7/webapps ;"
                              
                              "      chown tomcat7 /var/lib/liferay/deploy/* ;"
                              ;f. Zeile angepasst
                              "      service tomcat7 start;"
                              ""
                              "   else"
                              "      echo \"\";" 
                              "      echo \"ERROR: Specified release does not exist or you don't have the permission for it! Please run again as root! For a list of the available releases, run this script without parameters in order to show the available releases!\" ;"
                              "      echo \"\"; "
                              "   fi"
                              "fi"
                              "if [ \"$#\" -ge 2 ]; then"
                              "   echo \"\";" 
                              "   echo \"Please specify 1 release parameter only!\" ;"
                              "   echo \"\" ; "
                              "fi"
                              ""]
      )))))

(defn install-script-do-rollout
  [& {:keys [custom-tomcat-home]}]
  "Creates script for rolling out a build deploying the new applications alongside. To be called by the admin connected to the server via ssh"
  {:deprecated "0.1.2"}
  (actions/plan-when-not 
    (stevedore/script (file-exists? "/var/lib/liferay/do-rollout.sh"))
    (actions/remote-file "/var/lib/liferay/do-rollout.sh"
                         :literal true
                         :owner "tomcat7" 
                         :group "pallet" 
                         :mode "0744"
                         :content 
                         (cloj-str/join
                           \newline
                           ["#!/bin/bash"                           
                            "if [ \"$#\" -eq 0 ]; then"
                            "   echo \"\" ;" 
                            "   echo \"Available Releases are:\" ;"
                            "   find portal-release-instance/ -type d | sort | cut -f2 -d'/' ;"
                            "   echo \"\" ; "
                            "   echo \"Please use the release you want to deploy after rollout as a parameter for this script\" ;"
                            "   echo \"\" ; "
                            "fi"
                            "if [ \"$#\" -eq 1 ]; then"
                            "   if [ -d /var/lib/liferay/portal-release-instance/${1} ]; then"
                            (if (empty? custom-tomcat-home)
                              "      service tomcat7 stop;"
                              (str "      " custom-tomcat-home "/bin/shutdown.sh;")
)
                            "      cp /var/lib/liferay/portal-release-instance/${1}/* /var/lib/liferay/deploy ;"
                            "      chown tomcat7 /var/lib/liferay/deploy/* ;"
                            (if (empty? custom-tomcat-home)
                              "      service tomcat7 start;"
                              (str "      " custom-tomcat-home "/bin/startup.sh;")
                              )
                            "   else"
                            "      echo \"\";" 
                            "      echo \"ERROR: Specified release does not exist or you don't have the permission for it! Please run again as root! For a list of the available releases, run this script without parameters in order to show the available releases!\" ;"
                            "      echo \"\"; "
                            "   fi"
                            "fi"
                            "if [ \"$#\" -ge 2 ]; then"
                            "   echo \"\";" 
                            "   echo \"Please specify 1 release parameter only!\" ;"
                            "   echo \"\" ; "
                            "fi"
                            ""]
      )) 
  ))


(defn install-release-management-directories
  []
  "Creates the directories that will be used for the release management"
  {:deprecated "0.1.2"}
  (actions/directory
    "/var/lib/liferay/portal-release-instance"
    :action :create
    :group "pallet"
    :owner "pallet"
    :mode "755"
    )   
  )

(defn install-release-management
  [& {:keys [custom-tomcat-home]}]
  "Runs all functions to be called in install phase"
  {:deprecated "0.1.2"}
  (install-script-do-deploy)
  (install-script-do-rollout :custom-tomcat-home custom-tomcat-home)
  (install-release-management-directories)
  )


(defn configure-release-definitions
  "DEPRICATED; downloads all release-definitions; was supposed to be called in the configure-phase;
   refer to rollout-release-definitions and call it in the rollout-phase"
  {:deprecated "0.1.2"}
  [& {:keys [available-app-releases 
             all-supported-app-releases 
             plugin-blacklist
             ]}]
  (let [all-supported-app-releases-full-mapping (create-all-supported-app-releases-full-mapping all-supported-app-releases)]
    "Installs the new release configuration by deleting depricated releases and fetching the current configuration. Call in configuration phase "
    (delete-deprecated-releases :available-app-releases available-app-releases
                                :all-supported-app-releases all-supported-app-releases-full-mapping)
    (fetch-available-app-releases :available-app-releases available-app-releases
                              :all-supported-app-releases all-supported-app-releases-full-mapping
                              :plugin-blacklist plugin-blacklist) 
    ))


(defn rollout-release-definitions
  "Rolls out the new release configuration by deleting depricated releases and fetching the current configuration;
   only rolls out those releases which fit the currently defined build-version"
  {:deprecated "0.1.2"}
  [& {:keys [build-version
             build-releases
             available-app-releases 
             all-supported-app-releases 
             plugin-blacklist
             ]}]
  (let [all-supported-app-releases-full-mapping (create-all-supported-app-releases-full-mapping all-supported-app-releases)
        available-app-releases-for-given-build (get-available-releases-build-specific build-version build-releases available-app-releases)
        ]
    (delete-deprecated-releases :available-app-releases available-app-releases-for-given-build
                                :all-supported-app-releases all-supported-app-releases-full-mapping)
    (fetch-available-app-releases :available-app-releases available-app-releases-for-given-build
                              :all-supported-app-releases all-supported-app-releases-full-mapping
                              :plugin-blackliinstall-script-do-deployst plugin-blacklist) 
    ))





                 
                 





