(ns macchiato.auth.middleware
  (:require
    [macchiato.auth.protocols :as proto]))

(defn authentication-request
  "Updates request with authentication. If multiple `backends` are given
  each of them gets a chance to authenticate the request."
  [request & backends]
  (let [authdata (loop [[backend & backends] backends]
                   (when backend
                     (let [request (assoc request :auth-backend backend)]
                       (or (some->> request
                                    (proto/-parse backend)
                                    (proto/-authenticate backend request))
                           (recur backends)))))]
    (assoc request :identity authdata)))

(defn wrap-authentication
  "Ring middleware that enables authentication for your Macchiato
  handler. When multiple `backends` are given each of them gets a
  chance to authenticate the request."
  [handler & backends]
  (if (empty? backends)
    handler
    (fn [request respond raise]
      (handler
        (apply authentication-request request backends)
        respond
        raise))))


(defn- fn->authorization-backend
  "Given a function that receives two parameters
  return an anonymous object that implements
  IAuthorization protocol."
  [callable]
  {:pre [(fn? callable)]}
  (reify
    proto/IAuthorization
    (-handle-unauthorized [_ request errordata]
      (callable request errordata))))

(defn authorization-error
  "Handles authorization errors.
  The `backend` parameter should be a plain function
  that accepts two parameters: request and errordata hashmap,
  or an instance that satisfies IAuthorization protocol."
  #_[request e backend raise]
  [e backend request respond raise]
  (let [backend (cond
                  (fn? backend)
                  (fn->authorization-backend backend)

                  (satisfies? proto/IAuthorization backend)
                  backend)]
    (if (instance? ExceptionInfo e)
      (let [data (ex-data e)]
        (if (= (:macchiato.auth/type data) :macchiato.auth/unauthorized)
          (->> (:macchiato.auth/payload data)
               (proto/-handle-unauthorized backend request)
               (respond))
          (raise e)))
      (if (satisfies? proto/IAuthorizationError e)
        (->> (proto/-get-error-data e)
             (proto/-handle-unauthorized backend request))
        (raise e)))))

(defn wrap-authorization
  "Macchiato middleware that enables authorization
  workflow for your Macchiato handler.
  The `backend` parameter should be a plain function
  that accepts two parameters: request and errordata
  hashmap, or an instance that satisfies IAuthorization
  protocol."
  [handler backend]
  (fn [request respond raise]
    (handler
      request
      respond
      (fn [e]
        (authorization-error e backend request respond raise))
      #_(authorization-error request % backend raise))))
