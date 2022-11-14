(ns geojson2gcode.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [geojson2gcode.geometry :as geom]
            [geojson2gcode.translate :as trans]
            [geojson2gcode.gcode :as gcode]))

(defn encode
  [config geo]
  (as-> (geom/bounds geo) *
    (trans/make-coords-scaler * (trans/handle-mirroring (trans/determine-real-bounds * config)))
    (trans/scale * geo)
    (:coordinates *)
    ;; nb: this fn doesn't get the config that was upsted with aspect ratio stuff
    (gcode/encode config *)))

(defn -main [& args]
  ;; start simple, take from stdin and put to stdout
  ;; worry about args and shit later
  (as-> (slurp *in*) *
    (json/read-str * :key-fn keyword)
    (encode {:laser-speed 1500 ; this config should be an edn file and be specified via arg
             :laser-intensity 1000
             :travel-speed 3000
             :y-min 0 :y-max 250}
            *)
    (println *)))
