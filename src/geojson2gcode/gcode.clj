(ns geojson2gcode.gcode)

(defn m4
  [{:keys [s]}]
  (format "m4 s%d" s))

(defn g0
  [{:keys [x y f s]}]
  (str "g0"
       (when (number? x) (format " x%.4f" x))
       (when (number? y) (format " y%.4f" y))
       (when (number? f) (format " f%d" f))
       (when (number? s) (format " s%d" s))))

(defn g1
  [{:keys [x y f s]}]
  (str "g1"
       (when (number? x) (format " x%.4f" x))
       (when (number? y) (format " y%.4f" y))
       (when (number? f) (format " f%d" f))
       (when (number? s) (format " s%d" s))))

(defn prepare-coords
  [paths]
  (if (number? (first paths))
    (let [[x y] paths]
      {:x x :y y})
    (map prepare-coords paths)))

(defn construct-gcode
  [{:keys [laser-speed laser-intensity travel-speed] :as config} coords]
  (if (map? (first coords))
    (reduce
     (fn [gcode coord]
       (str gcode "\n"
            (g1 (merge coord {:f laser-speed :s laser-intensity}))))
     (g0 (merge (first coords) {:f travel-speed}))
     (rest coords))
    (map (partial construct-gcode config) coords)))

(defn encode
  [config paths]
  (let [gcode (->> (prepare-coords paths)
                   (construct-gcode config))]
    (str ; append \n to end of str (posix file requirement)
     (reduce
      #(str %1 "\n" %2)
      (str "g90\n"                     ; absolute coordinates
           (m4 {:s (:laser-intensity config)}))
      gcode)
     "\n")))
