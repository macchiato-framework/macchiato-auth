(defproject macchiato/auth "0.0.10"
  :description "a library that provides authentication and authorization facilities"
  :url "https://github.com/yogthos/macchiato-framework/macchiato-auth"
  :scm {:name "git"
         :url "https://github.com/macchiato-framework/macchiato-auth.git"}
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :clojurescript? true
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.191" :scope "provided"]
                 [macchiato/core "0.2.10"]
                 [clout "2.2.0"]]
  :plugins [[codox "0.6.4"]
            [lein-doo "0.1.7"]
            [macchiato/lein-npm "0.6.6"]
            [lein-cljsbuild "1.1.4"]]
  :npm {:name "@macchiato/auth"
        :write-package-json true
        :private false
        :directories {:lib "src"}
        :files ["src/*"]
        :author {:name "Dmitri Sotnikov"
                 :email "dmitri.sotnikov@gmail.com"
                 :url "http://yogthos.net/"}}
  :profiles {:test
             {:cljsbuild
                   {:builds
                    {:test
                     {:source-paths ["src" "test"]
                      :compiler     {:main          macchiato.test.runner
                                     :output-to     "target/test/core.js"
                                     :target        :nodejs
                                     :optimizations :none
                                     :source-map    true
                                     :pretty-print  true}}}}
              :doo {:build "test"}}}

  :aliases
  {"test"
   ["do"
    ["npm" "install"]
    ["clean"]
    ["with-profile" "test" "doo" "node" "once"]]
   "test-watch"
   ["do"
    ["npm" "install"]
    ["clean"]
    ["with-profile" "test" "doo" "node"]]})
