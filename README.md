# About dda-liferay-crate

[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-liferay-crate.svg)](https://clojars.org/dda/dda-liferay-crate)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-liferay-crate.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-liferay-crate)

[![Slack](https://img.shields.io/badge/chat-clojurians-green.svg?style=flat)](https://clojurians.slack.com/messages/#dda-pallet/) | [<img src="https://domaindrivenarchitecture.org/img/meetup.svg" width=50 alt="DevOps Hacking with Clojure Meetup"> DevOps Hacking with Clojure](https://www.meetup.com/de-DE/preview/dda-pallet-DevOps-Hacking-with-Clojure) | [Website & Blog](https://domaindrivenarchitecture.org)


This is a crate to install, configure and run a full blown 3 tier liferay server on a Linux system.

This version of this crate uses Apache2 as httpd server, mariadb as database and tomcat8 as web application server on Ubuntu for Liferay version 7.x (for backwards compatibility also Liferay version 6.x can be installed, but is not recommended anymore).

## Table of contents

* [Compatibility](#compatibility)
* [Features](#features)
* [Usage documentation](#usage-documentation)
  + [1. Prepare your target machine](#1-prepare-your-target-machine)
  + [2. Download installer and configuration facilities](#2-download-installer-and-configuration-facilities)
  + [3. Adapt the configuration files](#3-adapt-the-configuration-files)
    - [Targets config example](#targets-config-example)
    - [Liferay config example](#liferay-config-example)
  + [4. Execute installation](#4-execute-installation)
  + [5. Deploy and configure liferay](#5-deploy-and-configure-liferay)
    - [Deploy liferay to tomcat](#deploy-liferay-to-tomcat)
    - [Start liferay](#start-liferay)
  + [Watch log for debug reasons](#watch-log-for-debug-reasons)
* [Reference](#reference)
  + [Domain API](#domain-api)
    - [Targets](#targets)
    - [Liferay config](#liferay-config)
  + [Infra API](#infra-api)
* [License](#license)


## Compatibility

This crate is working with:
 * clojure 1.7
 * pallet 0.8
 * ubuntu 16.04
 * apache httpd 2.4
 * mariadb 10.x
 * apache tomcat8
 * openjdk8
 * LiferayCE-7.0.4-ga5

## Features

This crate installs Liferay with all the necessary components: httpd, tomcat, mariadb and backup facilities.

This software can be installed on a Linux standalone system, but usually would be installed on a Linux *virtual machine*. For this reason we usually refer to the target system as "virtual machine" in the text below.

## Usage documentation
You can install the liferay crate by following the steps below:

### 1. Prepare your target machine
This crate was tested on an installed ubuntu 16.04 installation. If not already prepared, please perfom the following actions on the target machine:
1. Install ubuntu16.04
2. Ensure your system is up-to-date, has java installed and openssh-server running:
```
sudo apt-get update
sudo apt-get upgrade
sudo apt-get install openjdk8
sudo apt-get install openssh-server
```
By the way, openssh-server is not required when installing this crate on the local machine instead of remotely.

### 2. Download installer and configuration facilities
1. First download the [installer](https://github.com/DomainDrivenArchitecture/dda-liferay-crate/releases/tag/dda-liferay-crate-0.x.x).
<!--- TODO update links --->
1. Then download the 2 example configuration files into the same folder where you've saved the installer.  
 * [targets.edn](https://github.com/DomainDrivenArchitecture/dda-liferay-create/blob/master/targets.edn)  
 * [liferay.edn](https://github.com/DomainDrivenArchitecture/dda-liferay-create/blob/master/targets.edn)

### 3. Adapt the configuration files
The configuration for installing this crate consists of two files, which specify both WHERE to install the software and WHAT to install.
* `targets.edn`: describes on which target system(s) the software will be installed.
* `liferay.edn`: describes the configuration of the application-server.

You need to adjust the values of the fields of `targets.edn` according to your own settings. The file `liferay.edn` can be used as given, if you just want to create a demo installation.

#### Targets config example
Example content of file `targets.edn`:
```clojure
{:existing [{:node-name "test-vm1"            ; semantic name (keep the default or use a name that suits you)
             :node-ip "192.168.56.104"}]      ; the ip4 address of the machine to be provisioned
 :provisioning-user {:login "initial"         ; user on the target machine, must have sudo rights
                     :password "secure1234"}} ; password can be empty, if a ssh key is authorized
```

#### Liferay config example
Example content of file `liferay.edn`:
```clojure
{:liferay-version :LR7                  ; specifies the Liferay version to be installed either :LR7 or :LR6
 :fq-domain-name "example.de"           ; the full qualified domain name
 :db-root-passwd {:plain "test1234"}    ; the root password for the database
 :db-user-name "dbtestuser"             ; the database user
 :db-user-passwd {:plain "test1234"}    ; the user password for the database
 :settings #{:test}}                    ; multiple keywords can be set. E.g. :test will use snakeoil certificates

```

Instead of using plain passwords, you can use the possibilities of other **secrets**. For more information about this topic, please refer to [dda-pallet-commons](https://github.com/DomainDrivenArchitecture/dda-pallet-commons/blob/master/doc/secret_spec.md).

### 4. Execute installation
You can start the installation in a terminal by running the installer with the name of the `liferay.edn`-file:
<!--- TODO update version --->
```bash
java -jar dda-liferay-ide-0.3.3-standalone.jar liferay.edn
```
This will apply the installation and configuration process to the provided targets defined in `targets.edn`. This can take several minutes, as a lot of software needs to be installed. In case of success you'll see something similar as:

### 5. Deploy and configure liferay
To finish your installation and to set up liferay properly several manual steps on the targets are required:

#### Deploy liferay to tomcat
Stop tomcat and use the rollout script.

```bash
sudo service tomcat8 stop                            #use tomcat7 for liferay 6
sudo /var/lib/liferay/do-rollout.sh LiferayCE-7.0.4  #different version for liferay 6
```
Note, the Liferay version above (7.0.4) is just an example. You get a list of all possible versions by running the rollout-script without parameters:
```bash
sudo /var/lib/liferay/do-rollout.sh
```

#### Start liferay
Perform the following steps in order to finish the liferay setup.
* Restart apache and start tomcat with liferay now deployed:
```bash
sudo service apache2 restart
sudo service tomcat8 start           #use tomcat7 for liferay 6
```
* Open browser with the url or ip-address where you installed liferay (e.g. http://localhost in case you installed it locally or open the browser from within the target machine). If no valid certificates were supplied, you will see the security warning of your browser (e.g. if you had used :test settings, then dummy snakeoil certificates were installed). In this case you still can access liferay by using the following or similar steps to add an exception to your browser: Click "Advanced" > "Add Exception" > "Confirm Security Exception".
* Next you should see the liferay basic configuration screen with fields already filled in, like the database configuration. Adjust the settings according to you needs, if you want, then click **Finish configuration** button. Note, that this may take some minutes dependent on your environment.
* In case of succes you'll see the message **Your configuration was saved successfully... Please restart the portal now.**
* Copy the just created liferay configuration properties to the appropriate tomcat folder by:
```bash
sudo cp /var/lib/liferay/portal-setup-wizard.properties /var/lib/tomcat8/webapps/ROOT/WEB-INF/classes/portal-ext.properties
```
* Restart tomcat with the new liferay portal properties:
```bash
sudo service tomcat8 restart
```
* In the browser open the url or ip-address where you installed liferay (e.g. http://localhost in case you installed it locally). The default userid and password are test@liferay.com respectively test, which should be changed after the successful setup. **Note**, that it may take some minutes to open the page, dependent on your environment.
* On the pages which come up, please proceed with accepting the license and completing the the password reminder.
* In the end you should see the liferay welcome page, e.g. "Hello World".


### Watch log for debug reasons
In case of problems you may want to have a look at the log-file:
`less logs/pallet.log`


## Backup
The dda-liferay-crate installs a facility (a so-called cron-job) which makes a backup of the liferay data on a regular base. The data backed up consists of:
* the liferay database (i.e. database name: lportal)
* all files and folders in the liferay data folder: "/var/lib/liferay/data/"

### Backup file locations
The scripts for backing up and restore can be found at: "/usr/local/lib/dda-backup/"
```bash
liferay_backup.sh
liferay_restore.sh
```

The files containing the data backed up are stored at: "/var/backups/transport-outgoing/".  
If you'd like to save your data backups at another place you may transfer the files from you to the appropriate location.

When restoring, the data to be restored needs to be placed in folder: ""

### Manual backup
A backup can also be triggered manually by running the backup script in a terminal:
```bash
sudo /usr/ ...
```

### Restore a backup
A backup can also be triggered manually by running the backup script in a terminal:
```bash
sudo /usr/ ...
```


## Reference
Some details about the architecture: We provide two levels of API. **Domain** is a high-level API with many build in conventions. If this conventions don't fit your needs, you can use our low-level **infra** API and realize your own conventions.

### Domain API

#### Target configuration
The schema for the targets config (used in file "targets.edn"):
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


#### Liferay configuration
The schema for the liferay configuration (used in file "liferay.edn"):
```clojure
(def LiferayVersion (s/enum :LR6 :LR7))

(def LiferayApp
  "Represents a liferay application (portlet, theme or the portal itself)."
  [(s/one s/Str "name") (s/one s/Str "url")])

(def LiferayRelease
  "LiferayRelease contains a release name with specification of versioned apps."
  {:name s/Str
   :version version/Version
   (s/optional-key :app) LiferayApp
   (s/optional-key :config) [s/Str]
   (s/optional-key :hooks) [LiferayApp]
   (s/optional-key :layouts) [LiferayApp]
   (s/optional-key :themes) [LiferayApp]
   (s/optional-key :portlets) [LiferayApp]
   (s/optional-key :ext) [LiferayApp]})

(def DomainConfig
  "The high-level domain configuration for the liferay-crate."
  {:liferay-version LiferayVersion       ;either :LR7 or :LR6
   :fq-domain-name s/Str                 ;the fully qualified domain name
   (s/optional-key :google-id) s/Str
   :db-root-passwd secret/Secret
   :db-user-name s/Str
   :db-user-passwd secret/Secret
   ;if :test is specified in :settings, snakeoil certificates will be used
   :settings (hash-set (s/enum :test))
   (s/optional-key :releases) [LiferayRelease]     ;when missing a default will be used
   (s/optional-key :backup) {:bucket-name s/Str
                             :gpg {:gpg-public-key  secret/Secret
                                   :gpg-private-key secret/Secret
                                   :gpg-passphrase  secret/Secret}
                             :aws {:aws-access-key-id secret/Secret
                                   :aws-secret-access-key secret/Secret}}})
```

For `Secret` you can find more adapters in dda-pallet-commons.

### Infra API
The Infra configuration is a configuration on the infrastructure level of a crate. It contains the complete configuration options that are possible with the crate functions. It is defined as specified below:
```clojure
(def LiferayApp
  "Represents a liferay application (portlet, theme or the portal itself)."
  [(s/one s/Str "name") (s/one s/Str "url")])

(def LiferayRelease
  "LiferayRelease contains a release name with specification of versioned apps."
  {:name s/Str
   :version version/Version
   (s/optional-key :app) LiferayApp
   (s/optional-key :config) [s/Str]
   (s/optional-key :hooks) [LiferayApp]
   (s/optional-key :layouts) [LiferayApp]
   (s/optional-key :themes) [LiferayApp]
   (s/optional-key :portlets) [LiferayApp]
   (s/optional-key :ext) [LiferayApp]})

(def LiferayCrateConfig
  "The infra config schema."
  {:fq-domain-name s/Str
   :home-dir dir-model/NonRootDirectory
   :lib-dir dir-model/NonRootDirectory
   :deploy-dir dir-model/NonRootDirectory
   (s/optional-key :osgi) osgi/OsgiConfig
   :repo-download-source s/Str
   :dependencies [s/Str]
   :release-dir dir-model/NonRootDirectory
   :releases [LiferayRelease]
   :tomcat {:tomcat-root-dir s/Str
            :tomcat-webapps-dir s/Str
            :tomcat-user s/Str
            :tomcat-service s/Str}
   :db {:db-name s/Str
        :db-user-name s/Str
        :db-user-passwd s/Str}})
```

## License
Published under [apache2.0 license](LICENSE.md)
