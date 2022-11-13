(ns geojson2gcode.translate)

(defn make-scaler
  [[in-min in-max] [out-min out-max]]
  (fn [val]
    (+ (/ (* (- val in-min)
             (- out-max out-min))
          (- in-max in-min))
       out-min)))

(defn make-coords-scaler
  [{:keys [long-min long-max lat-min lat-max] :as bounds} {:keys [x-min x-max y-min y-max] :as gcode-bounds}]
  (let [scale-long (make-scaler [long-min long-max] [x-min x-max])
        scale-lat (make-scaler [lat-min lat-max] [y-min y-max])]
    (fn [[long lat]]
      [(scale-long long)
       (scale-lat lat)])))

(defmulti scale
  (fn [scaler geojson]
    (:type geojson)))

(defmethod scale "Point"
  [scaler geojson]
  (update geojson :coordinates scaler))

(defmethod scale "LineString" ; array of Point
  [scaler {:keys [coordinates] :as geojson}]
  (assoc geojson :coordinates (mapv
                               (fn [coords]
                                 (:coordinates
                                  (scale scaler
                                         {:type "Point"
                                          :coordinates coords})))
                               coordinates)))

(defmethod scale "MultiLineString" ; array of LineString
  [scaler {:keys [coordinates] :as geojson}]
  (assoc geojson :coordinates (mapv
                               (fn [coords]
                                 (:coordinates
                                  (scale scaler
                                         {:type "LineString"
                                          :coordinates coords})))
                               coordinates)))

(defmethod scale "Polygon"
  [scaler geojson]
  (assoc geojson :coordinates (:coordinates (scale scaler (assoc geojson :type "LineString")))))
