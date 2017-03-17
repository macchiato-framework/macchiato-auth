(ns macchiato.auth.backends.token
  (:require
    [cljs.nodejs :as node]
    [macchiato.auth :refer [authenticated?]]
    [macchiato.auth.http :refer [find-header]]
    [macchiato.auth.protocols :as proto]))

(def ^:private jwt (node/require "jsonwebtoken"))

(defn- handle-unauthorized-default
  "A default response constructor for an unauthorized request."
  [request]
  (if (authenticated? request)
    {:status 403 :headers {} :body "Permission denied"}
    {:status 401 :headers {} :body "Unauthorized"}))

(defn- parse-header
  [{:keys [headers]} token-name]
  (some->> (find-header headers "authorization")
           (re-find (re-pattern (str "^" token-name " (.+)$")))
           (second)))

(defn jws-backend
  [{:keys [secret unauthorized-handler options token-name on-error]
    :or   {token-name "Token"}}]
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (parse-header request token-name))

    (-authenticate [_ request data]
      (try
        (.verify jwt data secret)
        (.decode jwt data secret)
        (catch js/Error e
          (when (fn? on-error)
            (on-error request e)
            nil))))))


(defn jwe-backend
  [{:keys [secret unauthorized-handler options token-name on-error]
    :or   {token-name "Token"}}]
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (parse-header request token-name))
    (-authenticate [_ request data]
      (try
        (.verify jwt data secret)
        (catch js/Error e
          (when (fn? on-error)
            (on-error request e)
            nil))))

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if unauthorized-handler
        (unauthorized-handler request metadata)
        (handle-unauthorized-default request)))))


(defn token-backend
  [{:keys [authfn unauthorized-handler token-name] :or {token-name "Token"}}]
  {:pre [(ifn? authfn)]}
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (parse-header request token-name))
    (-authenticate [_ request token]
      (authfn request token))

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if unauthorized-handler
        (unauthorized-handler request metadata)
        (handle-unauthorized-default request)))))
