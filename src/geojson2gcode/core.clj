(ns geojson2gcode.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [geojson2gcode.geometry :as geom]
            [geojson2gcode.translate :as trans]
            [geojson2gcode.gcode :as gcode]))

(defn ratio-based-min-max
  [{:keys [x-min x-max y-min y-max] :as config} {:keys [lat-long-ratio long-lat-ratio] :as bounds}]
  (cond (and (nil? x-min) (nil? x-max)) (merge config {:x-min (* y-min lat-long-ratio) :x-max (* y-max lat-long-ratio)})
        (and (nil? y-min) (nil? y-max)) (merge config {:y-min (* x-min long-lat-ratio) :y-max (* x-max long-lat-ratio)})
        (and x-min x-max y-min y-max) config
        :else (throw (ex-info "Not sure how to keep aspect ratio based on keys in config map" {:config config :bounds bounds}))))

(defn encode
  [config geo]
  (as-> (geom/bounds geo) *
    (trans/make-coords-scaler * (ratio-based-min-max config *))
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
             :x-min 0 :x-max 150}
            *)
    (println *)))
