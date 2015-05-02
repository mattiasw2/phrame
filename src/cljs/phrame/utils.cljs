(ns ^:figwheel-always phrame.utils)

(defn to-json-string [o]
  (.stringify js/JSON (clj->js o)))
