(ns hello                                        
  (:require [io.pedestal.http :as http]          
            [io.pedestal.http.route :as route]))

(defn ok
  ([body]
    {:status 200 :body body}))

(defn bad-request []
    {:status 400 :body "Bad request"})

(defn not-found []
  {:status 404 :body "Not found"})

(defn greeting-for [name]
  (if-not (empty? name)
    (str "Hello, " name)))

(def unmentionables #{"YHWH" "Voldemort" "Mxyzptlk" "Rumplestiltskin" "曹操"})

(defn respond-hello [request]
  (let [name (get-in request [:query-params :name])
        greeting (greeting-for name)
        resp (cond
               (unmentionables name) (not-found)
               greeting              (ok greeting)
               :else                 (bad-request))]
    resp))

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]}))

(defn create-server []
  (http/create-server
    {::http/routes routes
     ::http/type   :jetty
     ::http/port   8890}))

(defn start []
  (http/start (create-server)))
