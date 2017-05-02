(ns chain-reaction.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(def M 8)

(def N 10)

(defn new-board [m n]
    (vec (repeat m (vec (repeat n {:player "B" :number 0})))))

(def player-color {"X" "Red" "Y" "Green" "B" "Grey"})
(def player-to-move (atom "X"))
(def player-data (atom {"X" {:number-of-moves 0, :number-of-boxes 0, :sum-of-boxes 0},
                        "Y" {:number-of-moves 0, :number-of-boxes 0, :sum-of-boxes 0}}))
;; define your app data so that it doesn't get over-written on reload

(def app-state (atom {:text "Welcome to Chain Reaction Game"
                      :board (new-board M N)
                      :game-status :in-progress}))

(defn update-player-info []
    (let [flat-board (flatten (@app-state :board))
          nob-x (count (filter #(= "X" (% :player)) flat-board))
          nob-y (count (filter #(= "Y" (% :player)) flat-board))
          sob-x (reduce + (map #(% :number) (filter #(= "X" (% :player)) flat-board)))
          sob-y (reduce + (map #(% :number) (filter #(= "Y" (% :player)) flat-board)))])
    (swap! player-data update-in [@player-to-move :number-of-moves] inc)
    (swap! player-data assoc-in ["X" :number-of-boxes] nob-x)
    (swap! player-data assoc-in ["Y" :number-of-boxes] nob-y)
    (swap! player-data assoc-in ["X" :sum-of-boxes] sob-x)
    (swap! player-data assoc-in ["Y" :sum-of-boxes] sob-y))

(defn max-value [i j]
    (- 3 (count (filter zero? [i j (- (- M 1) i) (- (- N 1) j)]))))

(defn valid-index [[i j]]
    (and (>= i 0) (>= j 0) (< i M) (< j N)))

(defn neighbours [i j]
    (filter valid-index [[(- i 1) j] [(+ i 1) j] [i (- j 1)] [i (+ j 1)]]))

(defn neighbours-update [i j]
  (loop [neighbour (neighbours i j)]
    (if (empty? neighbour)
        nil
        (do (swap! app-state assoc-in [:board (first (first neighbour)) (second (first neighbour)) :player] @player-to-move)
            (swap! app-state update-in [:board (first (first neighbour)) (second (first neighbour)) :number] inc)
            (recur (rest neighbour))))))

(defn split-update [[i j]]
  (neighbours-update i j)
   (if (= (+ 1 (max-value i j)) (get-in @app-state [:board i j :number]))
       (do (swap! app-state assoc-in [:board i j :player] "B")
           (swap! app-state assoc-in [:board i j :number] 0))
       (swap! app-state update-in [:board i j :number] - (+ 1 (max-value i j)))))

(defn overall-split-update [split-list]
    (loop [li split-list]
        (if (empty? li)
            nil
            (do (split-update (first li))
                (recur (rest li))))))

(defn ready-to-split
    []
    (for [i (range M)
          j (range N)
            :when (> (get-in @app-state [:board i j :number]) (max-value i j))]
        [i j]))

(defn update-app-state [i j]
  (if (contains? #{"B" @player-to-move} (get-in @app-state [:board i j :player]))
      (do (swap! app-state assoc-in [:board i j :player] @player-to-move)
          (swap! app-state update-in [:board i j :number] inc)
          (while (> (count (ready-to-split)) 0) (overall-split-update (ready-to-split)))
          (reset! player-to-move ({"X" "Y", "Y" "X"} @player-to-move)))))

(defn rectangle [i j]
    [:svg 
      {:on-click (fn [e]
                    (update-app-state j i))}
    [:rect 
      {:width 0.88
      :height 0.88
      :fill (player-color (get-in @app-state [:board j i :player]))
      :x (+ 0.05 (* 0.9 i))
      :y (+ 0.05 (* 0.9 j))}]
    [:text {:x (+ 0.25 (* 0.9 i))
            :y (+ 0.70 (* 0.9 j))
            :font-size 0.7} (let [text (get-in @app-state [:board j i :number])]
                                 (if (not= 0 text) (str text)))]])


(defn chain-reaction []
  [:div
   [:h1 (:text @app-state)]
   [:h4
   (case (get-in @app-state [:game-status])
            :in-progress "Game in progress ")]
   [:svg
   {:view-box "0 0 10 12"
   :width 500
   :height 500}
   (for [i (range M)
         j (range N)]
      [rectangle j i])]])

(reagent/render-component [chain-reaction]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
