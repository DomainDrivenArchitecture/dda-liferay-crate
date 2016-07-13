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

(ns org.domaindrivenarchitecture.pallet.crate.liferay.release-model
  (:require 
    [clojure.string :as string]
    [schema.core :as s :include-macros true]
    [org.domaindrivenarchitecture.config.commons.version-model :as version]
    [org.domaindrivenarchitecture.config.commons.directory-model :as directory]))

(def LiferayApp
  "Represents a liferay application (portlet, theme or the portal itself)."
  [(s/one s/Str "name") (s/one s/Str "url")])

(def LiferayRelease
  "LiferayRelease relates a release name with specification of versioned apps."
  {:name s/Str
   :version version/Version
   (s/optional-key :app) LiferayApp
   (s/optional-key :config) [s/Str]
   (s/optional-key :hooks) [LiferayApp]
   (s/optional-key :layouts) [LiferayApp]
   (s/optional-key :themes) [LiferayApp]
   (s/optional-key :portlets) [LiferayApp]
   (s/optional-key :ext) [LiferayApp]
   })

(def LiferayReleaseConfig
  "The configuration for liferay release feature."
  {:release-dir directory/NonRootDirectory
   :releases [LiferayRelease]})

(s/defn default-release
  "The default release configuration."
  [portal-ext-lines]
 {:name "LiferayCE"
  :version [6 2 1]
  :app ["ROOT" "http://iweb.dl.sourceforge.net/project/lportal/Liferay%20Portal/6.2.1%20GA2/liferay-portal-6.2-ce-ga2-20140319114139101.war"]
  :config portal-ext-lines})

(defn app-in-vec? 
  "Returns wheather a liferay app with specified name is in vector apps"
  [apps name]
  (if (empty? apps)
    false
    (if (= (first (last apps)) name)
      true
      (app-in-vec? (pop apps) name))
    ))

(defn merge-apps
  "Merge two vector of apps from right to left. Duplicate apps (same name) are ignored and the
right-most app wins."
  [p1 p2] 
  (apply conj 
         (vec (keep #(if-not (app-in-vec? p2 (first %)) %) p1))
         p2))

(s/defn ^:always-validate merge-releases :- LiferayRelease
  "Merges multiple liferay releases into a combined one. All non-app keys are from the right-most
release. Apps are merged from right to left. Duplicate apps (same name) are ignored and the
right-most app wins." 
 [& vals]
 (apply merge-with 
        (fn [& args] 
          (if (and (every? vector? args) (vector? (ffirst args)))
            (apply merge-apps args)
            (last args))
          ) 
        vals)
 )