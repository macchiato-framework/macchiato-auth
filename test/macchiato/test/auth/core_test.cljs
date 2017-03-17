(ns macchiato.test.auth.core-test
  (:require
    [macchiato.auth :as auth]
    [macchiato.auth.middleware :as mw]
    [macchiato.auth.backends.basic :as basic]
    [macchiato.auth.backends.session :as session]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]
    [macchiato.auth.protocols :as proto]))

(defn auth-backend
  [secret token-name]
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (get request token-name))

    (-authenticate [_ request data]
      (assert data)
      (when (= data secret)
        :valid))))

(defn auth-handler [req res raise]
  (res req))

(deftest wrap-authentication
  (testing "Using auth requests"
    (let [handler  (mw/wrap-authentication
                     auth-handler
                     (auth-backend ::ok ::authdata))
          response (handler {::authdata ::ok} identity identity)]
      (is (= (:identity response) :valid))
      (is (= (::authdata response) ::ok))))
  (testing "Using anon request"
    (let [handler (mw/wrap-authentication auth-handler (auth-backend ::ok ::authdata))
          response (handler {} identity identity)]
      (is (= (:identity response) nil))
      (is (= (::authdata response) nil))))

  (testing "Using wrong request"
    (let [handler (mw/wrap-authentication auth-handler (auth-backend ::ok ::authdata))
          response (handler {::authdata ::fake} identity identity)]
      (is (nil? (:identity response)))
      (is (= (::authdata response) ::fake)))))


(deftest wrap-authentication-with-multiple-backends
  (let [backends [(auth-backend ::ok-1 ::authdata)
                  (auth-backend ::ok-2 ::authdata2)]
        handler (apply mw/wrap-authentication auth-handler backends)]

    (testing "backend #1 succeeds"
      (let [response (handler {::authdata ::ok-1} identity identity)]
        (is (= (:identity response) :valid))
        (is (= (::authdata response) ::ok-1))))

    (testing "backend #2 succeeds"
      (let [response (handler {::authdata2 ::ok-2} identity identity)]
        (is (= (:identity response) :valid))
        (is (= (::authdata2 response) ::ok-2))))

    (testing "no backends succeeds"
      (let [response (handler {::authdata ::fake} identity identity)]
        (is (nil? (:identity response)))
        (is (= (::authdata response) ::fake))))

    (testing "handler called exactly once"
      (let [state (atom 0)
            counter (fn [req res raise]
                      (swap! state inc)
                      (res req))
            handler (apply mw/wrap-authentication counter backends)
            response (handler {::authdata ::fake} identity identity)]
        (is (nil? (:identity response)))
        (is (= (::authdata response) ::fake))
        (is (= @state 1))))

    (testing "with zero backends"
      (let [request {:uri "/"}]
        (is (= ((mw/wrap-authentication auth-handler) request identity identity)
               request))))))


(defn basic-auth-request [uri method]
  {:uri     uri
   :method  method
   :headers {"authorization" "Basic QWxhZGRpbjpPcGVuU2VzYW1l"}})

(defn session-auth-request [uri method session]
  {:uri     uri
   :method  method
   :session session})

#_(deftest basic-backend-test
    (let [handler (mw/wrap-authentication
                    (fn [req res raise]
                      (println "got req" req)
                      (res req))
                    (basic/http-basic-backend
                      {:authfn (fn [_] true)}))]
      (handler (basic-auth-request "/" :post)
               identity
               identity)))

#_(deftest session-backend-test
    (let [handler (mw/wrap-authentication
                    (fn [req res raise]
                      (println "got req" req)
                      (res req))
                    (session/session-backend))]
      (handler (session-auth-request "/" :post
                                     {:foo :bar})
               identity
               identity)))
