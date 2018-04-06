(ns whole-note.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [qbits.alia :as alia]
            [clojure.data.json :as json])
            )


;Db connections


;Mock Collection v1
(def myMap
  {
    :sleeping-beast
    {
      :name "Zodd"
      :godhand "void"
    }


    :sleeping-angel
    {
      :name "ALexiel"
      :godhand "Slan"
    }
  }
  )

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))
(defn get-project
  [request]
  (let [projname (get-in request [:path-params :name])]
     (http/json-response ((keyword projname) myMap))
  )
)

(defn get-projects
  [request]
  (http/json-response myMap))

;Getting the employees from our Cassandra Database
(defn get-employees
  [request]
  (let [cluster (alia/cluster {:contact-points ["localhost"]})
        session (alia/connect cluster :cassandratraining)]
  (prn (alia/execute session "SELECT * FROM emp"))
  (http/json-response (alia/execute session "SELECT * FROM emp"))
  ))

;Inserting record for our Cassandra database
(defn add-employee [request]
  (let [cluster (alia/cluster {:contact-points ["localhost"]})
        session (alia/connect cluster :cassandratraining)
        ;mapFromJson (json/read-json (:json-params request))
        mapFromJson (:json-params request)
        insertstring (str "INSERT INTO  emp(emp_id,emp_city,emp_email,emp_name,emp_phone,emp_salary) VALUES('" (mapFromJson :emp_id) "'" ")")]
    (prn insertstring)
    (prn mapFromJson)
    (ring-resp/created "http://fake-201-url" "fake 201 in the body"))
  )

(defn add-project
  [request]
  (prn (:json-params request))
  (ring-resp/created "http://fake-201-url" "fake 201 in the body"))

(defn home-page
  [request]
  (prn request)
  (ring-resp/response "Hello World!"))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/about" :get (conj common-interceptors `about-page)]
              ["/projects" :get (conj common-interceptors `get-projects)]
              ["/projects/:name" :get (conj common-interceptors `get-project)]
              ["/addprojects" :post (conj common-interceptors `add-project)]
              ["/employees" :get (conj common-interceptors `get-employees)]
              ["/employees" :post (conj common-interceptors `add-employee)]})

;; Map-based routes
;(def routes `{"/" {:interceptors [(body-params/body-params) http/html-body]
;                   :get home-page
;                   "/about" {:get about-page}}})

;; Terse/Vector-based routes
;(def routes
;  `[[["/" {:get home-page}
;      ^:interceptors [(body-params/body-params) http/html-body]
;      ["/about" {:get about-page}]]]])


;; Consumed by whole-note.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ::http/type :jetty
              ;;::http/host "localhost"
              ::http/port (Integer. (or (System/getenv "PORT") 2057))
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})
