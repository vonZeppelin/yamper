;; kudos to https://github.com/bliksemman/boot-gh-pages

(use
  'clojure.java.io
  'clojure.java.shell)
(import
  java.io.SequenceInputStream
  java.nio.charset.StandardCharsets
  java.nio.file.FileSystems
  java.util.Collections)

(def deploy-dir (file "./public"))
(def deploy-branch "refs/heads/gh-pages")
(def deploy-ignore ["js/release/**"])

(def git-import-header
  (str
    "commit " deploy-branch "\n"
    "committer deploy.clj <> now\n"
    "data 0\n\n"))

(defn abort []
  (System/exit 1))

(defn str->input-stream [s]
  (input-stream (.getBytes s StandardCharsets/UTF_8)))

(let [deploy-dir (.toPath deploy-dir)]
  (defn deploy-dir-relative-path [f]
    (.relativize deploy-dir (.toPath f))))

(defn git-modified-line [f]
  (format
    "M 644 inline %s\ndata %d\n"
    (deploy-dir-relative-path f)
    (.length f)))

(defn git-import-data []
  (let [fs (FileSystems/getDefault)
        ignore-matchers (map
                          #(.getPathMatcher fs (str "glob:" %))
                          deploy-ignore)
        ignore? (fn [f]
                  (or
                    (.isDirectory f)
                    (let [path (deploy-dir-relative-path f)]
                      (some #(.matches % path) ignore-matchers))))]
    (SequenceInputStream.
      (str->input-stream git-import-header)
      (->> deploy-dir
           file-seq
           (remove ignore?)
           (map (fn [f]
                  (println "* " (str f))
                  f))
           (reduce
             (fn [acc f]
               (-> [acc
                    (str->input-stream (git-modified-line f))
                    (input-stream f)
                    (str->input-stream "\n")]
                   Collections/enumeration
                   SequenceInputStream.))
             (str->input-stream ""))))))

(defn git [& args]
  (let [{:keys [exit err]} (apply sh "git" args)]
    (when-not (zero? exit)
      (println err)
      (abort))))

(defn git-fast-import []
  (println "Adding files to deploy...")
  (git "fast-import" "--date-format=now" "--force" :in (git-import-data)))

(defn git-push []
  (println "Pushing to the repository...")
  (git "push" "--force" "origin" deploy-branch))

(when-not (.isDirectory deploy-dir)
  (println "Deploy directory does not exist. Aborting.")
  (abort))
(when (empty? (.list deploy-dir))
  (println "Deploy directory is empty. Aborting.")
  (abort))

(with-sh-dir "."
  (git-fast-import)
  (git-push))
