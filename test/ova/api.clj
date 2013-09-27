(ns ova.api
  (:require [ova.core :refer :all]
            [midje.sweet :refer :all]))

[[:chapter {:title "API Reference"}]]


[[:section {:title "Basics"}]]
[[:subsection {:title "ova"}]]
"An `ova` deals with data in a vector. The data can be anything but it is recommended that the data are clojure maps."

[[{:numbered false}]]
(fact
  (def ov (ova [1 2 3 4]))

  (def ov (ova [{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
                 {:id :a2 :score 15 :name "John"  :gender :m :nationality :aus}]))

  (def ov (ova [{:type "form" :data {:sex :m :age 23}}
                 {:type "form" :data {:sex :f :age 24}}])))


[[:subsection {:title "persistent!"}]]
"Since `ova.core.Ova` implements the `clojure.lang.ITransientCollection` interface, it can be made persistent with `persistent!`."

[[{:numbered false}]]
(fact
  (persistent! (ova [1 2 3 4]))
  => [1 2 3 4])

[[:subsection {:title "reinit!"}]]

"`reinit!` resets the data elements in an ova to another set of values. Any change in the ova requires it to be wrapped in a `dosync` macro."

[[{:numbered false}]]
(fact
  (def ov (ova [1 2 3 4]))
  (dosync (reinit! ov [5 6 7 8 9]))
  (persistent! ov)
  => [5 6 7 8 9])

[[:subsection {:title "<<"}]]
"The output macro is a shorthand for outputting the value of `ova` after a series of transformations. There is an implicit `dosync` block within the macro."
[[{:numbered false}]]
(fact
  (<< (def ov (ova [1 2 3 4]))
      (reinit! ov [5 6 7 8 9]))
  => [5 6 7 8 9])


[[:section {:title "Clojure"}]]

"Built-in operations supported including (but not limited to):

   - `map`, `reduce`, `first`, `next`, `nth` and many more `seq` operations
   - `get`, `contains?`
   - `deref`
   - `add-watch`, `remove-watch`
   - `pop!`, `push!`, `conj!`
"


[[:section {:title "Query"}]]

"Where ova shines is in the various ways that elements can be selected. It is best to define some data that can be queried:"

[[{:numbered false}]]
(def players
  (ova [{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}
        {:id :a2 :score 15 :info {:name "John"  :gender :m :nationality :aus}}
        {:id :a3 :score 15 :info {:name "Dave"  :gender :m :nationality :aus}}
        {:id :a4 :score 11 :info {:name "Henry" :gender :m :nationality :usa}}
        {:id :a5 :score 20 :info {:name "Scott" :gender :m :nationality :usa}}
        {:id :a6 :score 13 :info {:name "Tom"   :gender :m :nationality :usa}}
        {:id :a7 :score 15 :info {:name "Jill"  :gender :f :nationality :aus}}
        {:id :a8 :score 19 :info {:name "Sally" :gender :f :nationality :usa}}
        {:id :a9 :score 13 :info {:name "Rose"  :gender :f :nationality :aus}}]))

[[:subsection {:title "select"}]]

"##### Index:"

[[{:numbered false}]]
(fact
  (select players 0)
  => #{{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}})

"##### Predicates:"
[[{:numbered false}]]
(fact
  (select players #(= (:id %) :a1))
  => #{{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}})

"##### List Predicates:"
[[{:numbered false}]]
(fact
  (select players '(:id (= :a1)))
  => #{{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}})

"##### Vector Predicates:"
[[{:numbered false}]]
(fact
  (select players [:id :a1])
  => #{{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}}

  (select players [:score even?])
  => #{{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}
       {:id :a5 :score 20 :info {:name "Scott" :gender :m :nationality :usa}}}

  (select players [:score '(< 13)])
  => #{{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}
       {:id :a4 :score 11 :info {:name "Henry" :gender :m :nationality :usa}}}

  (select players [:score 13 [:info :gender] :f])
  => #{{:id :a9 :score 13 :info {:name "Rose"  :gender :f :nationality :aus}}})

"##### Sets:"
[[{:numbered false}]]
(fact
  (select players #{1 2})
  => #{{:id :a2 :score 15 :info {:name "John"  :gender :m :nationality :aus}}
       {:id :a3 :score 15 :info {:name "Dave"  :gender :m :nationality :aus}}}

  (select players #{[:score even?] [:score 13 [:info :gender] :f]})
  => #{{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}
       {:id :a5 :score 20 :info {:name "Scott" :gender :m :nationality :usa}}
       {:id :a9 :score 13 :info {:name "Rose"  :gender :f :nationality :aus}}})

[[:subsection {:title "selectv"}]]

"`selectv` is the same as `select` except it returns a vector instead of a set."
[[{:numbered false}]]
(fact
  (selectv players #{[:score even?] [:score 13 [:info :gender] :f]})
  => [{:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}
      {:id :a5 :score 20 :info {:name "Scott" :gender :m :nationality :usa}}
      {:id :a9 :score 13 :info {:name "Rose"  :gender :f :nationality :aus}}])



[[:subsection {:title "fn"}]]
"`ova` implements the `clojure.lang.IFn` interface and so can be called with select parameters. It can be used to return elements within an array. Additionally, if an element has an :id tag, it will search based on the :id tag."

[[{:numbered false}]]
(fact
  (players 0)
  => {:id :a1 :score 10 :info {:name "Bill"  :gender :m :nationality :aus}}

  (players 1)
  => {:id :a2 :score 15 :info {:name "John"  :gender :m :nationality :aus}}

  (players :a3)
  => {:id :a3 :score 15 :info {:name "Dave"  :gender :m :nationality :aus}}

  (:a3 players)
  => {:id :a3 :score 15 :info {:name "Dave"  :gender :m :nationality :aus}}

  (ov :a10)
  => nil)

[[:section {:title "Array Operations"}]]

[[:subsection {:title "append!"}]]
"`append!` adds additional elements to the end:"

[[{:numbered false}]]
(fact
  (<< (append! (ova [1 2 3 4])
               5 6 7 8))
  => [1 2 3 4 5 6 7 8])

[[:subsection {:title "concat!"}]]
"`concat!` joins an array at the end:"

[[{:numbered false}]]
(fact
  (<< (concat! (ova [1 2 3 4])
               [5 6 7 8]))
  => [1 2 3 4 5 6 7 8])


[[:subsection {:title "insert!"}]]
"`insert!` allows elements to be inserted."

[[{:numbered false}]]
(fact
  (<< (insert! (ova [:a :b :c :e :f])
               :d 3))
   => [:a :b :c :d :e :f])


[[:subsection {:title "empty!"}]]
"`empty!` clears all elements"

[[{:numbered false}]]
(fact
  (<< (empty! (ova [:a :b :c :d])))
  => [])


[[:subsection {:title "remove!"}]]
"`remove!` will selectively remove elements from the `ova`. The query syntax can be used"

[[{:numbered false}]]
(fact
  (<< (remove! (ova [:a :b :c :d])
               '(= :a)))
  => [:b :c :d]

  (<< (remove! (ova [1 2 3 4 5 6 7 8 9])
               #{'(< 3) '(> 6)}))
  => [3 4 5 6])


[[:subsection {:title "filter!"}]]
"`filter!` performs the opposite of `remove!`. It will keep all elements in the array that matches the query."

[[{:numbered false}]]
(fact
  (<< (filter! (ova [:a :b :c :d])
               '(= :a)))
  => [:a]

  (<< (filter! (ova [1 2 3 4 5 6 7 8 9])
             #{'(< 3) '(> 6)}))
  => [1 2 7 8 9])

[[:subsection {:title "sort!"}]]
"`sort!` arranges the array in order of the comparator. It can take only a comparator, or a selector/comparator combination."

[[{:numbered false}]]
(fact
  (<< (sort! (ova [9 8 7 6 5 4 3 2 1])
             <))
  => [1 2 3 4 5 6 7 8 9]

  (<< (sort! (ova [1 2 3 4 5 6 7 8 9])
             identity >))
  => [9 8 7 6 5 4 3 2 1])

[[:subsection {:title "reverse!"}]]
"`reverse!` arranges array elements in reverse"

[[{:numbered false}]]
(fact
  (<< (reverse! (ova [1 2 3 4 5 6 7 8 9])))
  => [9 8 7 6 5 4 3 2 1])

[[:section {:title "Element Operations"}]]
"Element operations are specific to manipulating the elements within the array."

[[:subsection {:title "!!"}]]
"`!!` sets the value of all selected indices to a specified value."

[[{:numbered false}]]
(fact
  (<< (!! (ova [1 2 3 4 5 6 7 8 9]) 0 0))
  => [0 2 3 4 5 6 7 8 9]


  (<< (!! (ova [1 2 3 4 5 6 7 8 9]) odd? 0))
  => [0 2 0 4 0 6 0 8 0]


  (<< (!! (ova [1 2 3 4 5 6 7 8 9]) '(> 4) 0))
  => [1 2 3 4 0 0 0 0 0])

[[:subsection {:title "map!"}]]
"`map!` performs an operation on every element."

[[{:numbered false}]]
(fact
  (<< (map! (ova [1 2 3 4 5 6 7 8 9])
            inc))
  => [2 3 4 5 6 7 8 9 10])


[[:subsection {:title "smap!"}]]
"`smap!` performs an operation only on selected elements"
[[{:numbered false}]]
(fact
  (<< (smap! (ova [1 2 3 4 5 6 7 8 9])
             odd? inc))
  => [2 2 4 4 6 6 8 8 10])


[[:subsection {:title "map-indexed!"}]]
"`map-indexed!` performs an operation with the element index as the second parameter on every element"

[[{:numbered false}]]
(fact
  (<< (map-indexed! (ova [1 2 3 4 5 6 7 8 9])
                    +))
  => [1 3 5 7 9 11 13 15 17])


[[:subsection {:title "smap-indexed!"}]]
"`smap-indexed!` performs an operation with the element index as the second parameter on selected elements"

[[{:numbered false}]]
(fact
  (<< (smap-indexed! (ova [1 2 3 4 5 6 7 8 9])
                     odd? +))
  => [1 2 5 4 9 6 13 8 17])

[[:subsection {:title "!>"}]]
"The threading array performs a series of operations on selected elements."
[[{:numbered false}]]
(fact
  (<< (!> (ova [1 2 3 4 5 6 7 8 9])
          odd?
          (* 10)
          (+ 5)))
  => [15 2 35 4 55 6 75 8 95])


[[:section {:title "Element Watch"}]]

"Watches can be set up so that. Instead of a normal ref/atom watch where there are four inputs to the watch function, the Element watch requires an additional input to distinguish which array a change has occured. The function signature looks like:"

[[{:numbered false}]]
(comment
  (fn [o r k p v]  ;; ova, ref, key, prev, current
    (... do something ...)))

[[:subsection {:title "get-elem-watch"}]]

"`get-elem-watches` takes as input an `ova` and returns a map of element watches and their keys."

[[:subsection {:title "add-elem-watch"}]]

"`add-elem-watch` adds a watch function on all elements of an `ova`."

[[{:numbered false}]]
(fact
  (def ov     (ova [1 2 3 4]))
  (def watch  (atom []))
  (def cj-fn  (fn  [o r k p v]  ;; ova, ref, key, prev, current
                (swap! watch conj [p v])))

  (add-elem-watch ov :conj cj-fn) ;; add watch
  (keys (get-elem-watches ov))    ;; get watches
  => [:conj]

  (<< (map! ov + 10))   ;; elements in ov are manipulated
  => [11 12 13 14]

  (sort @watch)
  => [[1 11] [2 12] [3 13] [4 14]]) ;; watch is also changed

[[:subsection {:title "remove-elem-watch"}]]

"`remove-elem-watch` cleares the element watch function to a `ova`."

[[{:numbered false}]]
(fact
  (remove-elem-watch ov :conj)
  (keys (get-elem-watches ov))
  => nil)
