(ns macchiato.auth.protocols)

(defprotocol IAuthentication
  "Protocol that defines unified workflow steps for
  all authentication backends."
  (-parse [_ request]
          "Parse token from the request. If it returns `nil`
          the `authenticate` phase will be skipped and the
          handler will be called directly.")
  (-authenticate [_ request data]
                 "Given a request and parsed data (from previous step),
                 try to authenticate this data.
                 If this method returns not nil value, the request
                 will be considered authenticated and the value will
                 be attached to request under `:identity` attribute."))

(defprotocol IAuthorization
  "Protocol that defines unified workflow steps for
  authorization exceptions."
  (-handle-unauthorized [_ request metadata]
                        "This function is executed when a `NotAuthorizedException`
                        exception is intercepted by authorization wrapper.
                        It should return a valid ring response."))

(defprotocol IAuthorizationError
  "Abstraction that allows the user to extend the exception
  based authorization system with own types."
  (-get-error-data [_] "Ger error information."))
