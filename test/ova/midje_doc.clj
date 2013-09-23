(ns ova.midje-doc
  (:require [ova.core :refer :all]
            [midje.sweet :refer :all]))

[[:chapter {:title "Motivation"}]]

"*Isn't the whole point of clojure to move to a more functional style of programming using immutable data structures?*

Yes. Exactly. Clojure has rid of alot of complexity on the jvm by requiring the programmer to think functionally. However, the issue of shared state is still a problem in multithreaded applications. Clojure has `ref`'s and `atom`'s to resolve this issue but they tend to be a little basic.

A typical use case of shared state is an array within a `ref`:

 - Elements containing data can be added or removed from an array.
 - The data within each element can change.
 - The data has to be accessible by a number of threads.

In the case above, the best option would be or to construct a `ref` containing a vector of `ref`'s containing data. `ova` is just such a datastructure.
"    

[[:chapter {:title "Installation"}]]

"Add to `project.clj`"

[[{:numbered false}]]
(comment [im.chit/ova "0.9.1"])

"All functions are in the `ova.core` namespace."

[[:chapter {:title "Creating"}]]
[[:section {:title "ova"}]]
"An `ova` deals with data in a vector."

[[{:title "Creating an ova - primitives"}]]
(def ov-pr (ova [1 2 3 4]))
(fact
(-> ov-pr type str) => "class ova.core.Ova")

"Although it can deal with primitives, an `ova` is most useful when the elements of the vector are maps."

[[{:title "Creating an ova - maps" :tag "ov-data"}]]
(fact
  (def RESULTS 
    [{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
     {:id :a2 :score 15 :name "John"  :gender :m :nationality :aus}
     {:id :a3 :score 15 :name "Dave"  :gender :m :nationality :aus}
     {:id :a4 :score 11 :name "Henry" :gender :m :nationality :usa}
     {:id :a5 :score 20 :name "Scott" :gender :m :nationality :usa}
     {:id :a6 :score 13 :name "Tom"   :gender :m :nationality :usa}
     {:id :a7 :score 15 :name "Jill"  :gender :f :nationality :aus}
     {:id :a8 :score 19 :name "Sally" :gender :f :nationality :usa}
     {:id :a9 :score 13 :name "Rose"  :gender :f :nationality :aus}])

  (def ov (ova RESULTS))
  
  (-> ov-pr type str) => "class ova.core.Ova")

[[:section {:title "persistent!"}]]
"Since `ova.core.Ova` implements the `clojure.lang.ITransientCollection` interface, it can be made persistent with `persistent!`."

(fact
  [[{:title "Make persistent"}]]
  (persistent! (ova [1 2 3 4])) 
  => [1 2 3 4]

  [[{:title "Shorthand"}]]
  (<< (ova [1 2 3 4])) => [1 2 3 4])

[[:section {:title "reinit!"}]]

"`reinit!` resets the data elements in an ova to another set of values."

(fact
  (def ov (ova [1 2 3 4]))
  (dosync (reinit! ov [5 6 7 8 9]))
  (persistent! ov)
  => [5 6 7 8 9])

[[:chapter {:title "Selecting"}]]

"Selecting of elements have been made as easy a possible"

(def ov (ova RESULTS))

[[:section {:title "select"}]]

[[:subsection {:title "using index"}]]
(fact
  (select ov 0)
  => #{{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}}
  (select ov 1)
  => #{{:id :a2 :score 15 :name "John"  :gender :m :nationality :aus}})

[[:subsection {:title "using vector"}]]
(fact
  (select ov [:id :a1])
  => #{{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}}
  (select ov [:gender :f])
  => #{{:id :a7 :score 15 :name "Jill"  :gender :f :nationality :aus}
       {:id :a8 :score 19 :name "Sally" :gender :f :nationality :usa}
       {:id :a9 :score 13 :name "Rose"  :gender :f :nationality :aus}}
  (select ov [:score 13])
  => #{{:id :a6 :score 13 :name "Tom"   :gender :m :nationality :usa}
       {:id :a9 :score 13 :name "Rose"  :gender :f :nationality :aus}}
  (select ov [:score even?])
  => #{{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
       {:id :a5 :score 20 :name "Scott" :gender :m :nationality :usa}}
       
  (select ov #(-> % :score (< 13)))
  => #{{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
      {:id :a4 :score 11 :name "Henry" :gender :m :nationality :usa}}
      
  (select ov [:score '(< 13)])
  => #{{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
       {:id :a4 :score 11 :name "Henry" :gender :m :nationality :usa}}
       
  (select ov [:score 13 :gender :f])
  => #{{:id :a9 :score 13 :name "Rose"  :gender :f :nationality :aus}}
  )

[[:subsection {:title "nested maps"}]]
(fact
  (select (ova [{:l1 {:l2 {:l3 "val"}}}])
          [[:l1 :l2 :l3] "val"])
  => #{{:l1 {:l2 {:l3 "val"}}}}

  (select (ova [{:l1 {:l2 {:l3 "val"} 
                     :flag true}}])
          #(-> % :l1 :flag))
  => #{{:l1 {:l2 {:l3 "val"}
             :flag true}}}

  (select (ova [{:l1 {:l2 {:l3 "val"} 
                     :flag true}}])
          [:l1 '(:flag)])
  => #{{:l1 {:l2 {:l3 "val"}
             :flag true}}}
  
  (select (ova [{:l1 {:l2 {:l3 "val"} 
                     :flag true}}])
          [[:l1 :flag] true])
  => #{{:l1 {:l2 {:l3 "val"}
             :flag true}}}
             
  
  (select (ova [{:l1 {:l2 {:l3 "val"} 
                     :flag true}}])
          [[:l1 :l2 :l3] "val" [:l1 :flag] true])
  => #{{:l1 {:l2 {:l3 "val"}
            :flag true}}})

