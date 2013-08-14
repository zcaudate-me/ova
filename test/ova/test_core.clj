(ns ova.test-core
  (:use midje.sweet)
  (:require [ova.core :as v]
            [ova.fn :refer [check]] :reload))

(def ^:dynamic *ova* (v/ova))

(def ^:dynamic *data*
  [{:id :1 :val 1} {:id :2 :val 1}
   {:id :3 :val 2} {:id :4 :val 2}])

(defn to-int [s]
  (Long/parseLong s))

(defn is-ova [& [chk]]
  (fn [obj]
    (and (= "class ova.core.Ova" (str (type obj)))
         (let [schk (or chk (sequence chk))]
           (check (persistent! obj) schk)))))

(facts "invoke"
  (dosync (v/reinit! *ova* *data*))

  (*ova* 0) => {:val 1, :id :1}
  (*ova* :1) => {:val 1, :id :1}
  (*ova* :id :1) => {:val 1, :id :1}
  (*ova* :val 1) => {:val 1, :id :1}
  (*ova* '(:id) :1) => {:val 1, :id :1}
  (*ova* (list :id name) "1") => {:val 1, :id :1}
  (*ova* (list :val) even?) => {:val 2, :id :3}
  (*ova* (list :id name) "1") => {:val 1, :id :1})

(facts "indices"
  (dosync (v/reinit! *ova* *data*))

  (:1 *ova*) => {:val 1, :id :1}
  (*ova* :1) => {:val 1, :id :1}
  (v/indices *ova*) => #{0 1 2 3}
  (v/indices *ova* 0) => #{0}
  (v/indices *ova* 2) => #{2}
  (v/indices *ova* #{1 2}) => #{1 2}
  (v/indices *ova* #{0}) => #{0}
  (v/indices *ova* #{4}) => #{}
  (v/indices *ova* :1) => #{}
  (v/indices *ova* [:val odd?]) => #{0 1}
  (v/indices *ova* #(even? (:val %))) => #{2 3}
  (v/indices *ova* [:id :1]) => #{0}
  (v/indices *ova* [:val 1]) => #{0 1}
  (v/indices *ova* [:val even?]) => #{2 3}
  (v/indices *ova* [:val even? '(:id (name) (to-int)) odd?]) => #{2}
  )

(fact "select will grab the necessary entries"
  (dosync (v/reinit! *ova* *data*))

  (v/select *ova*) => #{{:id :1, :val 1} {:id :2, :val 1} {:id :3, :val 2} {:id :4, :val 2}}
  (v/select *ova* 0) => #{{:id :1 :val 1}}
  (v/select *ova* 2) => #{{:id :3 :val 2}}
  (v/select *ova* #{1 2}) => #{{:id :2 :val 1} {:id :3 :val 2}}
  (v/select *ova* #{0}) => #{{:id :1 :val 1}}
  (v/select *ova* #{4}) => #{}
  (v/select *ova* 2) => #{{:id :3 :val 2}}
  (v/select *ova* [:id '((name) (to-int) (odd?))])
  => #{{:id :1 :val 1} {:id :3 :val 2}}
  (v/select *ova* #(even? (:val %))) => #{{:id :3 :val 2} {:id :4 :val 2}}
  (v/select *ova* [:id :1]) => #{{:id :1 :val 1}}
  (v/select *ova* [:val 1]) => #{{:id :1 :val 1} {:id :2 :val 1}}
  (v/select *ova* [:val nil?]) => #{}
  (v/select *ova* '(:id (name) (to-int) odd?))
  => #{{:id :1 :val 1} {:id :3 :val 2}}
  (v/select *ova* [:val even?]) => #{{:id :3 :val 2} {:id :4 :val 2}}
  (v/select *ova* [:val even? :id :3]) => #{{:id :3 :val 2}}
  (v/select *ova* #{[:id :1] [:val 2]})
  => #{{:id :1 :val 1} {:id :3 :val 2} {:id :4 :val 2}})

(against-background
  [(before :checks
           (dosync (v/reinit! *ova* *data*)))]

  (fact "map!"
    (dosync (v/reinit! *ova* *data*))

    (dosync (v/map! *ova* dissoc :val))
    => (is-ova [{:id :1} {:id :2} {:id :3} {:id :4}])

    (dosync (v/map! *ova* assoc :val 10))
    => (is-ova [{:id :1 :val 10} {:id :2 :val 10}
                {:id :3 :val 10} {:id :4 :val 10}])

    (dosync (v/map! *ova* empty?))
    => (is-ova [false false false false]))

  (fact "map-indexed!"
    (dosync (v/map-indexed! *ova* (fn [i obj] (assoc obj :val i))))
    => (is-ova [{:id :1 :val 0} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4 :val 3}]))

  (fact "smap!"
    (dosync (v/smap! *ova* [:val 1] assoc :val 100))
    => (is-ova [{:id :1 :val 100} {:id :2 :val 100}
                {:id :3 :val 2} {:id :4 :val 2}])

    (dosync (v/smap! *ova* [:id :4 :val 2] dissoc :val))
    => (is-ova [{:id :1 :val 1} {:id :2 :val 1}
                {:id :3 :val 2} {:id :4}])

    (dosync (v/smap! *ova* #{[:id :4] [:val 1]} dissoc :val))
    => (is-ova [{:id :1} {:id :2} {:id :3 :val 2} {:id :4}]))

  (fact "smap-indexed!"
    (dosync (v/smap-indexed! *ova* [:val 1] (fn [i obj] {:id i})))
    => (is-ova [{:id 0} {:id 1}
                {:id :3 :val 2} {:id :4 :val 2}])))


(against-background
  [(before :checks
           (dosync (v/reinit! *ova* (range 10))))]

 (fact "reverse!"
    (dosync (v/reverse! *ova*)) => (is-ova [9 8 7 6 5 4 3 2 1 0]))

  (fact "sort"
    (dosync (v/sort! *ova* >)) => (is-ova [9 8 7 6 5 4 3 2 1 0])
    (dosync (v/sort! *ova* <)) => (is-ova (range 10)))

  (fact "concat!"
    (dosync (v/concat! *ova* (range 10 20))) => (is-ova (range 20))
    (dosync (v/concat! *ova* *ova*)) => (is-ova (concat (range 10) (range 10))))

  (fact "append!"
    (dosync (v/append! *ova* 10 11 12)) => (is-ova (range 13)))

  (fact "insert!"
    (dosync (v/insert! *ova* 10)) => (is-ova [0 1 2 3 4 5 6 7 8 9 10])
    (dosync (v/insert! *ova* 5 5)) => (is-ova [0 1 2 3 4 5 5 6 7 8 9])
    (dosync (v/insert! *ova* :N 0)) => (is-ova [:N 0 1 2 3 4 5 6 7 8 9])
    (dosync (v/insert! *ova* :N 10)) => (is-ova [0 1 2 3 4 5 6 7 8 9 :N]))

  (fact "filter!"
    (dosync (v/filter! *ova* odd?)) => (is-ova [1 3 5 7 9])
    (dosync (v/filter! *ova* '(not= 3))) => (is-ova [0 1 2 4 5 6 7 8 9])
    (dosync (v/filter! *ova* #{'(< 3) '(> 6)})) => (is-ova [0 1 2 7 8 9])
    (dosync (v/filter! *ova* [identity '(>= 3) identity '(<= 6)]))
    => (is-ova [ 3 4 5 6]))

  (fact "remove!"
    (dosync (v/remove! *ova* odd?)) => (is-ova [0 2 4 6 8])
    (dosync (v/remove! *ova* '(not= 3))) => (is-ova [3])
    (dosync (v/remove! *ova* #{'(< 3) '(> 6)})) => (is-ova [3 4 5 6])))

(facts "remove! using array checks"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1}
                              {:id 3 :val 2} {:id 4 :val 2}]))))
    (dosync (v/remove! ov [:id 1]))
    => (is-ova [{:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}])
    (dosync (v/remove! ov [:val 2]))
    => (is-ova [{:id 1 :val 1} {:id 2 :val 1}])
    (dosync (v/remove! ov #(odd? (:id %))))
    => (is-ova [{:id 2 :val 1} {:id 4 :val 2}])
    (dosync (v/remove! ov #(even? (:val %))))
    => (is-ova [{:id 1 :val 1} {:id 2 :val 1}])
    (dosync (v/remove! ov #{1 2}))
    => (is-ova [{:id 1 :val 1} {:id 4 :val 2}])
    (dosync (v/remove! ov #{0}))
    => (is-ova [{:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}])
    (dosync (v/remove! ov #{4}))
    => (is-ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}])
    (dosync (v/remove! ov [:id odd?]))
    => (is-ova  [{:id 2 :val 1} {:id 4 :val 2}])
    (dosync (v/remove! ov [:val even?]))
    => (is-ova [{:id 1 :val 1} {:id 2 :val 1}])
    (dosync (v/remove! ov [:id odd? :val even?]))
    => (is-ova [{:id 1 :val 1} {:id 2 :val 1} {:id 4 :val 2}]))



(facts "!>set is like update but replaces the entire cell"
  (against-background
    (before :checks
            (def ov (v/ova [1 2 3 4 5 6]))))
    (dosync (v/!>set ov 0 0)) => (is-ova 0 2 3 4 5 6)
    (dosync (v/!>set ov #{1 2} 0)) => (is-ova 1 0 0 4 5 6)
    (dosync (v/!>set ov odd? 0)) => (is-ova 0 2 0 4 0 6)
    (dosync (v/!>set ov [:id 1] 0)) => (is-ova 1 2 3 4 5 6)
    (dosync (v/!>set (v/ova [{:id 1 :val 1} {:id 2 :val 1}]) [:id 1] 0))
    => (is-ova [0 {:id 2 :val 1}]))

(facts "!>merge will operate on maps only"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 2} 0]))))

  (dosync (v/!>merge ov 0 {:val 2}))
  => (is-ova [{:id 1 :val 2} {:id 2 :val 2} 0])

  (dosync (v/!>merge ov 1 {:id 3 :val 3 :valb 4}))
  => (is-ova [{:id 1 :val 1} {:id 3 :val 3 :valb 4} 0])
  (dosync (v/!>merge ov 2))
  => (throws Exception)
  (dosync (v/!>merge ov [:id 1] {:val 2}))
  => (is-ova [{:id 1 :val 2} {:id 2 :val 2} 0]))

(facts "!>merge using array checks"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 2}]))))

  (dosync (v/!>merge ov [:id 1] {:val 2}))
  => (is-ova [{:id 1 :val 2} {:id 2 :val 2}])
  (dosync (v/!>merge ov [:val 2] {:val 3}))
  => (is-ova [{:id 1 :val 1} {:id 2 :val 3}]))

(facts "!>merge using array checks"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1}
                            {:id 3 :val 2} {:id 4 :val 2}]))))

  (dosync (v/!>merge ov [:id 1] {:val 2}))
  => (is-ova [{:id 1 :val 2} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}])
  (dosync (v/!>merge ov [:val 2] {:val 3}))
  => (is-ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 3}])
  (dosync (v/!>merge ov #(odd? (:id %)) {:val 3}))
  => (is-ova [{:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2}])
  (dosync (v/!>merge ov #(even? (:val %)) {:val 4}))
  => (is-ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4}])
  (dosync (v/!>merge ov #{1 2} {:val 4}))
  => (is-ova [{:id 1 :val 1} {:id 2 :val 4} {:id 3 :val 4} {:id 4 :val 2}])
  (dosync (v/!>merge ov #{0} {:val 4}))
  => (is-ova [{:id 1 :val 4} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}])
  (dosync (v/!>merge ov #{4} {:val 4}))
  => (is-ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}])
  (dosync (v/!>merge ov [:id odd?] {:val 3}))
  => (is-ova [{:id 1 :val 3} {:id 2 :val 1} {:id 3 :val 3} {:id 4 :val 2}])
  (dosync (v/!>merge ov [:val even?] {:val 4}))
  => (is-ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 4} {:id 4 :val 4}])
  (dosync (v/!>merge ov [:id odd? :val even?] {:valb 4}))
  => (is-ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2 :valb 4} {:id 4 :val 2}]))

(facts "!>update-in"
  (against-background
    (before :checks
            (def ov (v/ova [{:id 1 :val {:a 1}}]))))
  (dosync (v/!>update-in ov [:id 1] [:val :x] (constantly 2)))
  => (is-ova [{:id 1 :val {:a 1 :x 2}}])
  (dosync (v/!>update-in ov [:id 1] [:val :a] (constantly 2)))
  => (is-ova [{:id 1 :val {:a 2}}]))

(facts "!>assoc-in"
    (against-background
      (before :checks
              (def ov (v/ova [{:id 1 :val {:a 1}}]))))
    (dosync (v/!>assoc-in ov [:id 1] [:val :x] 2))
    => (is-ova [{:id 1 :val {:a 1 :x 2}}])
    (dosync (v/!>assoc-in ov [:id 1] [:val :a] 2))
    => (is-ova [{:id 1 :val {:a 2}}]))

(facts "testing the add-elem-watch function with map"
    (let [ov     (v/ova [1 2 3 4])
          out    (atom [])
          cj-fn  (fn  [o r k p v & args]
                   ;;(prchkntln o r k p v args )
                   (swap! out conj [p v]))
          _      (v/add-elem-watch ov :conj cj-fn)
          _      (dosync (v/map! ov inc))]
      (facts "out is updated"
        ov => (is-ova [2 3 4 5])
        (sort @out) => [[1 2] [2 3] [3 4] [4 5]])))

(facts "testing the add-elem-watch function when an element has been deleted"
    (let [ov     (v/ova [1 2 3 4])
          out    (atom [])
          cj-fn  (fn  [_ _ _ p v & args]
                   (swap! out conj [p v]))
          _      (v/add-elem-watch ov :conj cj-fn)
          _      (dosync (v/remove! ov 0))
          _      (dosync (v/map! ov inc))]
      (facts "out is updated"
        ov => (is-ova [3 4 5])
        (sort @out) => [[2 3] [3 4] [4 5]])))

(facts "testing the add-elem-watch function when an elements are arbitrarily "
    (let [ov     (v/ova [1 2 3 4])
          out    (atom [])
          cj-fn  (fn  [_ _ _ p v & args]
                   (swap! out conj [p v]))
          _      (v/add-elem-watch ov :conj cj-fn)]

      (dosync (v/insert! ov 1 3))
      (fact
        ov => (is-ova [1 2 3 1 4])
        @out => [])

      (dosync (v/remove! ov odd?))
      (fact
        ov => (is-ova [2 4])
        @out => [])

      (dosync (v/map! ov inc))
      (fact
        ov => (is-ova [3 5])
        @out => (just [[2 3] [4 5]] :in-any-order))

      (dosync (v/concat! ov [1 2 3 4 5]))
      (fact
        ov  => (is-ova [3 5 1 2 3 4 5])
        @out => (just [[2 3] [4 5]] :in-any-order))

      (dosync (v/sort! ov))
      (fact
        ov  => (is-ova [1 2 3 3 4 5 5])
        @out => (just [[2 3] [4 5]] :in-any-order))))
