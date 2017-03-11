(ns app.core
  (:require [clojure.string :as string]
            [ring.adapter.jetty :as ring]))




(declare -main handler)

(defn -main []
  (ring/run-jetty handler {:port 8080}))

(defn handler [{:keys [uri]}]  ; ps ven is a swede
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (->> (string/split uri #"/")
              (filter (complement empty?))
              (map #(Integer/parseInt %))
              (reduce +)
              str)})
