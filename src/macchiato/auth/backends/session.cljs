(ns macchiato.auth.backends.session
  (:require
    [macchiato.auth :refer [authenticated?]]
    [macchiato.auth.protocols :as proto]))

(defn session-backend
  [& [{:keys [unauthorized-handler]}]]
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (:identity (:session request)))
    (-authenticate [_ request data]
      data)

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if unauthorized-handler
        (unauthorized-handler request metadata)
        (if (authenticated? request)
          {:status  403
           :headers {}
           :body    "Permission denied"}
          {:status  401
           :headers {}
           :body    "Unauthorized"})))))
