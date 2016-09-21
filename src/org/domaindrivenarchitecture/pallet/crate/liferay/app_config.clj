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


(ns org.domaindrivenarchitecture.pallet.crate.liferay.app-config
  (require
    [schema.core :as s]
    [schema-tools.core :as st]
    [pallet.stevedore :as stevedore]
    [pallet.script.scriptlib :as lib]
    [pallet.stevedore.bash :as bash]
    [org.domaindrivenarchitecture.config.commons.directory-model :as dir-model]
    [org.domaindrivenarchitecture.pallet.crate.mysql :as mysql]
    [org.domaindrivenarchitecture.pallet.crate.liferay.release-model :as schema]))

(def etc-default-tomcat7
  ["TOMCAT7_USER=tomcat7"
   "TOMCAT7_GROUP=tomcat7"
   "#JAVA_HOME=/usr/lib/jvm/openjdk-6-jdk"
   (str "JAVA_OPTS=\"-Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true " 
        "-Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false "
        "-Duser.timezone=GMT"
        "-Xms1536m -Xmx2560m -XX:MaxPermSize=512m -XX:+UseConcMarkSweepGC\"")
   "#JAVA_OPTS=\"${JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n\""
   "TOMCAT7_SECURITY=no"
   "#AUTHBIND=no"]
   )

;this does not neet to be changed for LR7
(def etc-tomcat7-Catalina-localhost-ROOT-xml
  ["<Context path=\"\" crossContext=\"true\">"
   ""
   "  <!-- JAAS -->"
   ""
   "  <!--<Realm"
   "      className=\"org.apache.catalina.realm.JAASRealm\""
   "      appName=\"PortalRealm\""
   "      userClassNames=\"com.liferay.portal.kernel.security.jaas.PortalPrincipal\""
   "      roleClassNames=\"com.liferay.portal.kernel.security.jaas.PortalRole\""
   "  />-->"
   " "
   "  <!--"
   "  Uncomment the following to disable persistent sessions across reboots."
   "  -->"
   " "
   "  <!--<Manager pathname=\"\" />-->"
   " "
   "  <!--"
   "  Uncomment the following to not use sessions. See the property"
   "  \"session.disabled\" in portal.properties."
   "  -->"
   " "
   "  <!--<Manager className=\"com.liferay.support.tomcat.session.SessionLessManagerBase\" />-->"
   ""
   "</Context>"]
  )

