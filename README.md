# dda-liferay-crate

This is a crate to install, configure and run a full blown 3 tier liferay server via Pallet.

Currently this crate uses Apache2 as httpd, mysql as db and tomcat7 as web applicatin server on Ubuntu.

If you are interested in enhancing this to provide additional
configuration options or to work with other linux flavors,
contributions are welcome!

## compatability

This crate is working with:
 * clojure 1.7
 * pallet 0.8
 * ubuntu 14.04
 * apache httpd 2.4
 * apache tomcat7
 * oracle java1.6 as specified by liferay 6.2
 * oracle mysql
 * liferay 6.2.1

## Features
 * comunicate using https
 * integrate with backup
 
## Usage Examples

### Configuration
(ns some-server-spec
  (:require
      [pallet.api :as api]
      [clojure.java.io :as io]
      [org.domaindrivenarchitecture.pallet.crate.config.node :as node-record]
      [org.domaindrivenarchitecture.pallet.crate.liferay :as liferay])

(def ^:dynamic liferay-website
  (let [db-user-passwd "userpwd"
        db-name "lportal"
        db-user-name "liferay_user"]
    (node-record/new-node 
      :host-name "www"  
      :pallet-cm-user-name "root"
      :additional-config 
      {:dda-liferay 
       {:db {:root-passwd "rootpwd"
             :db-name db-name
             :user-name db-user-name
             :user-passwd db-user-passwd}
        :httpd {:fqdn "www.somedomain.de"
                :domain-cert (slurp (io/resource "certfile"))
                :domain-key (slurp (io/resource "keyfile"))
                :ca-cert (slurp (io/resource "intermediate"))}
        :tomcat {:Xmx "6072m"}
        :third-party-download-root-dir "artifact-root-dir"
        :releases [liferay/default-release]
        }}
      )))

### Server Specification
(ns some-server-spec
  (:require
      [pallet.api :as api]
      [org.domaindrivenarchitecture.pallet.crate.config :as config]
      [org.domaindrivenarchitecture.pallet.crate.liferay :as liferay])

(def ^:dynamic liferay-group
  (api/group-spec
    "liferay-group"
    :extends 
    [(config/with-config meissa-config/config) 
    liferay/with-liferay]))

(defn -main
  [group-spec & {:keys [phase] :or {phase '(:settings :configure)}}]
  (cm-base/execute-main group-spec provider :phase phase))

### Run pallet  
(-main liferay-group :phase '(:settings :install :configure :prepare-rollout))


## Usage Examples
(init-node :group-spec liferay-group :id :default-instance)
(-main liferay-group :phase '(:settings :install :configure))

## License

Copyright © 2016, meissa GmbH 

Distributed under the Apache2 License.

