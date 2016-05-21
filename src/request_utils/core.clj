(ns ^{:added "0.1.0" :author "Marcos Lamúria"}
  request-utils.core
  (:require [cheshire.core :as json]
            [clojure.string :as clj-str]
            [ring.util.codec :as codec]
            [result.core :as result]
            [clojure.core.async :refer [chan <!! >!! close! go <! timeout]]
            [manifold.deferred :as d]
            [aleph.http :as http]))

(defn url-encode
  [query-params]
  (codec/url-encode query-params "UTF-8"))

(defn query-string-param-builder
  [query-string-key data]
  (when data
    (if (coll? data)
      (str query-string-key "=" (clj-str/join "," data))
      (str query-string-key "=" data))))

(defn clean-record
  [record]
  (apply dissoc record (for [[k v] record :when (nil? v)] k)))

(defn build-query-string
  [query-params]
  (clj-str/join "&"
                (for [[k v] query-params] (query-string-param-builder (name k)
                                                                      (url-encode v)))))

(defn build-url
  [host path query-params]
  (if query-params
    (str host path "?" (build-query-string (clean-record query-params)))
    (str host path)))

(defn add-headers
  [headers http-ops]
  (assoc http-ops :headers headers))

(defn parse-body
  [body]
  (cond-> body
    (or (map? body) (seq? body)) (json/generate-string body)))

(defn add-body
  [http-ops data]
  (assoc http-ops :body (parse-body data)))

(defn prepare-data
  "Prepares all the data to do the request. This function receives two parameters
  method and data.
  The method can be http reques methods:
    * :get
    * :put
    * :post
    * :delete
  As you can see in the following example the data is a map with all data
  related with the request:
    {:host 'http://api.fake.com'
     :path '/fake/path'
     :query-params {:a '1' :b '2'}
     :headers {'content-type' 'application/json'
               'Authorization' 'ewqjwehqkwh'}
     :body {:name 'fake name' :age 34}}

  Only the host is required. It's mandatory to have the protocol and
  the host together.
  Valid :host value: http://api.fake.com
  Invalid :host value: api.fake.com"
  [data method]
  (let [http-opts (-> (add-headers (:headers data) (:http-opts data))
                      (assoc :throw-exceptions? false)
                      (add-body (:body data)))]
    (assoc data :host (:host data)
                :requests 0
                :retries (- (or (:retries data) 3) 1)
                :url (build-url (:host data) (:path data) (:query-params data))
                :http-opts http-opts
                :request-method method
                :method-fn (cond
                             (= :post method) http/post
                             (= :put method) http/put
                             :else http/get))))

(defn- retry?
  "Verifies that the given error response is the final one, or that
  we should try it again."
  [data response]
  (and (instance? java.util.concurrent.TimeoutException response)
       (not= 0 (int (:retries data)))))

(def final-response? (comp not retry?))

(defn- get-success
  "Checks if the response was successful, based on the status code"
  [response]
  (let [first-char (-> response :status str first)]
    (or (= \2 first-char)
        (= \3 first-char) )))

(defn- prepare-response
  "Handles post-response"
  [data response]
  (try
    (merge {:success (get-success response)
            :status (:status response)
            :requests (inc (:requests data))}
           (json/parse-string (slurp (:body response)) true))
    (catch Exception ex
      {:exception ex})))

(defn- prepare-error
  "Handles post-response errors"
  [data response]
  (try
    (cond
      (instance? java.util.concurrent.TimeoutException response)
      {:status 408
       :error (str "Error getting " (:url data))
       :request-time (-> data :http-opts :request-timeout)
       :requests (inc (:requests data))
       :data {:message "Timed out"}}
      (instance? clojure.lang.ExceptionInfo response)
      (merge {:status (.getMessage response)
              :error (str "Error getting " (:url data))
              :requests (inc (:requests data))
              :request-time (:request-time (.getData response))}
             (json/parse-string (slurp (:body (.getData response))) true))
      (instance? Throwable response)
      {:error (str "Error getting " (:url data))
       :caused-by response}
      :else
      (-> response
          (assoc :error (str "Error getting " (:url data)))
          (assoc :requests (inc (:requests data)))
          (assoc :status (-> response :data :cause))
          (assoc :body-data (slurp (-> response :data :body)))))
    (catch Exception ex
      {:error (str "Error getting " (:url data))
       :exception (.getMessage ex)})))

(defn fetch-response
  "Fetches the response for a given URL"
  [data]
  (try
    (let [result-ch (or (:result-ch data) (chan 1))
          async-stream ((:method-fn data) (:url data) (:http-opts data))]
      (d/on-realized async-stream
                     (fn [x]
                       (if x
                         (>!! result-ch (prepare-response data x))
                         (>!! result-ch (result/failure (prepare-response data x))))
                       (close! result-ch))
                     (fn [x]
                       (if (final-response? data x)
                         (do
                           (>!! result-ch (prepare-error data x))
                           (close! result-ch))
                         (go
                           (<! (timeout (* 300 (+ 1 (int (:requests data))))))
                           (fetch-response (-> data
                                               (assoc :result-ch result-ch)
                                               (update :retries dec)
                                               (update :requests inc)))))))
      result-ch)
    (catch Exception ex
      (go (result/failure ex)))))

(defn http-get
  "Makes a GET request to the given API"
  [data]
  (fetch-response (prepare-data data :get)))

(defn http-post
  "Makes a POST request to the given API"
  [data]
  (fetch-response (prepare-data data :post)))

(defn http-put
  "Makes a PUT request to the given API"
  [data]
  (fetch-response (prepare-data data :put)))
