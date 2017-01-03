(ns macchiato.auth.backends.basic
  (:require
    [macchiato.auth :refer [authenticated?]]
    [clojure.string :as s]
    [goog.crypt.base64 :as b64]
    [macchiato.auth.protocols :as proto]
    [macchiato.auth.http :refer [find-header]]))

(defn- parse-header
  "Given a request, try to extract and parse the basic header."
  [{:keys [headers]}]
  (let [pattern (re-pattern "^Basic (.+)$")
        decoded (some->> (find-header headers "authorization")
                         (re-find pattern)
                         (second)
                         (b64/decodeString))]
    (when-let [[username password] (s/split decoded #":" 2)]
      {:username username
       :password password})))

(defn http-basic-backend
  [{:keys [realm authfn unauthorized-handler] :or {realm "Macchiato Auth"}}]
  {:pre [(ifn? authfn)]}
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (parse-header request))
    (-authenticate [_ request data]
      (authfn request data))

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if unauthorized-handler
        (unauthorized-handler request (assoc metadata :realm realm))
        (if (authenticated? request)
          {:status  403
           :headers {}
           :body    "Permission denied"}
          {:status  401
           :headers {"WWW-Authenticate" (str "Basic realm=\"" realm "\"")}
           :body    "Unauthorized"})))))
