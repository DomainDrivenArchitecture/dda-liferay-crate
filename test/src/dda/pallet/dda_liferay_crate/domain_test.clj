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

(ns dda.pallet.dda-liferay-crate.domain-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as s]
    [dda.pallet.dda-liferay-crate.domain :as sut]))

; --------------------------- Test configs ---------------------------
(def config-simple
  "domainConfigResolved"
  {:liferay-version :LR6
   :fq-domain-name "liferay.example.de"
   :db-root-passwd "test1234"
   :db-user-name "dbtestuser"
   :db-user-passwd "test1234"
   :settings #{ :test}})

(def config-full
  "domainConfigResolved"
  {:liferay-version :LR6
   :fq-domain-name "liferay.example.de"
   :google-id "xxxxxxxxxxxxxxxxxxxxx"
   :db-root-passwd "test1234"
   :db-user-name "dbtestuser"
   :db-user-passwd "test1234"
   :settings #{}
   :releases
     [{:name "LiferayCE"
       :version [7 0 4]
       :app ["ROOT" "https://netcologne.dl.sourceforge.net/project/lportal/Liferay%20Portal/7.0.4%20GA5/liferay-ce-portal-7.0-ga5-20171018150113838.war"]
       :config ["some portal extension lines"]}]

   :backup {:bucket-name "meissa-backup"
            :gpg {:gpg-public-key
                  "-----BEGIN PGP PUBLIC KEY BLOCK-----
  Version: GnuPG v1

  mI0EWbJf3wEEALq5GqjqzlYRLLXa3oz0Ow/nPnzurAPr1+yn2UZinSMk6wgB6ayf
  9DySPvUW9+VLrlLvCSLEo/tX2NwSdn6ZWR454inlmJrzm+G5JPWc4ANhCE61JVME
  l6pPXTQ+CgsPLQspq6leD7+WrBEIKQuXZpcQv2HnirvbNKeYn9eT1xibABEBAAG0
  CHRlc3R1c2VyiLgEEwECACIFAlmyX98CGwMGCwkIBwMCBhUIAgkKCwQWAgMBAh4B
  AheAAAoJEEoEofT3ZQcF/dEEALTrwkAWtpy219Y36pt6IxcvPG8ei8j3/sAP7IX4
  mHR0cTObZQO2jKaTr8hBRNX9Xt/kiLflIbj9oIt56WeT31FFjISI8wg+mT9e8F3J
  WHZTsvoXmHuQs4m2Vk/9Wkkcvv5uJuhzM+SAsB24cvkluLeUaxjZ8HgIN+LCyGlL
  ioDTuI0EWbJf3wEEAOtyW/nlXK6DXhfwZOWemI+QheyIzgnR21rz2emUIBRIzgc4
  OU/SSFTIIOo1e/S8ReCV6/cLK6HubWb79TG32SSEF0jxcbY8SA3ylvJeHnfOpwOh
  RDDyNSJU422kNt6ojblQZ+bh8OQciZcOoNuNlVVBelWlt1t8kJGA2+hOkCehABEB
  AAGInwQYAQIACQUCWbJf3wIbDAAKCRBKBKH092UHBf8NA/9bZdANGsp5uLxyaMIO
  J/wzlkhuyRpR9Ht+VdzARdzFEQLo77TLGEA1N/Sa2afgD3VxveR4LZaWhFK6F670
  CN8eNvWgBPPb5UT/wngyJZTPWkkcKUx0v5k4J6K1aCXLNPNTN94SBRsohEw2pxp4
  tszJa3+UaMuSzvG+Tlv8woAv8w==
  =WIFr
  -----END PGP PUBLIC KEY BLOCK-----"
                  :gpg-private-key
                  "-----BEGIN PGP PRIVATE KEY BLOCK-----
  Version: GnuPG v1

  lQH+BFmyX98BBAC6uRqo6s5WESy12t6M9DsP5z587qwD69fsp9lGYp0jJOsIAems
  n/Q8kj71FvflS65S7wkixKP7V9jcEnZ+mVkeOeIp5Zia85vhuST1nOADYQhOtSVT
  BJeqT100PgoLDy0LKaupXg+/lqwRCCkLl2aXEL9h54q72zSnmJ/Xk9cYmwARAQAB
  /gMDAvL6QaUiGwhSYDLKtIpCG/YCg8BFmZ5iN6mzmfWrkxPWKiU7/jlrf6sk5uDX
  ZNZtlR6MUfNlcywhqG4IA+ObJanJO5N5PSm+AbprOsMTc50KqHIqry7uTuYR9pok
  8Mq3ZU1VQmkKmRm8mOVTaEmQsWdgCrB2FcIOrhaMJWZF4VjmwITn5aZeHpHABMAh
  LpBYvJCEA2PaFsi6Tp340ZxKdgQVI63nWjPvMZZY19BWqyPsYDFW1VUrua1epNRg
  GRY0BLoEJyhitaYfM7Z0wIFACZ7DcDPw4rE8CovPCYJmAIyeSslf1HLlI/fgyon4
  zJwNNzArexzXJElIHtTaDsaUZCobN0CkzbVRgimX8aebXd+YDDruU0qBPjNyY5DE
  wjmKsm4NEVOP4u+qGzCWulYI3Bgq/KYpYos7D9Fm0LyZdKrFKm4x/d0OnGk832ej
  GjdC2LJCynzYWaxDvqRmW9+nlbzwcBZ4kykrkTBTrn8qtAh0ZXN0dXNlcoi4BBMB
  AgAiBQJZsl/fAhsDBgsJCAcDAgYVCAIJCgsEFgIDAQIeAQIXgAAKCRBKBKH092UH
  Bf3RBAC068JAFracttfWN+qbeiMXLzxvHovI9/7AD+yF+Jh0dHEzm2UDtoymk6/I
  QUTV/V7f5Ii35SG4/aCLeelnk99RRYyEiPMIPpk/XvBdyVh2U7L6F5h7kLOJtlZP
  /VpJHL7+biboczPkgLAduHL5Jbi3lGsY2fB4CDfiwshpS4qA050B/gRZsl/fAQQA
  63Jb+eVcroNeF/Bk5Z6Yj5CF7IjOCdHbWvPZ6ZQgFEjOBzg5T9JIVMgg6jV79LxF
  4JXr9wsroe5tZvv1MbfZJIQXSPFxtjxIDfKW8l4ed86nA6FEMPI1IlTjbaQ23qiN
  uVBn5uHw5ByJlw6g242VVUF6VaW3W3yQkYDb6E6QJ6EAEQEAAf4DAwLy+kGlIhsI
  UmCEqj7yzxwXjnx2awryBn2wUETtXvw3hqiTm+nF6a4qtPzU6k1tn5Gsy9pZajMz
  sfl1CAuUC8bx9PI/JIeaTM4LoJFPxwIn4cwYUIH5GJ/wI9/jF2gEbsS+kRXv9dUS
  vIF6v5y/6aOm3wbwotr/9orRi2oTYHi3P8LNp8llPzNzptlTUdXoHw1xtt1/PutB
  JMrXPGQIdicNzRhBL52g+mzMlGBn6rMkcdF+oAxcwNTprjjOnsEBRob+CikDR6H4
  Qj6viZ3hQ3NNax1GKul3fbvrJn+Q48TiVH5hiN6DG5qC+y6tVvkU4BCDWRmvSNYk
  kk9PZjFdYItZF9/WyAVJis0JXOniMaP254VO+XHVOmESu2YiXoWmFwXct3GLg5t/
  TpdCjSDV8F6Th9deuVUloyKkti3MyHZmkdSX6pspZd5UnPlflzLu3a/aXleOhdru
  uFkFxI7K0IftIXqk2vLa7R8oOn5CwYifBBgBAgAJBQJZsl/fAhsMAAoJEEoEofT3
  ZQcF/w0D/1tl0A0aynm4vHJowg4n/DOWSG7JGlH0e35V3MBF3MURAujvtMsYQDU3
  9JrZp+APdXG95HgtlpaEUroXrvQI3x429aAE89vlRP/CeDIllM9aSRwpTHS/mTgn
  orVoJcs081M33hIFGyiETDanGni2zMlrf5Roy5LO8b5OW/zCgC/z
  =0/aC
  -----END PGP PRIVATE KEY BLOCK-----
  "
                  :gpg-passphrase "passphrase"}
            :aws {:aws-access-key-id "some id"
                  :aws-secret-access-key "some secret"}}})

; -------------------------------- Tests ------------------------------})
(deftest test-httpd-configuration
  (testing
    "test the httpd config creation"
    (is (thrown? Exception (sut/httpd-domain-configuration {})))
    (is (= "liferay.example.de"
          (:domain-name (sut/httpd-domain-configuration config-simple))))
    (is (= "xxxxxxxxxxxxxxxxxxxxx"
          (:google-id (sut/httpd-domain-configuration config-full))))))

(deftest test-backup-configuration
  (testing
    "test the backup config creation"
    (is (thrown? Exception (sut/backup-domain-configuration {})))
    (is (= "liferay"
           (:backup-name (sut/backup-domain-configuration config-full))))))

(deftest test-infra-configuration
  (testing
    "test the infra config creation"
    (is (thrown? Exception (sut/infra-configuration {})))
    (is (= "liferay.example.de"
           (:fq-domain-name (:dda-liferay-crate (sut/infra-configuration config-full)))))))
