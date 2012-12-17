(defproject com.thelastcitadel/lobsang "0.2.0-SNAPSHOT"
  :description "Lobsang is the son of Wen the Eternally Surprised"
  :url "https://github.com/hiredman/lobsang"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.mnode.ical4j/ical4j "1.0.4"]
                 [clj-http "0.6.2"]
                 [ring/ring-core "1.1.6"]]
  :plugins [[lein-swank "1.4.3"]
            [lein-ring "0.7.5"]]
  :ring {:handler lobsang.web/handler
         :init lobsang.web/init})
