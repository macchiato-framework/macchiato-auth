(defproject macchiato/auth "0.0.1"
  :description "a library that provides authentication and authorization facilities"
  :url "https://github.com/yogthos/macchiato-framework/macchiato-auth"
  :scm {:name "git"
         :url "https://github.com/macchiato-framework/macchiato-auth.git"}
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :clojurescript? true
  :dependencies []
  :plugins [[codox "0.6.4"]
            [lein-npm "0.6.2"]]
  :npm {:dependencies [[bcrypt "1.0.2"]
                       [jsonwebtoken "7.2.1"]
                       [scrypt "6.0.3"]]}
  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "1.9.293"]]
              :plugins [[lein-cljsbuild "1.1.4"]]}})
