(ns macchiato.test.auth.core-test
  (:require
    [macchiato.auth :as auth]
    [macchiato.auth.middleware :as mw]
    [macchiato.auth.accessrules :as ar]
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
        :valid))

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if (auth/authenticated? request)
        ::permission-denied
        ::unauthorized))))

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

(defn request [uri method]
  {:uri uri
   :method method})

(defn basic-auth-request [uri method]
  {:uri     uri
   :method  method
   :headers {"authorization" "Basic QWxhZGRpbjpPcGVuU2VzYW1l"}})

(defn session-auth-request [uri method session]
  {:uri     uri
   :method  method
   :session session})

(deftest basic-backend-test
  (let [handler (mw/wrap-authentication
                 (fn [req res raise]
                   (println "got req" req)
                   (res req))
                 (basic/http-basic-backend
                  {:authfn (fn [_] true)}))]
    (testing "With authorization header"
      (let [response (handler (basic-auth-request "/" :post)
                              identity
                              identity)]
        (is (= true (:identity response)))))
    (testing "Without authorization header"
      (let [response (handler (request "/" :post)
                              identity
                              identity)]
        (is (= nil (:identity response)))))))

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

(deftest access-rules
  (testing "Access rules"
    (let [admin-access (fn [req] (true? (::admin req)))
          rules [{:pattern #"^/public/.*"
                  :handler (constantly true)}
                 {:pattern #"^/secured/.*"
                  :handler auth/authenticated?}
                 {:pattern #"^/admin/.*"
                  :handler admin-access}]

          backend (auth-backend ::ok ::authdata)
          reject-policy-handler
          (-> auth-handler
              (ar/wrap-access-rules {:rules rules :policy :reject})
              (mw/wrap-authorization backend)
              (mw/wrap-authentication backend))
          allow-policy-handler
          (-> auth-handler
              (ar/wrap-access-rules {:rules rules :policy :allow})
              (mw/wrap-authorization backend)
              (mw/wrap-authentication backend))
          public
          (reject-policy-handler {:uri            "/public/files"
                                  :request-method :get}
                                 identity identity)
          unauthorized
          (reject-policy-handler {:uri            "/secured/files"
                                  :request-method :get}
                                 identity identity)
          permission-denied
          (reject-policy-handler {:uri            "/admin/files"
                                  :request-method :get
                                  ::authdata      ::ok}
                                 identity identity)
          permission-granted
          (reject-policy-handler {:uri            "/admin/files"
                                  :request-method :get
                                  ::admin         true}
                                 identity identity)
          rejected-because-of-policy
          (reject-policy-handler {:uri            "/path/that/does/not/match/to/anything"
                                  :request-method :get}
                                 identity identity)
          allowed-because-of-policy
          (allow-policy-handler {:uri            "/path/that/does/not/match/to/anything"
                                 :request-method :post}
                                identity identity)
          ]
      (is (= "/public/files" (:uri public)))
      (is (= ::unauthorized unauthorized))
      (is (= ::permission-denied permission-denied))
      (is (= "/admin/files" (:uri permission-granted)))
      (is (= ::unauthorized rejected-because-of-policy))
      (is (and (= "/path/that/does/not/match/to/anything" (:uri allowed-because-of-policy))
               (= :post (:request-method allowed-because-of-policy))))
      )))