(defproject yamper "0.1.0"
  :description "Yet Another Music PlayER"
  :url "https://github.com/vonZeppelin/yamper"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]
                 ;; misc libs
                 [alandipert/storage-atom "2.0.1"]
                 [com.cemerick/url "0.1.1"]
                 [cljs-http "0.1.44"]
                 ;; ui libs
                 [reagent "0.7.0"]
                 [baking-soda "0.1.3" :exclusions [cljsjs/reactstrap]]
                 [cljsjs/bootstrap-notify "3.1.3-0"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-exec "0.3.7"]
            [lein-figwheel "0.5.14"]]

  :resource-paths ["public"]
  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :figwheel {:http-server-root "."
             :nrepl-port 7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs ["public/css"]}

  :cljsbuild {:builds {:app {:source-paths ["src" "env/dev/cljs"]
                             :compiler {:main "yamper.dev"
                                        :output-to "public/js/app.js"
                                        :output-dir "public/js/out"
                                        :asset-path "js/out"
                                        :source-map true
                                        :optimizations :none
                                        :pretty-print true}
                             :figwheel {:on-jsload "yamper.core/mount-root"
                                        :open-urls ["http://localhost:3449/index.html"]}}
                       :release {:source-paths ["src" "env/prod/cljs"]
                                 :compiler {:output-to "public/js/app.js"
                                            :output-dir "public/js/release"
                                            :asset-path "js/out"
                                            :optimizations :advanced
                                            :pretty-print false}}}}

  :aliases {"package" ["do" "clean" ["cljsbuild" "once" "release"]]
            "publish" ["do" "package" ["exec" "deploy.clj"]]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.8"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [figwheel-sidecar "0.5.14"]
                                  [org.clojure/tools.nrepl "0.2.13"]]}})
