(def jetty-version "9.4.53.v20231009")

(defproject org.openvoxproject/trapperkeeper-webserver-jetty9 "4.5.3-SNAPSHOT"
  :description "A jetty9-based webserver implementation for use with the org.openvoxproject/trapperkeeper service framework."
  :url "https://github.com/openvoxproject/trapperkeeper-webserver-jetty9"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.9.1"

  :parent-project {:coords [org.openvoxproject/clj-parent "7.4.1-SNAPSHOT"]
                   :inherit [:managed-dependencies]}

  ;; Abort when version ranges or version conflicts are detected in
  ;; dependencies. Also supports :warn to simply emit warnings.
  ;; requires lein 2.2.0+.
  :pedantic? :abort
  :dependencies [[org.clojure/clojure]
                 [org.clojure/java.jmx]
                 [org.clojure/tools.logging]

                 [org.codehaus.janino/janino]
                 [org.flatland/ordered "1.5.9"]

                 [javax.servlet/javax.servlet-api "3.1.0"]
                 ;; Jetty Webserver
                 [org.eclipse.jetty/jetty-server ~jetty-version
                  :exclusions [org.eclipse.jetty.orbit/javax.servlet]]
                 [org.eclipse.jetty/jetty-servlet ~jetty-version]
                 [org.eclipse.jetty/jetty-servlets ~jetty-version]
                 [org.eclipse.jetty/jetty-webapp ~jetty-version]
                 [org.eclipse.jetty/jetty-proxy ~jetty-version]
                 [org.eclipse.jetty/jetty-jmx ~jetty-version]
                 [org.eclipse.jetty.websocket/websocket-server ~jetty-version]

                 [prismatic/schema]
                 [ring/ring-servlet]
                 [ring/ring-codec]

                 [org.openvoxproject/ssl-utils]
                 [org.openvoxproject/kitchensink]
                 [org.openvoxproject/trapperkeeper]
                 [org.openvoxproject/i18n]
                 [org.openvoxproject/trapperkeeper-filesystem-watcher]

                 [org.slf4j/jul-to-slf4j]]

  :source-paths  ["src"]
  :java-source-paths  ["java"]

  :plugins [[lein-parent "0.3.7"]
            [org.openvoxproject/i18n "0.9.3-SNAPSHOT"]]

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD
                                     :sign-releases false}]]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the code in the test/ directory. Downstream projects can then
  ;; depend on this test jar using a :classifier in their :dependencies to reuse the test utility
  ;; code that we have.
  :classifiers [["test" :testutils]]

  :test-paths ["test/clj"]

  :profiles {:defaults {:source-paths ["examples/multiserver_app/src"
                                       "examples/ring_app/src"
                                       "examples/servlet_app/src/clj"
                                       "examples/war_app/src"
                                       "examples/webrouting_app/src"]
                        :java-source-paths ["examples/servlet_app/src/java"
                                            "test/java"]
                        :dependencies [[org.openvoxproject/http-client]
                                       [org.openvoxproject/kitchensink nil :classifier "test"]
                                       [org.openvoxproject/trapperkeeper nil :classifier "test"]
                                       [org.clojure/tools.namespace]
                                       [compojure]
                                       [ring/ring-core]]
                        :resource-paths ["dev-resources"]
                        :jvm-opts ["-Djava.util.logging.config.file=dev-resources/logging.properties"]}
             :dev [:defaults :pseudo-dev]

             ;; per https://github.com/technomancy/leiningen/issues/1907
             ;; the provided profile is necessary for lein jar / lein install
             :provided {:dependencies [[org.bouncycastle/bcpkix-jdk18on]]
                        :resource-paths ["dev-resources"]}
             ;; a pseudo dev profile that can be combined with the FIPS profiling for testing only
             :pseudo-dev {:dependencies [
                                         [stylefruits/gniazdo nil :exclusions [org.eclipse.jetty.websocket/websocket-api
                                                                               org.eclipse.jetty.websocket/websocket-client
                                                                               org.eclipse.jetty/jetty-util]]]}
             :fips-dependencies {:dependencies [[org.bouncycastle/bcpkix-fips]
                                                [org.bouncycastle/bc-fips]
                                                [org.bouncycastle/bctls-fips]]
                                 :exclusions [[org.bouncycastle/bcpkix-jdk18on]]
                                 ;; this only ensures that we run with the proper profiles
                                 ;; during testing. This JVM opt will be set in the puppet module
                                 ;; that sets up the JVM classpaths during installation.
                                 :jvm-opts ~(let [version (System/getProperty "java.version")
                                                  [major minor _] (clojure.string/split version #"\.")
                                                  unsupported-ex (ex-info "Unsupported major Java version. Expects 11 or 17."
                                                                   {:major major
                                                                    :minor minor})]
                                              (condp = (java.lang.Integer/parseInt major)
                                                17 ["-Djava.security.properties==dev-resources/jdk17-fips-security"]
                                                21 ["-Djava.security.properties==dev-resources/jdk21-fips-security"]
                                                (throw unsupported-ex)))}
             :fips [:defaults :fips-dependencies] ; merge in the default profile
             :testutils {:source-paths ^:replace ["test/clj"]
                         :java-source-paths ^:replace ["test/java"]}}

  :main puppetlabs.trapperkeeper.main)
