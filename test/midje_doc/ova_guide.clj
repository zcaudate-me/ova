(ns midje-doc.ova-guide
  (:require [ova.core :refer :all]
            [midje.sweet :refer :all]))

[[:chapter {:title "Installation"}]]

"Add to `project.clj`"

[[{:numbered false}]]
(comment [im.chit/ova "0.9.6"])

"All functions are in the `ova.core` namespace."

[[{:numbered false}]]
(comment (use 'ova.core))

[[:chapter {:title "Motivation"}]]

"An `ova` represents a mutable array of elements. The question should really be asked: Why?

**Biased Answer:** Because it is the most fully featured and *bestest* mutable array.... *EVER!*

In all seriousness, `ova` has been designed especially for dealing with shared mutable state in multi-threaded applications. Clojure uses `refs` and `atoms` off the shelf to resolve this issue but left out methods to deal with arrays of shared elements. `ova` has been specifically designed for the following use case:

 - Elements (usually clojure maps) can be added or removed from an array
 - Element data are accessible and mutated from several threads.
 - Array itself can also be mutated from several threads.

These type of situations are usally co-ordinated using a external cache store like redis. Ova is no where near as fully featured as these libraries. The actual `ova` datastructure is a `ref` containing a `vector` containing ref. The library comes with a whole bundle of goodies to deal with mutation:

 - Clean element selection and array manipulation syntax
 - Watches for both the array and array elements
 - Designed to play nicely with `dosync` and `refs`
 - Pure clojure

The library has been abstracted out of [cronj](https://github.com/zcaudate/cronj), a task scheduling library where it is used to track and manipulate shared state. The `ova` syntax abstracts away alot of clutter. An example of tracking state in a multi-threaded environment can be seen in a [scoreboard example](#scoreboard-example):
"

[[:chapter {:title "Walkthrough"}]]

"The key to `ova` lies in the ease of manipulating the postions of elements within an array as well as updating the elements themselves. We begin by constructing and displaying an ova."

[[:section {:title "Constructor"}]]
[[{:numbered false}]]
(fact
  (def ov
    (ova [{:val 1} {:val 2}
          {:val 3} {:val 4}]))

  (-> ov class str)
  => "class ova.core.Ova")

[[:section {:title "Dereferencing"}]]
"An `ova` is a `ref` of a `vector` of `refs`. They are dereferenced accordingly:"
[[{:numbered false}]]
(fact
  (mapv deref (deref ov))
  => [{:val 1} {:val 2}
      {:val 3} {:val 4}]

  (<< ov)                     ;; Shorthand
  => [{:val 1} {:val 2}
      {:val 3} {:val 4}])

[[:section {:title "Append / Insert / Concat"}]]
"Adding elements to the ova is very straight forward:"

[[{:numbered false}]]
(fact
  (<< (append! ov {:val 6}))         ;; Append
  => [{:val 1} {:val 2} {:val 3}
      {:val 4} {:val 6}]

  (<< (insert! ov {:val 5} 4))       ;; Insert
  => [{:val 1} {:val 2} {:val 3}
      {:val 4} {:val 5} {:val 6}]

  (<< (concat! ov [{:val 7}          ;; Concat
                   {:val 8}]))
  => [{:val 1} {:val 2} {:val 3}
      {:val 4} {:val 5} {:val 6}
      {:val 7} {:val 8}])

[[:section {:title "Select"}]]
"Where `ova` really shines is in the mechanism by which elements are selected. There are abundant ways of selecting elements - by index, by sets, by vectors, by predicates and by lists. The specific mechanism will be described more clearly in later sections."

[[{:numbered false}]]
(fact
  (select ov 0)                      ;; By Index
  => #{{:val 1}}

  (select ov #{0 1})                 ;; By Set of Index
  => #{{:val 1} {:val 2}}

  (select ov {:val 3})               ;; By Item
  => #{{:val 3}}

  (select ov #{{:val 3} {:val 4}})   ;; By Set of Items
  => #{{:val 3} {:val 4}}

  (select ov #(-> % :val even?))     ;; By Predicate
  => #{{:val 2} {:val 4}
       {:val 6} {:val 8}}

  (select ov '(:val even?))          ;; By List
  => #{{:val 2} {:val 4}
       {:val 6} {:val 8}}

  (select ov [:val 3])               ;; By Vector/Value
  => #{{:val 3}}

  (select ov [:val #{1 2 3}])       ;; By Vector/Set
  => #{{:val 1} {:val 2} {:val 3}}

  (select ov [:val '(< 4)])         ;; By Vector/List
  => #{{:val 1} {:val 2} {:val 3}}

  (select ov [:val even?            ;; By Vector/Predicate/List
              :val '(> 4)])
  => #{{:val 6} {:val 8}})

[[:section {:title "Remove / Filter"}]]
"`remove!` and `filter!` also use the same mechanism as `select`:"

[[{:numbered false}]]
(fact
  (<< (remove! ov 7))               ;; Index Notation
  => [{:val 1} {:val 2} {:val 3}
      {:val 4} {:val 5} {:val 6}
      {:val 7}]

  (<< (filter! ov #{1 2 3 4 5 6}))  ;; Set Notation
  => [{:val 2} {:val 3} {:val 4}
      {:val 5} {:val 6} {:val 7}]

  (<< (filter! ov [:val odd?]))     ;; Vector/Fn Notation
  => [{:val 3} {:val 5} {:val 7}]

  (<< (remove! ov [:val '(> 3)]))   ;; List Notation
  => [{:val 3}])

[[:section {:title "Sorting"}]]
"The `sort!` functions allows elements in the ova to be rearranged. The function becomes clearer to read with access and comparison defined seperately (last example)."

[[{:numbered false}]]
(fact
  (def ov (ova (map (fn [n] {:val n})
                    (range 8))))

  (<< ov)
  => [{:val 0} {:val 1} {:val 2}
      {:val 3} {:val 4} {:val 5}
      {:val 6} {:val 7}]

  (<< (sort! ov (fn [a b]          ;; Fn
                  (> (:val a)
                     (:val b)))))
  => [{:val 7} {:val 6} {:val 5}
      {:val 4} {:val 3} {:val 2}
      {:val 1} {:val 0}]

  (<< (sort! ov [:val] <))         ;; Accessor/Comparater
  => [{:val 0} {:val 1} {:val 2}
      {:val 3} {:val 4} {:val 5}
      {:val 6} {:val 7}])

[[:section {:title "Manipulation"}]]
"Using the same mechanism as `select`, bulk update of elements within the `ova` can be performed in a succint manner:"

[[{:numbered false}]]
(fact
  (def ov (ova (map (fn [n] {:val n})
                    (range 4))))

  (<< ov)
  => [{:val 0} {:val 1} {:val 2} {:val 3}]

  (<< (map! ov update-in [:val] inc))        ;; map! updates all elements
  => [{:val 1} {:val 2} {:val 3} {:val 4}]

  (<< (smap! ov [:val odd?]                  ;; update only odd elements
             update-in [:val] #(+ 10 %)))
  => [{:val 11} {:val 2} {:val 13} {:val 4}]

  (<< (smap! ov 0 update-in                     ;; update element at index 0
          [:val] #(- % 10)))
  => [{:val 1} {:val 2} {:val 13} {:val 4}]

  (<< (smap! ov [:val 13]                       ;; update element with :val of 13
          update-in [:val] #(- % 10)))
  => [{:val 1} {:val 2} {:val 3} {:val 4}]

  (<< (smap! ov [:val even?]                    ;; assoc new data to even :vals
          assoc-in [:x :y :z] 10))
  => [{:val 1} {:val 2 :x {:y {:z 10}}}
      {:val 3} {:val 4 :x {:y {:z 10}}}]

  (<< (smap! ov [:x :y :z] dissoc :x))          ;; dissoc :x for elements with nested [:x :y :z] keys
  => [{:val 1} {:val 2} {:val 3} {:val 4}]
  )


[[:section {:title "Ova Watch"}]]
"Because a ova is simply a ref, it can be watched for changes"

[[{:numbered false}]]
(fact
  (def ov (ova [0 1 2 3 4 5]))

  (def output (atom []))
  (add-watch ov
             :old-new
             (fn [ov k p n]
               (swap! output conj [(mapv deref p)
                                   (mapv deref n)])))

  (do (dosync (sort! ov >))
      (deref output))
  => [[[0 1 2 3 4 5]
       [5 4 3 2 1 0]]])

[[:section {:title "Element Watch"}]]
"Entire elements of the ova can be watched. A more substantial example can be seen in the [scoreboard example](#scoreboard-example):"

[[{:numbered false}]]
(fact
  (def ov (ova [0 1 2 3 4 5]))

  (def output (atom []))

  (add-elem-watch      ;; key, ova, ref, previous, next
      ov :elem-old-new
      (fn [k o r p n]
        (swap! output conj [p n])))

  (<< (!! ov 0 :zero))
  => [:zero 1 2 3 4 5]

  (deref output)
  => [[0 :zero]]

  (<< (!! ov 3 :three))
  => [:zero 1 2 :three 4 5]

  (deref output)
  => [[0 :zero] [3 :three]])

[[:subsection {:title "Element Change Watch"}]]
"The `add-elem-change-watch` function can be used to only notify when an element has changed."

[[{:numbered false}]]
(fact
  (def ov (ova [0 1 2 3 4 5]))

  (def output (atom []))

  (add-elem-change-watch   ;; key, ova, ref, previous, next
     ov :elem-old-new  identity
     (fn [k o r p n]
       (swap! output conj [p n])))

  (do (<< (!! ov 0 :zero))  ;; a pair is added to output
      (deref output))
  => [[0 :zero]]

  (do (<< (!! ov 0 0))      ;; another pair is added to output
      (deref output))
  => [[0 :zero] [:zero 0]]

  (do (<< (!! ov 0 0))      ;; no change to output
      (deref output))
  => [[0 :zero] [:zero 0]]
)

[[:section {:title "Clojure Protocols"}]]
"`ova` implements the sequence protocol so it is compatible with all the bread and butter methods."

[[{:numbered false}]]
(fact
  (def ov (ova (map (fn [n] {:val n})
                    (range 8))))

  (seq ov)
  => '({:val 0} {:val 1} {:val 2}
       {:val 3} {:val 4} {:val 5}
       {:val 6} {:val 7})

  (map #(update-in % [:val] inc) ov)
  => '({:val 1} {:val 2} {:val 3}
       {:val 4} {:val 5} {:val 6}
       {:val 7} {:val 8})

  (last ov)
  => {:val 7}

  (count ov)
  => 8

  (get ov 0)
  => {:val 0}

  (nth ov 3)
  => {:val 3}

  (ov 0)
  => {:val 0}

  (ov [:val] #{1 2 3}) ;; Gets the first that matches
  => {:val 1}
  )

[[:chapter {:title "Indices Selection"}]]

"There are a number of ways elements in an `ova` can be selected. The library uses custom syntax to provide a shorthand for element selection. We use the function `indices` in order to give an examples of how searches can be expressed. Most of the functions like `select`, `remove!`, `filter!`, `smap!`, `smap-indexed!`, and convenience macros are all built on top of the `indices` function and so can be used accordingly once the convention is understood."

[[:section {:title "by index"}]]
"The most straight-forward being the index itself, represented using a number."

[[{:numbered false}]]
(fact
  (def ov (ova [{:v 0, :a {:c 4}}    ;; 0
                {:v 1, :a {:d 3}}    ;; 1
                {:v 2, :b {:c 2}}    ;; 2
                {:v 3, :b {:d 1}}])) ;; 3

  (indices ov)           ;; return all indices
  => [0 1 2 3]

  (indices ov 0)         ;; return indices of the 0th element
  => [0]

  (indices ov 10)        ;; return indices of the 10th element
  => [])

[[:section {:title "by value"}]]
"A less common way is to search for indices by value."

[[{:numbered false}]]
(fact
  (indices ov            ;; return indices of elements matching term
           {:v 0 :a {:c 4}})
  => [0])


[[:section {:title "by predicate"}]]
"Most of the time, predicates are used. They allow selection of any element returning a non-nil value when evaluated against the predicate. Predicates can take the form of functions, keywords or list representation."

[[{:numbered false}]]
(fact
  (indices ov #(get % :a))   ;; retur indicies where (:a elem) is non-nil

  => [0 1]

  (indices ov #(:a %))       ;; more succint function form

  => [0 1]

  (indices ov :a)            ;; keyword form, same as #(:a %)

  => [0 1]

  (indices ov '(get :a))     ;; list form, same as #(get % :a)

  => [0 1]

  (indices ov '(:a))         ;; list form, same as #(:a %)

  => [0 1])

[[:section {:title "by sets (or)"}]]
"sets can be used to compose more complex searches by acting as an `union` operator over its members"

[[{:numbered false}]]
(fact
  (indices ov #{0 1})        ;; return indices 0 and 1
  => [0 1]

  (indices ov #{:a 2})       ;; return indices of searching for both 2 and :a
  => (just [0 1 2] :in-any-order)

  (indices ov #{'(:a)        ;; a more complex example
                #(= (:v %) 2)})
  => (just [0 1 2] :in-any-order))

[[:section {:title "by vectors (and)"}]]
"vectors can be used to combine predicates for more selective filtering of elements"

[[{:numbered false}]]
(fact
  (indices ov [:v 0])        ;; return indicies where (:a ele) = {:c 4}
  => [0]

  (indices ov [:v '(= 0)])   ;; return indicies where (:a ele) = {:c 4}
  => [0]

  (indices ov [:a #(% :c)])  ;; return indicies where (:a ele) has a :c element
  => [0]

  (indices ov [:a '(:c)])    ;; with list predicate
  => [0]

  (indices ov [:a :c])       ;; with keyword predicate
  => [0]

  (indices ov [:v odd?       ;; combining predicates
               :v '(> 1)])
  => [3]

  (indices ov #{[:a :c] 2})  ;; used within a set

  => (just [0 2] :in-any-order))


[[:section {:title "accessing nested elements"}]]
"When dealing with nested maps, a vector can be used instead of a keyword to specify rules of selection using nested elements"
(fact
  (indices ov [[:b :c] 2])   ;; with value
  => [2]

  (indices ov [[:v] '(< 3)]) ;; with predicate
  => [0 1 2]

  (indices ov [:v 2          ;; combining in vector
               [:b :c] 2])
  => [2])


[[:file {:src "test/midje_doc/ova_api.clj"}]]

[[:file {:src "test/midje_doc/ova_scoreboard_example.clj"}]]

[[:chapter {:title "End Notes"}]]

"For any feedback, requests and comments, please feel free to lodge an issue on github or contact me directly.

Chris.
"
