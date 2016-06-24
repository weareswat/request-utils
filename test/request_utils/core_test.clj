(ns request-utils.core-test
  (:require [clojure.test :refer :all]
            [result.core :as result]
            [clojure.core.async :refer [<!!]]
            [request-utils.core :as core]))

(deftest add-headers-test
  (let [headers {"Content-Type" "application/json"
                 "Authorization" "token"}
        result (core/add-headers headers {})]
    (is (= headers (:headers result)))))

(deftest add-body-test
  (let [data {:body {:payment {:client {:name "John Santos"
                                        :email "johnsantos@mail.com"
                                        :address {:country "pt"
                                                  :address "Av. Fontes Pereira de Melo"
                                                  :city "Lisboa"}}
                               :amount 229
                               :currency "EUR"
                               :items [{:ref 123
                                        :name "InvoiceXpress plan"
                                        :descr "New subscription to invoicexpress"
                                        :qt 1}]
                               :ext_invoiceid "C12423324"}}}
        result (core/add-body data {})]
    (is (:body result))))

(deftest query-string-builder
  (testing "with list"
    (is (= "key=1,2" (core/query-string-param-builder "key" [1 2]))))
  (testing "with value"
    (is (= "key=1" (core/query-string-param-builder "key" 1))))
  (testing "with nil"
    (is (nil? (core/query-string-param-builder "key" nil)))))

(deftest build-query-string-test
  (testing "simple case"
    (is (= "key1=1" (core/build-query-string {:key1 1}))))
  (testing "space case"
    (is (= "key1=1&key2=1%202"
           (core/build-query-string {:key1 1
                                     :key2 "1 2"})))))

(deftest build-url-test
  (testing "simple case"
    (is (= "http://host.com/path?key1=1&key2=1%202"
           (core/build-url "http://host.com"
                           "/path"
                           {:key1 1
                            :key2 "1 2"}))))
  (testing "nil properties"
    (is (= "http://host.com/path?key1=1"
           (core/build-url "http://host.com"
                           "/path"
                           {:key1 1
                            :key2 nil})))))

(deftest prepare-data-test
  (let [data {:host "http://www.fakepath.com"
              :path "/"
              :headers {"content-type" "application/json"
                        "Authorization" "ewqjwehqkwh"}
              :body {:payment {:client {:name "John Santos"
                                 :email "johnsantos@mail.com"
                                 :address {:country "pt"
                                           :address "Av. Fontes Pereira de Melo"
                                           :city "Lisboa"}}
                        :amount 229
                        :currency "EUR"
                        :items [{:ref 123
                                 :name "InvoiceXpress plan"
                                 :descr "New subscription to invoicexpress"
                                 :qt 1}]
                        :ext_invoiceid "C12423324"}}
               :query-params {:bubu 1
                              :xpto "xpto"}}
        method :post
        result (core/prepare-data data method)]

    (testing "host"
      (is (= (:host data)
             (:host result))))

    (testing "retries"
      (is (= 2
             (:retries result))))

    (testing "url"
      (is (= (str (:host data) (:path data) "?bubu=1&xpto=xpto")
             (:url result))))

    (testing "request-method"
      (is (= method
             (:request-method result))))

    (testing "http-ops"
      (is (not (nil? (:http-opts result))))

      (testing "headers"
        (is (= (:headers data)
               (get-in result [:http-opts :headers]))))

      (testing "body"
        (is (= (core/parse-body (:body data))
               (get-in result [:http-opts :body])))))

    (testing "method-fn"
      (is (not (nil? (:method-fn result)))))))

(deftest http-get
  (let [result (<!! (core/http-get {:host "http://app.clanhr.com/directory-api/"}))]
    (is (= 200
           (:status result)))))

(deftest http-get-non-json-response
  (let [result (<!! (core/http-get {:host "http://www.google.com"
                                    :plain-body? true}))]
    (is (= 200 (:status result)))))
