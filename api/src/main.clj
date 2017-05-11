(ns main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]))
            
(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})
  
(def ok       (partial response 200))
(def created  (partial response 201))
(def accepted (partial response 202))

(def echo
  {:name :echo
  :enter (fn [context]
           (let [request (:request context)
                 response (ok request)]
             (assoc context :response response)))})

(defonce database (atom {}))

(def database-interceptor
  {:name :database-interceptor
  :enter (fn [context]
           (update context :request assoc :database @database))
   :leave (fn [context]
           (if-let [[op & args] (:tx-data context)]
             (do
               (apply swap! database op args)
               (assoc-in context [:request :database] @database))
             context))})

 (def routes
  (route/expand-routes
   #{["/todo"                :post   echo :route-name :list-create]
     ["/todo"                :get    echo :route-name :list-query-form]
     ["/todo/:list-id"       :get    echo :route-name :list-view]
     ["/todo/:list-id"       :post   echo :route-name :list-item-create]
     ["/todo/:list-id/:item" :get    echo :route-name :list-item-view]
     ["/todo/:list-id/:item" :put    echo :route-name :list-item-update]
     ["/todo/:list-id/:item" :delete echo :route-name :list-item-delete]}))
     
(def service-options
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   8890})
   
(defn start []
  (http/start (http/create-server service-options)))
  
(defonce server (atom nil))

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                       (assoc service-options ::http/join? false)))))
                       
(defn stop-dev []
  (http/stop @server))
  
(defn restart []
  (stop-dev)
  (start-dev))