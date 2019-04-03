(defproject yamper "0.1.0"
  :description "Yet Another Music PlayER"
  :url "https://github.com/vonZeppelin/yamper"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.490"]
                 ;; ui libs
                 [com.hypaer/ant-man "1.7.4"]
                 [reagent "0.8.1"]
                 ;; misc libs
                 [alandipert/storage-atom "2.0.1"]
                 [com.arohner/uri "0.1.2"]
                 [clj-commons/secretary "1.2.4"]
                 [cljs-http "0.1.46"]
                 [org.clojure/core.match "0.3.0"]]

  :resource-paths ["public"]
  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :figwheel {:http-server-root "."
             :nrepl-port 7002
             :nrepl-middleware ["cider.piggieback/wrap-cljs-repl"]
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

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.4.0"]
                                  [figwheel-sidecar "0.5.18"]
                                  [nrepl "0.6.0"]]
                   :plugins [[lein-cljsbuild "1.1.7"]
                             [lein-exec "0.3.7"]
                             [lein-figwheel "0.5.18"]]}})
