(ns midje-doc.ova-scoreboard-example
  (:require [ova.core :refer :all]
            [midje.sweet :refer :all]))

[[:chapter {:title "Scoreboard Example"}]]

[[:section {:title "Setup"}]]


"#### data
 - a scoreboard is used to track player attempts, scores and high-scores"

[[{:numbered false}]]
(def scoreboard
  (ova [{:name "Bill" :attempts 0 :score {:all ()}}
        {:name "John" :attempts 0 :score {:all ()}}
        {:name "Sally" :attempts 0 :score {:all ()}}
        {:name "Fred"  :attempts 0 :score {:all ()}}]))

"#### update notifiers
 - one to print when an attempt has been made to play a game
 - one to print when there is a new highscore"

[[{:numbered false}]]
(add-elem-change-watch
   scoreboard :notify-attempt [:attempts]
   (fn [k o r p n]  ;; key, ova, ref, previous, next
     (println (:name @r) "is on attempt" n)))

(add-elem-change-watch
   scoreboard :notify-high-score [:score :highest]
   (fn [k o r p n]
     (println (:name @r) "has a new highscore: " n)))

"#### score watch
   - When there is a score update, check if it is the highest score. If it is, then update the highest score to the current score"

[[{:numbered false}]]
(add-elem-change-watch
   scoreboard :update-high-score [:score :all]
   (fn [k o r p n]
     (let [hs    [:score :highest]
           high  (get-in @r hs)
           current (first n)]
       (if (and current
                (or (nil? high)
                    (< high current)))
         (dosync (alter r assoc-in hs current))))))

"#### game simulation
 - `sim-game` and `sim-n-games` are used to update the scoreboard
 - the time to finish the game is randomised
 - the wait-time between subsequent games is randomised
 - the score they get is also randomised
"

[[{:numbered false}]]
(defn sim-game [scoreboard name]
  ;; increment number of attempts
  (dosync (!> scoreboard [:name name]
              (update-in [:attempts] inc)))

  ;; simulate game playing time
  (Thread/sleep (rand-int 500))

  ;; conj the newest score at the start of the list
  (dosync (!> scoreboard [:name name]
              (update-in [:score :all] conj (rand-int 50)))))

(defn sim-n-games [scoreboard name n]
  (when (> n 0)
    (Thread/sleep (rand-int 500))
    (sim-game scoreboard name)
    (recur scoreboard name (dec n))))

"#### multi-threading
  - for each player on the scoreboard, they each play a random number of games simultaneously
  - the same scoreboard is used to keep track of state
"

[[{:numbered false}]]
(defn sim! [scoreboard]
  (let [names (map :name scoreboard)]
    (doseq [nm names]
      (future (sim-n-games scoreboard nm (+ 5 (rand-int 5)))))))

[[:section {:title "Simulation"}]]

"A sample simulation is show below:"

[[{:numbered false}]]
(comment
  (sim! scoreboard)

  => [Sally is on attempt 1
      Bill is on attempt 1
      Bill has a new highscore  35
      Sally has a new highscore  40
      John is on attempt 1
      Fred is on attempt 1
      Sally is on attempt 2
      Fred has a new highscore  38
      John has a new highscore  28
      Bill is on attempt 2
      Fred is on attempt 2
      John is on attempt 2
      John is on attempt 3
      Bill is on attempt 3
      Sally is on attempt 3
      Bill has a new highscore  36
      Bill is on attempt 4
      John is on attempt 4
      Fred is on attempt 3
      Sally is on attempt 4
      Fred is on attempt 4
      John is on attempt 5
      Sally is on attempt 5
      Bill is on attempt 5
      Bill has a new highscore  39
      Bill is on attempt 6
      John has a new highscore  30
      Fred is on attempt 5
      John is on attempt 6
      Bill has a new highscore  41
      John is on attempt 7
      Bill is on attempt 7
      Sally is on attempt 6
      John is on attempt 8
      Sally is on attempt 7
      Bill is on attempt 8
      John is on attempt 9
      John has a new highscore  39
      Sally is on attempt 8
      Bill has a new highscore  45
      Sally is on attempt 1
      Fred is on attempt 1
      John is on attempt 1
      Bill is on attempt 1
      Sally has a new highscore  49
      John has a new highscore  49
      Bill has a new highscore  3
      Bill is on attempt 2
      Fred has a new highscore  22
      John is on attempt 2
      Bill has a new highscore  18
      Fred is on attempt 2
      Bill is on attempt 3
      Sally is on attempt 2
      John is on attempt 3
      Fred is on attempt 3
      Bill has a new highscore  39
      Fred has a new highscore  47
      Fred is on attempt 4
      John is on attempt 4
      Bill is on attempt 4
      Sally is on attempt 3
      Fred is on attempt 5
      Sally is on attempt 4
      John is on attempt 5
      Bill is on attempt 5
      Sally is on attempt 5
      Bill is on attempt 6
      John is on attempt 6
      Sally is on attempt 6
      Sally is on attempt 7
      John is on attempt 7
      Bill is on attempt 7
      Bill is on attempt 8
      Sally is on attempt 8
      Bill has a new highscore  44
      Bill is on attempt 9
      Bill has a new highscore  45]

  (<< scoreboard)

  => [{:name "Bill", :attempts 9, :score {:highest 45, :all (45 44 36 9 24 25 39 18 3)}}
      {:name "John", :attempts 7, :score {:highest 49, :all (20 37 32 8 48 37 49)}}
      {:name "Sally", :attempts 8, :score {:highest 49, :all (1 48 7 12 43 0 39 49)}}
      {:name "Fred", :attempts 5, :score {:highest 47, :all (16 40 47 15 22)}}])
