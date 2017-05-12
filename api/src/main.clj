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

(def handle-data
  {:name :handle-data
   :enter (fn [context]
            (update context :request assoc :database @database))
   :leave (fn [context]
            (if-let [[op & args] (:tx-data context)]
              (do
                (apply swap! database op args)
                (assoc-in context [:request :database] @database))
              context))})
              
(defn make-list [namae]
  {:name namae
   :items {}})
   
(defn make-list-item [namae]
  {:name namae
   :done? false})
   
(def list-create
  {:name :list-create
   :enter (fn [context]
            (let [namae    (get-in context [:request :query-params :name] "Unnamed List")
                  new-list (make-list namae)
                  db-id    (str (gensym "1"))
                  url      (route/url-for :list-view :params {:list-id db-id})]
              (assoc context
                     :response (created new-list "Location" url)
                     :tx-data [assoc db-id new-list])))})

(def find-by-id get)

(def list-view
  {:name :list-view
   :enter (fn [context]
            (if-let [db-id (get-in context [:request :params :list-id])]
              (let [db (get-in context [:request :database])]
                (if-let [the-list (find-by-id db db-id)]
                  (assoc context :result the-list)
                  context)
                context))}))

(def routes
  (route/expand-routes
   #{["/todo"                :post   [handle-data list-create]]
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
  
(defn test-request [verb url]
  (io.pedestal.test/response-for (::http/service-fn @server) verb url))
  