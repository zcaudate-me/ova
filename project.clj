(defproject im.chit/ova "0.9.1"
  :description "Stateful arrays for clojure"
  :url "http://github.com/zcaudate/ova"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [im.chit/hara "1.0.1"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}}
  :documentation {:files {"index"
                          {:input "test/ova/midje_doc.clj"
                           :title "ova"
                           :sub-title "stateful arrays for clojure"
                           :author "Chris Zheng"
                           :email  "z@caudate.me"}}})