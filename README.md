# dda-liferay-crate

[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-liferay-crate.svg)](https://clojars.org/dda/dda-liferay-crate)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-liferay-crate.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-liferay-crate)

[![Slack](https://img.shields.io/badge/chat-clojurians-green.svg?style=flat)](https://clojurians.slack.com/messages/#dda-pallet/) | [<img src="https://domaindrivenarchitecture.org/img/meetup.svg" width=50 alt="DevOps Hacking with Clojure Meetup"> DevOps Hacking with Clojure](https://www.meetup.com/de-DE/preview/dda-pallet-DevOps-Hacking-with-Clojure) | [Website & Blog](https://domaindrivenarchitecture.org)


This is a crate to install, configure and run a full blown 3 tier liferay server via Pallet.

Currently this crate uses Apache2 as httpd, mariadb as db and tomcat7 as web application server on Ubuntu.

If you are interested in enhancing this to provide additional
configuration options or to work with other linux flavors,
contributions are welcome!

## compatability

This crate is working with:
 * clojure 1.7
 * pallet 0.8
 * ubuntu 16.04
 * apache httpd 2.4
 
 
 
 * apache tomcat7
 * oracle java1.6 as specified by liferay 6.2
 * liferay 6.2.1 or liferay7



## Features

This crate installs liferay with all the necessary components. These components are liferay, httpd, tomcat, maraidb and backups.

## Usage documentation
This crate is responsible for configuring and installing liferay. 

### Prepare vm
This crate was tested on an installed ubuntu 16.04 installation. 
1. Install ubuntu16.04
2. In some cases update and upgrade can fix some minor problems. Be sure the remote machine has a running ssh-service.
```
sudo apt-get update
sudo apt-get upgrade
sudo apt-get install openssh-server
```

### Configuration
The configuration consists of two files defining both WHERE to install the software and WHAT to install and configure.
* `targets.edn`: describes on which target system(s) the software will be installed
* `liferay.edn`: describes the configuration of the application-server.

Examples of these files can be found in the root directory of this repository.

#### Targets config example
Example content of file `targets.edn`:
```clojure
{:existing [{:node-name "test-vm1"            ; semantic name
             :node-ip "35.157.19.218"}]       ; the ip4 address of the machine to be provisioned
 :provisioning-user {:login "initial"         ; account used to provision
                     :password "secure1234"}} ; optional password, if no ssh key is authorized
```

#### Liferay config example
Example content of file `liferay.edn`:
```clojure
; specification of installation and configuration for liferay and required SW
{:liferay-version :LR6                  ;specifies the Liferay version to be installed
 :fq-domain-name "example.de"           ; the full qualified domain name
 :google-id "xxxxxxxxxxxxxxxxxxxxx"     ; your google id
 :db-root-passwd {:plain "test1234"}    ; the root password for the database
 :db-user-name "dbtestuser"             ; the database user
 :db-user-passwd {:plain "test1234"}    ; the user password for the database
 :settings #{:test}}                    ; multiple keywords can be set. E.g. :test will use snakeoil certificates

```         

For `Secret` you can find more adapters in dda-palet-commons. 

#### Use Integration 
The dda-liferay-crate provides easy access to the required installation and configuration process.
To apply your configuration simply create the necessary files and proceed to the corresponding integration namespace.
For example:
```clojure
(in-ns 'dda.pallet.dda-liferay-crate.app.instantiate-existing)
(apply-install)
(apply-configure)
```   
This will apply the installation and configuration process to the provided targets defined in targets.edn.

### Watch log for debug reasons
In case of problems you may want to have a look at the log-file:
`less logs/pallet.log`

## Reference
Some details about the architecture: We provide two levels of API. **Domain** is a high-level API with many build in conventions. If this conventions don't fit your needs, you can use our low-level **infra** API and realize your own conventions.

### Domain API

#### Targets
The schema for the targets config is:
```clojure
(def ExistingNode {:node-name Str                   ; your name for the node
                   :node-ip Str                     ; nodes ip4 address       
                   })

(def ProvisioningUser {:login Str                   ; user account used for provisioning / executing tests
                       (optional-key :password) Str ; password, is no authorized ssh key is avail.
                       })

(def Targets {:existing [ExistingNode]              ; nodes to test or install
              :provisioning-user ProvisioningUser   ; common user account on all nodes given above
              })
```
The "targets.edn" uses this schema.

#### Liferay config
The schema for the liferay configuration is:
```clojure
(def DomainConfig
  "The high-level domain configuration for the liferay-crate."
  {:liferay-version LiferayVersion
   :fq-domain-name s/Str
   (s/optional-key :google-id) s/Str
   :db-root-passwd secret/Secret
   :db-user-name s/Str
   :db-user-passwd secret/Secret
   ;if :test is specified in :settings, snakeoil certificates will be used
   :settings (hash-set (s/enum :test))
   (s/optional-key :backup) {:bucket-name s/Str
                             :gpg {:gpg-public-key  secret/Secret
                                   :gpg-private-key secret/Secret
                                   :gpg-passphrase  secret/Secret}
                             :aws {:aws-access-key-id secret/Secret
                                   :aws-secret-access-key secret/Secret}}})
```

For `Secret` you can find more adapters in dda-palet-commons.

### Infra API
The Infra configuration is a configuration on the infrastructure level of a crate. It contains the complete configuration options that are possible with the crate functions. You can find the details of the infra configurations at the other crates used:
* [dda-tomcat-crate](https://github.com/DomainDrivenArchitecture/dda-tomcat-crate)
* [dda-backup-crate](https://github.com/DomainDrivenArchitecture/dda-backup-crate)
* [dda-httpd-crate](https://github.com/DomainDrivenArchitecture/dda-httpd-crate)
* [dda-mariadb-crate](https://github.com/DomainDrivenArchitecture/dda-mariadb-crate)


## License
Published under [apache2.0 license](LICENSE.md)