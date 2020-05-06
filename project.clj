(defproject segmentum "0.1.0-SNAPSHOT"

  :description "FIXME: write description"

  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.clojure/core.async "1.1.587"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.10.0"]
                 [clojure.java-time "0.3.2"]
                 [conman "0.8.8"]
                 [cprop "0.1.16"]
                 [expound "0.8.4"]
                 [funcool/struct "1.4.0"]
                 [luminus-aleph "0.1.6"]
                 [luminus-migrations "0.6.7"]
                 [luminus-transit "0.1.2"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.4"]
                 [metosin/muuntaja "0.6.6"]
                 [metosin/reitit "0.4.2"]
                 [metosin/ring-http-response "0.9.1"]
                 [mount "0.1.16"]
                 [nrepl "0.7.0"]
                 [org.postgresql/postgresql "42.2.12"]
                 [org.webjars.npm/bulma "0.8.2"]
                 [org.webjars.npm/material-icons "0.3.1"]
                 [org.webjars/webjars-locator "0.40"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.23"]
                 [patika "0.1.11"]
                 [com.rpl/defexception "0.2.0"]
                 [potemkin "0.4.5"]
                 [kezban "0.1.90"]
                 [amalloy/ring-gzip-middleware "0.1.4"]
                 [medley "1.3.0"]
                 [prismatic/schema "1.1.12"]]

  :min-lein-version "2.0.0"

  :source-paths ["src"]

  :test-paths ["test"]

  :resource-paths ["resources"]

  :target-path "target/%s/"

  :main ^:skip-aot segmentum.core

  :plugins [[pisano/lein-kibit "0.1.2"]
            [ertu/lein-cljfmt "0.1.1"]
            [lein-ancient "0.6.15"]]

  :cljfmt {:indents {#".*" [[:inner 0]]} :more-newlines? true}

  :jvm-opts ^:replace ["-server"
                       "-XX:-OmitStackTraceInFastThrow"
                       "-XX:+UseConcMarkSweepGC"
                       "-XX:+CMSParallelRemarkEnabled"
                       "-XX:+UseCMSInitiatingOccupancyOnly"
                       "-XX:CMSInitiatingOccupancyFraction=70"
                       "-XX:+ScavengeBeforeFullGC"
                       "-XX:+CMSScavengeBeforeRemark"
                       "-Djdk.attach.allowAttachSelf=true"]

  :profiles
  {:uberjar       {:omit-source    true
                   :aot            :all
                   :uberjar-name   "segmentum.jar"
                   :source-paths   ["env/prod/clj"]
                   :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev   {:jvm-opts       ["-Dconf=dev-config.edn"]

                   :dependencies   [[pjstadig/humane-test-output "0.10.0"]
                                    [prone "2020-01-17"]
                                    [ring/ring-devel "1.8.1"]
                                    [ring/ring-mock "0.4.0"]
                                    [jonase/eastwood "0.3.11" :exclusions [org.clojure/clojure]]]

                   :plugins        [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                    [jonase/eastwood "0.3.11" :exclusions [org.clojure/clojure]]]

                   :source-paths   ["env/dev/clj"]

                   :resource-paths ["env/dev/resources"]

                   :repl-options   {:init-ns user :timeout 120000}

                   :injections     [(require 'pjstadig.humane-test-output)
                                    (pjstadig.humane-test-output/activate!)]

                   :eastwood
                                   {:source-paths    ["src"]
                                    :config-files    ["./eastwood-config.clj"]
                                    :add-linters     [:unused-private-vars
                                                      ;; These linters are pretty useful but give a few false positives and can't be selectively
                                                      ;; disabled (yet)
                                                      ;;
                                                      ;; For example see https://github.com/jonase/eastwood/issues/193
                                                      ;;
                                                      ;; It's still useful to re-enable them and run them every once in a while because they catch
                                                      ;; a lot of actual errors too. Keep an eye on the issue above and re-enable them if we can
                                                      ;; get them to work
                                                      #_:unused-fn-args
                                                      #_:unused-locals]
                                    :exclude-linters [; Turn this off temporarily until we finish removing self-deprecated functions & macros
                                                      :deprecations
                                                      ;; this has a fit in libs that use Potemin `import-vars` such as `java-time`
                                                      :implicit-dependencies
                                                      ;; too many false positives for now
                                                      :unused-ret-vals]}}
   :project/test  {:jvm-opts       ["-Dconf=test-config.edn"]
                   :resource-paths ["env/test/resources"]}
   :profiles/dev  {}
   :profiles/test {}})
