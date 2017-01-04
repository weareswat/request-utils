(defproject weareswat/request-utils "0.7.0"
  :description "Some utilities to do async http requests with aleph"
  :url "https://github.com/weareswat/request-utils"
  :license {:name         "MIT"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [aleph "0.4.1"]
                 [environ "1.0.3"]
                 [clanhr/result "0.11.0"]
                 [ring/ring-codec "1.0.1"]
                 [cheshire "5.6.2"]
                 [org.clojure/core.async "0.2.385"]]

  :scm {:name "git"
        :url "https://github.com/weareswat/request-utils.git"}

  :aliases {"autotest" ["trampoline" "with-profile" "+test" "test-refresh"]
            "test"  ["trampoline" "test"]}

  :profiles {:test {:plugins [[com.jakemccrary/lein-test-refresh "0.15.0"]]}}

  :test-refresh {:quiet true
                 :changes-only true})
