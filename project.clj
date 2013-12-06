(defproject im.chit/ova "1.0.1"
  :description "Stateful arrays for clojure"
  :url "http://github.com/zcaudate/ova"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [im.chit/hara "1.0.1"]]
  :profiles {:dev {:dependencies [[midje "1.6.0"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :documentation {:files {"doc/index"
                          {:input "test/midje_doc/ova_guide.clj"
                           :title "ova"
                           :sub-title "stateful arrays for clojure"
                           :author "Chris Zheng"
                           :email  "z@caudate.me"
                           :tracking "UA-31320512-2"}}})
