(ns geojson2gcode.geometry)

(defn compute-ratios
  [{:keys [long-min long-max lat-min lat-max] :as bounds}]
  (merge bounds
         {:long-lat-ratio (/ (- long-max long-min) (- lat-max lat-min))
          :lat-long-ratio (/ (- lat-max lat-min) (- long-max long-min))}))

(defn bounds
  [{:keys [coordinates]}]
  (let [coordinates (flatten coordinates)]
    (-> (reduce
         (fn [bounds [long lat]]
           (-> bounds
               (update :long-min #(min long %))
               (update :long-max #(max long %))
               (update :lat-min #(min lat %))
               (update :lat-max #(max lat %))))
         {:long-min (first coordinates)
          :long-max (first coordinates)
          :lat-min (second coordinates)
          :lat-max (second coordinates)}
         (partition 2 coordinates))
        compute-ratios)))
