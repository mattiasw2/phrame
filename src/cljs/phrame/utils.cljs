(ns ^:figwheel-always phrame.utils)

(defn to-json-string [o]
  (.stringify js/JSON (clj->js o)))

(defn say [& args]
  (js/console.log (apply str args)))