(def etc-tomcat7-catalina-properties
  [
   "# Licensed to the Apache Software Foundation (ASF) under one or more"
   "# contributor license agreements.  See the NOTICE file distributed with"
   "# this work for additional information regarding copyright ownership."
   "# The ASF licenses this file to You under the Apache License, Version 2.0"
   ""
   "# (the \"License\"); you may not use this file except in compliance with"
   "# the License.  You may obtain a copy of the License at"
   "#"
   "#     http://www.apache.org/licenses/LICENSE-2.0"
   "#"
   "# Unless required by applicable law or agreed to in writing, software"
   "# distributed under the License is distributed on an \"AS IS\" BASIS,"
   "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."
   "# See the License for the specific language governing permissions and"
   "# limitations under the License."
   ""
   "#"
   "# List of comma-separated packages that start with or equal this string"
   "# will cause a security exception to be thrown when"
   "# passed to checkPackageAccess unless the"
   "# corresponding RuntimePermission (\"accessClassInPackage.\"+package) has"
   "# been granted."
   ""
   "package.access=sun.,org.apache.catalina.,org.apache.coyote.,org.apache.tomcat.,org.apache.jasper."
   "#"
   "# List of comma-separated packages that start with or equal this string"
   "# will cause a security exception to be thrown when"
   "# passed to checkPackageDefinition unless the"
   "# corresponding RuntimePermission (\"defineClassInPackage.\"+package) has"
   "# been granted."
   "#"
   "# by default, no packages are restricted for definition, and none of"
   "# the class loaders supplied with the JDK call checkPackageDefinition."
   "#"
   "package.definition=sun.,java.,org.apache.catalina.,org.apache.coyote.,org.apache.tomcat.,org.apache.jasper."
   ""
   "#"
   "#"
   "# List of comma-separated paths defining the contents of the \"common\""
   "# classloader. Prefixes should be used to define what is the repository type."
   "# Path may be relative to the CATALINA_HOME or CATALINA_BASE path or absolute."
   "# If left as blank,the JVM system loader will be used as Catalina's \"common\""
   "# loader."
   "# Examples:"
   "#     \"foo\": Add this folder as a class repository"
   "#     \"foo/*.jar\": Add all the JARs of the specified folder as class"
   "#                  repositories"
   "#     \"foo/bar.jar\": Add bar.jar as a class repository"
   "common.loader=${catalina.base}/lib,${catalina.base}/lib/*.jar,${catalina.home}/lib,${catalina.home}/lib/*.jar,/var/lib/liferay/lib/*.jar"
   ""
   "#"
   "# List of comma-separated paths defining the contents of the \"server\""
   "# classloader. Prefixes should be used to define what is the repository type."
   "# Path may be relative to the CATALINA_HOME or CATALINA_BASE path or absolute."
   "# If left as blank, the \"common\" loader will be used as Catalina's \"server\""
   "# loader."
   "# Examples:"
   "#     \"foo\": Add this folder as a class repository"
   "#     \"foo/*.jar\": Add all the JARs of the specified folder as class"
   "#                  repositories"
   "#     \"foo/bar.jar\": Add bar.jar as a class repository"
   "server.loader=/var/lib/tomcat7/server/classes,/var/lib/tomcat7/server/*.jar"
   ""
   "#"
   "# List of comma-separated paths defining the contents of the \"shared\""
   "# classloader. Prefixes should be used to define what is the repository type."
   "# Path may be relative to the CATALINA_BASE path or absolute. If left as blank,"
   "# the \"common\" loader will be used as Catalina's \"shared\" loader."
   "# Examples:"
   "#     \"foo\": Add this folder as a class repository"
   "#     \"foo/*.jar\": Add all the JARs of the specified folder as class"
   "#                  repositories"
   "#     \"foo/bar.jar\": Add bar.jar as a class repository"
   "# Please note that for single jars, e.g. bar.jar, you need the URL form"
   "# starting with file:."
   "shared.loader=/var/lib/tomcat7/shared/classes,/var/lib/tomcat7/shared/*.jar"
   ""
   "# List of JAR files that should not be scanned for configuration information"
   "# such as web fragments, TLD files etc. It must be a comma separated list of"
   "# JAR file names."
   "# The JARs listed below include:"
   "# - Tomcat Bootstrap JARs"
   "# - Tomcat API JARs"
   "# - Catalina JARs"
   "# - Jasper JARs"
   "# - Tomcat JARs"
   "# - Common non-Tomcat JARs"
   "# - Sun JDK JARs"
   "# - Apple JDK JARs"
   "tomcat.util.scan.DefaultJarScanner.jarsToSkip=\\"
   "bootstrap.jar,commons-daemon.jar,tomcat-juli.jar,\\"
   "annotations-api.jar,el-api.jar,jsp-api.jar,servlet-api.jar,\\"
   "catalina.jar,catalina-ant.jar,catalina-ha.jar,catalina-tribes.jar,\\"
   "jasper.jar,jasper-el.jar,ecj-*.jar,\\"
   "tomcat-api.jar,tomcat-util.jar,tomcat-coyote.jar,tomcat-dbcp.jar,\\"
   "tomcat-i18n-en.jar,tomcat-i18n-es.jar,tomcat-i18n-fr.jar,tomcat-i18n-ja.jar,\\"
   "tomcat-juli-adapters.jar,catalina-jmx-remote.jar,catalina-ws.jar,\\"
   "tomcat-jdbc.jar,\\"
   "commons-beanutils*.jar,commons-codec*.jar,commons-collections*.jar,\\"
   "commons-dbcp*.jar,commons-digester*.jar,commons-fileupload*.jar,\\"
   "commons-httpclient*.jar,commons-io*.jar,commons-lang*.jar,commons-logging*.jar,\\"
   "commons-math*.jar,commons-pool*.jar,\\"
   "jstl.jar,\\"
   "geronimo-spec-jaxrpc*.jar,wsdl4j*.jar,\\"
   "ant.jar,ant-junit*.jar,aspectj*.jar,jmx.jar,h2*.jar,hibernate*.jar,httpclient*.jar,\\"
   "jmx-tools.jar,jta*.jar,log4j*.jar,mail*.jar,slf4j*.jar,\\"
   "xercesImpl.jar,xmlParserAPIs.jar,xml-apis.jar,\\"
   "dnsns.jar,ldapsec.jar,localedata.jar,sunjce_provider.jar,sunmscapi.jar,\\"
   "sunpkcs11.jar,jhall.jar,tools.jar,\\"
   "sunec.jar,zipfs.jar,\\"
   "apple_provider.jar,AppleScriptEngine.jar,CoreAudio.jar,dns_sd.jar,\\"
   "j3daudio.jar,j3dcore.jar,j3dutils.jar,jai_core.jar,jai_codec.jar,\\"
   "mlibwrapper_jai.jar,MRJToolkit.jar,vecmath.jar,\\"
   "junit.jar,junit-*.jar,ant-launcher.jar"
   ""
   "#"
   "# String cache configuration."
   "tomcat.util.buf.StringCache.byte.enabled=true"
   "#tomcat.util.buf.StringCache.char.enabled=true"
   "#tomcat.util.buf.StringCache.trainThreshold=500000"
   "#tomcat.util.buf.StringCache.cacheSize=5000"
   ]
  )

