# dda-liferay-crate

[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-liferay-crate.svg)](https://clojars.org/dda/dda-liferay-crate)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-liferay-crate.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-liferay-crate)

[![Slack](https://img.shields.io/badge/chat-clojurians-green.svg?style=flat)](https://clojurians.slack.com/messages/#dda-pallet/) | [<img src="https://domaindrivenarchitecture.org/img/meetup.svg" width=50 alt="DevOps Hacking with Clojure Meetup"> DevOps Hacking with Clojure](https://www.meetup.com/de-DE/preview/dda-pallet-DevOps-Hacking-with-Clojure) | [Website & Blog](https://domaindrivenarchitecture.org)


This is a crate to install, configure and run a full blown 3 tier liferay server via Pallet.

Currently this crate uses Apache2 as httpd server, mariadb as database and tomcat8 as web application server on Ubuntu for Liferay version 7.x (for backwards compatibility also Liferay version 6.x can be installed, but is not recommended anymore).

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
2. Ensure you system is up-to-date, has java installed and openssh-server running:
```
sudo apt-get update
sudo apt-get upgrade
sudo apt-get install openjdk8
sudo apt-get install openssh-server
```
By the way, openssh-server isn't required, if you install this crate locally.

### 2. Download installer and configuration facilities
1. Please download the
<!--- TODO update links --->
[installer](https://github.com/DomainDrivenArchitecture/dda-liferay-crate/releases/tag/dda-liferay-crate-0.x.x).
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
To finish your installation and to set up liferay properly several manual steps are required:

#### Deploy liferay to tomcat
Stop tomcat and use the rollout script.

```bash
sudo service tomcat8 stop
sudo /var/lib/liferay/do-rollout.sh LiferayCE-7.0.4
```
The version of Liferay may be subjected to changed. You can get a list of possible versions to be installed by using the rollout-script without version specified:
```bash
sudo /var/lib/liferay/do-rollout.sh
```

#### Startup liferay
Restart apache and start tomcat with liferay now deployed:

```bash
sudo service apache2 restart
sudo service tomcat8 start
```
Please perform the following steps. Please note, that each step may take some time dependent on your environment.
* Open browser and go to http://localhost
* You'll see the liferay basic configuration screen with fields already filled in, like the database configuration. Adjust the settings according to you needs, if you want, then click **Finish configuration** button.
* In case of succes you'll see the message **Your configuration was saved successfully... Please restart the portal now.**
* Copy the just created liferay configuration properties to the appropriate tomcat folder:
```bash
sudo cp /var/lib/liferay/portal-setup-wizard.properties /var/lib/tomcat8/webapps/ROOT/WEB-INF/classes/portal-ext.properties
```

Restart tomcat with the new liferay settings:

```bash
sudo service tomcat8 restart
```





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
