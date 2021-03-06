(ns chain-reaction.core
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string :as str]
            [chain-reaction.rendercircle :as render]))

(enable-console-print!)

(def M (atom 0))

(def N (atom 0))

(def flag (atom false))

(defn new-board [m n]
    (vec (repeat m (vec (repeat n {:player "B" :number 0})))))

(def player-color {"X" "Red" "Y" "Green" "B" "Black"})

(def app-state (atom {:text "Welcome to Chain Reaction Game"
                      :board (new-board @M @N)
                      :game-status :in-progress
                      :player-to-move "X"
                      :player-data {"X" {:number-of-moves 0, :number-of-boxes 0, :sum-of-boxes 0},
                                    "Y" {:number-of-moves 0, :number-of-boxes 0, :sum-of-boxes 0}}}))

(defn reset-app-state []
    (reset! app-state {:text "Welcome to Chain Reaction Game"
                       :board (new-board @M @N)
                       :game-status :in-progress
                       :player-to-move "X"
                       :player-data {"X" {:number-of-moves 0, :number-of-boxes 0, :sum-of-boxes 0},
                                     "Y" {:number-of-moves 0, :number-of-boxes 0, :sum-of-boxes 0}}}))

(defn update-player-info []
    (let [flattened-board (flatten (@app-state :board))
          box-count-x (count (filter #(= "X" (% :player)) flattened-board))
          box-count-y (count (filter #(= "Y" (% :player)) flattened-board))
          score-x (reduce + (map #(% :number) (filter #(= "X" (% :player)) flattened-board)))
          score-y (reduce + (map #(% :number) (filter #(= "Y" (% :player)) flattened-board)))]
    (swap! app-state assoc-in [:player-data "X" :number-of-boxes] box-count-x)
    (swap! app-state assoc-in [:player-data "Y" :number-of-boxes] box-count-y)
    (swap! app-state assoc-in [:player-data "X" :sum-of-boxes] score-x)
    (swap! app-state assoc-in [:player-data "Y" :sum-of-boxes] score-y)))

(defn win? []
    (let [opponent-data ((@app-state :player-data) ({"X" "Y", "Y" "X"} (@app-state :player-to-move)))]
        (if (and (> (opponent-data :number-of-moves) 0) (= (opponent-data :number-of-boxes) 0))
            (swap! app-state assoc-in [:game-status] (str (@app-state :player-to-move) "-won")))))

(defn max-value [i j]
    (- 3 (count (filter zero? [i j (- (- @M 1) i) (- (- @N 1) j)]))))

(defn valid-index [[i j]]
    (and (>= i 0) (>= j 0) (< i @M) (< j @N)))

(defn neighbours [i j]
    (filter valid-index [[(- i 1) j] [(+ i 1) j] [i (- j 1)] [i (+ j 1)]]))

(defn neighbours-update [i j]
  (loop [neighbour-list (neighbours i j)]
    (if (empty? neighbour-list)
        nil
        (let [neighbour-row (first (first neighbour-list))
              neighbour-column (second (first neighbour-list))]
        (do (swap! app-state assoc-in [:board neighbour-row neighbour-column :player] (@app-state :player-to-move))
            (swap! app-state update-in [:board neighbour-row neighbour-column :number] inc)
            (recur (rest neighbour-list)))))))

(defn split-update [[i j]]
   (if (= (+ 1 (max-value i j)) (get-in @app-state [:board i j :number]))
       (do (swap! app-state assoc-in [:board i j :player] "B")
           (swap! app-state assoc-in [:board i j :number] 0))
       (swap! app-state update-in [:board i j :number] - (+ 1 (max-value i j))))
   (neighbours-update i j))

(defn overall-split-update [li]
    (loop [split-list li]
        (if (empty? split-list)
            nil
            (do (split-update (first split-list))
                (recur (rest split-list))))))

(defn ready-to-split []
    (vec (for [i (range @M)
          j (range @N)
            :when (> (get-in @app-state [:board i j :number]) (max-value i j))]
        [i j])))

(defn play-pause-music []
    (if (.-paused (.getElementById js/document "audio"))
        (.play (.getElementById js/document "audio"))
        (.pause (.getElementById js/document "audio"))))

(defn make-circle [i j n color]
  (cond
    (= n 0) '()
    (= n 1) (render/one-circle i j color)
    (= n 2) (render/two-circles i j color)
    (= n 3) (render/three-circles i j color)))

(defn update-app-state [i j]
  (if (contains? #{"B", (@app-state :player-to-move)} (get-in @app-state [:board i j :player]))
      (do (swap! app-state assoc-in [:board i j :player] (@app-state :player-to-move))
          (swap! app-state update-in [:board i j :number] inc)
          (reset! flag true))))

(defn rectangle [i j]
    [:svg 
      {:on-click (fn [e]
                    (when (and (= :in-progress (@app-state :game-status)) (not @flag))
                          (update-app-state j i)))}
    [:rect 
      {:width 0.88
      :height 0.88
      :fill "Black"
      :x (* 0.9 i)
      :y (* 0.9 j)
      :stroke (player-color (@app-state :player-to-move))
      :stroke-width 0.015}]
    (make-circle i j (get-in @app-state [:board j i :number]) (player-color (get-in @app-state [:board j i :player])))])


(defn chain-reaction []
  [:div
   [:h1 (:text @app-state)]
   [:h4
   (case (get-in @app-state [:game-status])
            :in-progress "Game in progress "
            "X-won" "X-won "
            "Y-won" "Y-won ")
   [:button {:id "restart-game-button"
             :on-click
             (fn [e]
                (reset-app-state))} "Restart the game"]
    [:button {:id "play-pause-music-button"
              :on-click
              (fn [e]
                 (play-pause-music))} "Play/Pause Music"]]
   [:svg
   {:view-box "0 0 10 12"
   :width 750
   :height 750}
   (for [i (range @M)
         j (range @N)]
      [rectangle j i])]
   [:table 
    [:thead 
        [:tr {:style {:background-color "Indigo" }}
            [:td "Player-Name"]
            [:td "No. of moves"]
            [:td "No. of boxes"]
            [:td "Score"]]]
    [:tbody
        [:tr {:style {:background-color "Red"}}
            [:td "X"]
            [:td (get-in @app-state [:player-data "X" :number-of-moves])]
            [:td (get-in @app-state [:player-data "X" :number-of-boxes])]
            [:td (get-in @app-state [:player-data "X" :sum-of-boxes])]]
        [:tr {:style {:background-color "Green"}}
            [:td "Y"]
            [:td (get-in @app-state [:player-data "Y" :number-of-moves])]
            [:td (get-in @app-state [:player-data "Y" :number-of-boxes])]
            [:td (get-in @app-state [:player-data "Y" :sum-of-boxes])]]]]])

(defn start-game-handler []
  (let [rows (js/parseInt (str/trim (.-value (.getElementById js/document "rows"))))
        columns (js/parseInt (str/trim (.-value (.getElementById js/document "columns"))))
        players (js/parseInt (str/trim (.-value (.getElementById js/document "players"))))]
        (reset! M rows)
        (reset! N columns)
        (reset-app-state)
        (set! (.-display (.-style (.getElementById js/document "start-game-container"))) "none")
        (.play (.getElementById js/document "audio"))))

(.addEventListener (.getElementById js/document "start-game-button") "click" start-game-handler)

(js/setInterval (fn [e]
                  (if @flag
                      (if (and (= :in-progress (@app-state :game-status)) (> (count (ready-to-split)) 0))
                          (do
                            (overall-split-update (ready-to-split))
                            (update-player-info)            
                            (win?))
                          (do
                            (reset! flag false)
                            (update-player-info)
                            (swap! app-state update-in [:player-data (@app-state :player-to-move) :number-of-moves] inc)
                            (swap! app-state assoc-in [:player-to-move] ({"X" "Y", "Y" "X"} (@app-state :player-to-move))))))) 
                200)

(reagent/render-component [chain-reaction]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