[[:subsection {:title "using sets"}]]
(fact
  (select ov #{0 1})
  => #{{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
       {:id :a2 :score 15 :name "John"  :gender :m :nationality :aus}}
       
  (select ov #{[:score even?] [:score 13 :gender :f]})
  => #{{:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
       {:id :a5 :score 20 :name "Scott" :gender :m :nationality :usa}
       {:id :a9 :score 13 :name "Rose"  :gender :f :nationality :aus}})



[[:section {:title "IFn"}]]
[[:subsection {:title "using index"}]]

(fact
  (ov 0)
  => {:id :a1 :score 10 :name "Bill"  :gender :m :nationality :aus}
  (ov 1)
  => {:id :a2 :score 15 :name "John"  :gender :m :nationality :aus})

[[:subsection {:title "using ids"}]]
(fact
  (ov :a3)
  => {:id :a3 :score 15 :name "Dave"  :gender :m :nationality :aus}
  (:a3 ov)
  => {:id :a3 :score 15 :name "Dave"  :gender :m :nationality :aus})

[[:subsection {:title "not found"}]]
(fact
  (ov :a10)
  => nil)

[[:chapter {:title "Inserting"}]]
[[:section {:title "append!"}]]
(fact
  (def ov (ova [1 2 3 4]))
  (dosync (append! ov 5 6 7 8)) 
  (<< ov) => [1 2 3 4 5 6 7 8])
  
[[:section {:title "concat!"}]]
(fact
  (def ov (ova [1 2 3 4]))
  (dosync (concat! ov [5 6 7 8])) 
  (<< ov) => [1 2 3 4 5 6 7 8])


[[:section {:title "insert!"}]]
(fact
  (def ov (ova [:a :b :c :e :f]))
  (dosync (insert! ov :d 3)) 
  (<< ov) => [:a :b :c :d :e :f])


[[:chapter {:title "Deleting"}]]
[[:section {:title "empty!"}]]
(fact
  (def ov (ova [:a :b :c :d]))
  (dosync (empty! ov)) 
  (<< ov) => [])


[[:section {:title "remove!"}]]
(fact
  (def ov (ova [:a :b :c :d]))
  (dosync (remove! ov '(= :a))) 
  (<< ov) => [:b :c :d])

(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (remove! ov #{'(< 3) '(> 6)})) 
  (<< ov) => [3 4 5 6])

[[:section {:title "filter!"}]]
(fact
  (def ov (ova [:a :b :c :d]))
  (dosync (filter! ov '(= :a))) 
  (<< ov) => [:a])

(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (filter! ov #{'(< 3) '(> 6)})) 
  (<< ov) => [1 2 7 8 9])
  
[[:chapter {:title "Sorting"}]]
[[:section {:title "sort!"}]]
(fact
  (def ov (ova [9 8 7 6 5 4 3 2 1]))
  (dosync (sort! ov <)) 
  (<< ov) => [1 2 3 4 5 6 7 8 9])

(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (sort! ov >)) 
  (<< ov) => [9 8 7 6 5 4 3 2 1])

[[:section {:title "reverse!"}]]
(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (reverse! ov)) 
  (<< ov) => [9 8 7 6 5 4 3 2 1])

[[:chapter {:title "Updating"}]]
[[:section {:title "!!"}]]
(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (!! ov 0 0)) 
  (<< ov) => [0 2 3 4 5 6 7 8 9])

(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (!! ov odd? 0)) 
  (<< ov) => [0 2 0 4 0 6 0 8 0])

(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (!! ov '(> 4) 0)) 
  (<< ov) => [1 2 3 4 0 0 0 0 0])

[[:section {:title "map!"}]]
(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (map! ov inc)) 
  (<< ov) => [2 3 4 5 6 7 8 9 10])


[[:section {:title "smap!"}]]
(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (smap! ov odd? inc)) 
  (<< ov) => [2 2 4 4 6 6 8 8 10])

(fact
  (def ov (ova [1 2 3 4 5 6 7 8 9]))
  (dosync (!> ov odd? inc)) 
  (<< ov) => [2 2 4 4 6 6 8 8 10])

[[:chapter {:title "Watch for Changes"}]]
[[:section {:title "add-elem-watch"}]]
(fact 
  (def ov     (ova [1 2 3 4]))
  (def watch  (atom []))
  (def cj-fn  (fn  [o r k p v]  ;; ova, ref, key, prev, current
                (swap! watch conj [p v]))) 
  
  [[{:title "see that watches are set"}]]
  (add-elem-watch ov :conj cj-fn)
  (keys (get-elem-watches ov))
  => [:conj]

  [[{:title "elements in ov are manipulated"}]]
  (dosync (map! ov + 10))
  (<< ov) => [11 12 13 14]

  [[{:title "watch is also changed"}]]  
  (sort @watch) => [[1 11] [2 12] [3 13] [4 14]])

[[:section {:title "remove-elem-watch"}]]
(fact   
  (remove-elem-watch ov :conj)
  (keys (get-elem-watches ov))
  => nil)