(s/defn portal-ext-properties
  "creates the default portal-ext.properties for mySql."
  [db-config :- mysql/DbConfig]
  ["#"
   "# Techbase"
   "#"
   "liferay.home=/var/lib/liferay"
   "setup.wizard.enabled=true"
   "index.on.startup=false"
   "#"
   "# MySQL"
   "#"
   "jdbc.default.driverClassName=com.mysql.jdbc.Driver"
   (str "jdbc.default.url=jdbc:mysql://localhost:3306/" (st/get-in db-config [:db-name]) 
        "?useUnicode=true&characterEncoding=UTF-8&useFastDateParsing=false")
   (str "jdbc.default.username=" (st/get-in db-config [:user-name]))
   (str "jdbc.default.password=" (st/get-in db-config [:user-passwd]))
   "#"
   "# C3PO" 
   "#"
   "#jdbc.default.acquireIncrement=2"
   "#jdbc.default.idleConnectionTestPeriod=60"
   "#jdbc.default.maxIdleTime=3600"
   "#jdbc.default.maxPoolSize=100"
   "#jdbc.default.minPoolSize=40"
   ""
   "#"
   "# Timeouts"
   "#"
   "com.liferay.util.Http.timeout=1000"
   "session.timeout=120"
   ""])

(s/defn ^:always-validate do-deploy-script
  "Provides the do-deploy script content."
  [prepare-dir :- dir-model/NonRootDirectory 
   deploy-dir :- dir-model/NonRootDirectory
   tomcat-dir :- dir-model/NonRootDirectory]
  (let [application-parts-hot ["hooks" "layouts" "portlets" "themes"]
        ;TODO ext muss hinzugefügt werden
        application-parts-full ["app" "hooks" "layouts" "portlets" "themes" "ext"]]
    (stevedore/with-script-language :pallet.stevedore.bash/bash
      (stevedore/with-source-line-comments false 
        (stevedore/script 
          ;(~lib/declare-arguments [release-dir hot-or-cold])
          ("if [ \"$#\" -eq 0 ]; then")
          (println "\"\"")
          (println "\"Usage is: prepare-rollout [release] [deployment-mode].\"")
          (println "\"  deployment-mode:      [hot|full] hot uses the liferay hot deployment mechanism for deploying portlets, themes, a.s.o.\"")
          (println "\"                                   full restarts tomcat and rolles out the liferay app itself, the configuration and portlets ...\"")
          (println "\"  Available Releases are:\"")
          (pipe (pipe ("find" ~prepare-dir "-mindepth 2 -type d") ("cut -d/ -f6")) ("sort -u"))
          (println "\"\"")
          ("exit 1")
          ("fi")
          ("if [ \"$#\" -ge 3 ]; then")
          (println "\"\"") 
          (println "\"Please specify 2 parameters only!\"")
          (println "\"\"")
          ("exit 1")
          ("fi")
          (if (directory? (str ~prepare-dir @1))
            (if (= @2 "hot") 
              (do
                (doseq [part ~application-parts-hot]
                  ("cp" (str ~prepare-dir @1 "/" @part "/*") ~deploy-dir))
                ("chown tomcat7" (str ~deploy-dir "*")))
              (do
                ("service tomcat7 stop")
                ("rm -rf" (str ~tomcat-dir "*"))
                (doseq [part ~application-parts-full]
                  ("cp" (str ~prepare-dir @1 "/" @part "/*") ~tomcat-dir))
                ("unzip" (str ~tomcat-dir "ROOT.war -d " ~tomcat-dir "ROOT/"))
                ("cp" (str ~prepare-dir @1 "/config/portal-ext.properties") (str ~tomcat-dir "ROOT/WEB-INF/classes/"))
                ("chown tomcat7" (str ~tomcat-dir "*"))
                ("service tomcat7 start")
                ))
            (do 
              (println "\"\"")
              (println "\"ERROR: Specified release does not exist or you don't have the permission for it! Please run again as root! For a list of the available releases, run this script without parameters in order to show the available releases!\" ;")
              (println "\"\"")))
          ))))
  )

