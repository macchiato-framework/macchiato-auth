(ns macchiato.auth)

(defn authenticated?
  "Return `true` if the `request` is an
  authenticated request.
  This function checks the `:identity` key
  in the request."
  [request]
  (boolean (:identity request)))

(defn unauthorized
  "return unauthorized error."
  ([] (unauthorized {}))
  ([errordata]
   (ex-info "Unauthorized." {::type ::unauthorized
                             ::payload errordata})))
