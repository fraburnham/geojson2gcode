(ns geojson2gcode.core
  (:gen-class)
  (:import org.apache.commons.cli.Options
           org.apache.commons.cli.DefaultParser)
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
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
  (let [options (-> (Options.)
                    (.addOption "c" "config-file" true "Config file for laser config and final size")
                    (.addOption "i" "input-file" true "Input geojson file. Consumes from *in* if not set.")
                    (.addOption "o" "output-file" true "Output gcode file. Dumps to *out* if not set."))
        cli-args (.parse (DefaultParser.) options (into-array java.lang.String args))
        config (if (.hasOption cli-args "c")
                 (edn/read-string (slurp (.getOptionValue cli-args "c")))
                 {:laser-speed 1500
                  :laser-intensity 1000
                  :travel-speed 3000
                  :x-min 0 :x-max 100
                  :y-min 0 :y-max 100})
        input (if (.hasOption cli-args "i")
                (.getOptionValue cli-args "i")
                *in*)
        output (if (.hasOption cli-args "o")
                 (.getOptionValue cli-args "o")
                 *out*)]
    (as-> (slurp input) *
      (json/read-str * :key-fn keyword)
      (encode config *)
      (spit output *))))
