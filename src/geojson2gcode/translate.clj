(ns geojson2gcode.translate)

(defn offset
  [bound-min bound-max outer-min outer-max]
  (/ (- (- outer-max outer-min)
        (- bound-max bound-min))
     2))

(defn make-scaler
  [[in-min in-max] [out-min out-max] offset]
  (fn [val]
    (+ (/ (* (- val in-min)
             (- out-max out-min))
          (- in-max in-min))
       out-min
       offset)))

(defn determine-real-bounds
  [{:keys [long-min long-max lat-min lat-max long-lat-ratio lat-long-ratio] :as bounds} {:keys [x-min x-max y-min y-max x-offset y-offset] :as config}]
  (if (> (- long-max long-min) (- lat-max lat-min))
    (let [real-y-min (* x-min lat-long-ratio)
          real-y-max (* x-max lat-long-ratio)]
      (merge config {:y-offset (offset real-y-min real-y-max y-min y-max)
                     :y-min real-y-min
                     :y-max real-y-max}))
    (let [real-x-min (* y-min long-lat-ratio)
          real-x-max (* y-max long-lat-ratio)]
      (merge config {:x-offset (offset real-x-min real-x-max x-min x-max)
                     :x-min real-x-min
                     :x-max real-x-max}))))

(defn make-coords-scaler
  [{:keys [long-min long-max lat-min lat-max] :as bounds} {:keys [x-min x-max y-min y-max x-offset y-offset] :as config}]
  ;; handle mirroring by swapping min and max output for the mirrored axis
  (let [scale-long (make-scaler [long-min long-max] [x-min x-max] (or x-offset 0))
        scale-lat (make-scaler [lat-min lat-max] [y-min y-max] (or y-offset 0))]
    (fn [[long lat]]
      [(scale-long long)
       (scale-lat lat)])))

(defn handle-mirroring
  [{:keys [x-mirror y-mirror] :as config}]
  (cond x-mirror (let [{:keys [x-min x-max]} config]
                   (merge config {:x-min x-max :x-max x-min}))
        y-mirror (let [{:keys [y-min y-max]} config]
                   (merge config {:y-min y-max :y-max y-min}))
        :else config))

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

