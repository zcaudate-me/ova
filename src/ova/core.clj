;; ## Stateful Arrays
;;
;; ova provides an array structure that allows for
;; state manipulation and watches over the entire
;; array as well as

(ns ova.core
  (:require [clojure.string :as s]
            [clojure.set :as set]
            [hara.common.error :refer [suppress]]
            [hara.common.fn :refer [get-> pcheck-> check-> suppress-pcheck]]
            [hara.state :refer [hash-keyword]]))

(defprotocol OvaProtocol
  (empty! [ova])
  (get-ref [ova])
  (clear-watches [ova])
  (add-elem-watch [ova k f])
  (remove-elem-watch [ova k])
  (get-elem-watches [ova])
  (clear-elem-watches [ova])
  (get-filtered [ova k sel nv]))

(defn- ova-state []
  {::data      (ref [])
   ::watches   (atom {})})

(defn- make-iwatch [ova]
  (fn [k & args]
    (doseq [w (get-elem-watches ova)]
      (let [wk (first w)
            wf (second w)]
        (apply wf wk ova args)))))

(defn- add-iwatch [ova irf]
  (let [k  (hash-keyword ova)
        f  (make-iwatch ova)]
    (add-watch irf k f)))

(defn- remove-iwatch [ova irf]
  (let [k (hash-keyword ova)]
    (remove-watch irf k)))

(deftype Ova [state]
  OvaProtocol
  (empty! [ova]
    (for [rf @ova]
      (remove-iwatch ova rf))
    (ref-set (::data state) [])
    ova)

  (get-ref [ova]
    (::data state))

  (clear-watches [ova]
    (doseq [[k _] (.getWatches ova)]
      (remove-watch ova k)))

  (add-elem-watch [ova k f]
    (swap! (::watches state) assoc k f))

  (remove-elem-watch [ova k]
    (swap! (::watches state) dissoc k))

  (get-elem-watches [ova]
    (deref (::watches state)))

  (clear-elem-watches [ova]
    (reset! (::watches state) {}))

  (get-filtered [ova k sel nv]
    (cond (and (nil? sel) (integer? k))
          (nth ova k nv)

          :else
          (let [res (->> (map deref @ova)
                         (filter (fn [m] (check-> m (or sel :id) k)))
                         first)]
            (or res nv))))

  clojure.lang.IDeref
  (deref [ova] @(::data state))

  clojure.lang.IRef
  (setValidator [ova vf]
    (.setValidator (::data state) vf))

  (getValidator [ova]
    (.getValidator (::data state)))

  (getWatches [ova]
    (.getWatches (::data state)))

  (addWatch [ova key callback]
    (add-watch (::data state) key callback))

  (removeWatch [ova key]
    (remove-watch (::data state) key))

  clojure.lang.ITransientCollection
  (conj [ova v]
    (let [ev (ref v)]
      (add-iwatch ova ev)
      (alter (::data state) conj ev))
    ova)

  (persistent [ova]
    (mapv deref @(::data state)))

  clojure.lang.ITransientAssociative
  (assoc [ova k v]
    (if-let [pv (get @ova k)]
      (ref-set pv v)
      (let [ev (ref v)]
        (add-iwatch ova ev)
        (alter (::data state) assoc k ev)))
    ova)

  clojure.lang.ITransientVector
  (assocN [ova i v] (assoc ova i v))

  (pop [ova]
    (if-let [lv (last @ova)]
      (remove-iwatch ova lv))
    (alter (::data state) pop)
    ova)

  clojure.lang.ILookup
  (valAt [ova k]
    (get-filtered ova k nil nil))

  (valAt [ova k not-found]
    (get-filtered ova k nil not-found))

  clojure.lang.Indexed
  (nth [ova i]
    (nth ova i nil))

  (nth [ova i not-found]
     (if-let [entry (nth @ova i)]
       @entry not-found))

  clojure.lang.Counted
  (count [ova] (count @ova))

  clojure.lang.Seqable
  (seq [ova]
    (let [res (map deref (seq @ova))]
      (if-not (empty? res) res)))

  clojure.lang.IFn
  (invoke [ova k] (get ova k))
  (invoke [ova sel k] (get-filtered ova k sel nil))

  java.lang.Object
  (toString [ova]
    (str (persistent! ova))))

(defmethod print-method
  Ova
  [ova w]
  (print-method
   (let [hash (.hashCode ova)
         contents (->>  @ova
                        (mapv #(-> % deref)))]
     (format "<Ova@%s %s>"
             hash contents)) w))

(defn concat! [ova es]
  (doseq [e es] (conj! ova e))
  ova)

(defn append! [ova & es] (concat! ova es))

(defn refresh!
  ([ova coll]
     (empty! ova)
     (concat! ova coll)))

(defn reinit!
  ([ova]
     (empty! ova)
     (clear-watches ova)
     (clear-elem-watches ova)
     ova)
  ([ova coll]
     (reinit! ova)
     (concat! ova coll)
     ova))

(defn ova
  ([] (Ova. (ova-state)))
  ([coll]
     (let [ova (Ova. (ova-state))]
       (dosync (concat! ova coll))
       ova)))

(defn- make-elem-change-watch [sel f]
  (fn [k ov rf p n]
    (let [pv (get-> p sel)
          nv (get-> n sel)]
      (cond (and (nil? pv) (nil? nv))
            nil

            (or (nil? pv) (nil? nv)
                (not (= pv nv)))
            (f k ov rf pv nv)))))

(defn add-elem-change-watch [ov k sel f]
  (add-elem-watch ov k (make-elem-change-watch sel f)))

(defn indices
  ([ova] (-> (count ova) range vec))
  ([ova pchk]
    (cond
   (number? pchk)
   (if (suppress (get ova pchk)) (list pchk) ())

   (set? pchk)
   (mapcat #(indices ova %) pchk)

   :else
   (filter (comp not nil?)
                (map-indexed (fn [i obj]
                               (suppress-pcheck obj pchk i))
                             ova)))))

(defn selectv
  ([ova]
      (persistent! ova))
  ([ova pchk]
    (cond (number? pchk)
          (if-let [val (suppress (get ova pchk))]
            (list val) ())

          (set? pchk) (mapcat #(selectv ova %) pchk)

          :else (filter
                 (fn [obj] (suppress-pcheck obj pchk obj))
                 ova))))

(defn select
  ([ova] (set (selectv ova)))
  ([ova pchk]
     (set (selectv ova pchk))))

(defn has? [ova pchk]
  (-> (select ova pchk) empty? not))

(defn map! [ova f & args]
  (doseq [evm @ova]
    (apply alter evm f args))
  ova)

(defn map-indexed! [ova f]
  (doseq [i (range (count ova))]
    (alter (@ova i) #(f i %) ))
  ova)

(defn smap! [ova pchk f & args]
  (let [idx (indices ova pchk)]
    (doseq [i idx]
      (apply alter (@ova i) f args)))
  ova)

(defn smap-indexed! [ova pchk f]
  (let [idx (indices ova pchk)]
    (doseq [i idx]
      (alter (@ova i) #(f i %))))
  ova)

(defn insert-fn [v val & [i]]
  (if (nil? i)
    (conj v val)
    (vec (clojure.core/concat (conj (subvec v 0 i) val)
                              (subvec v i)))))

(defn insert! [ova val & [i]]
  (let [evm (ref val)]
    (add-iwatch ova evm)
    (alter (get-ref ova) insert-fn evm i))
  ova)

(defn sort!
  ([ova] (sort! ova compare))
  ([ova comp]
     (alter (get-ref ova)
            #(sort (fn [x y]
                     (comp @x @y)) %))
     ova)
  ([ova sel comp]
     (alter (get-ref ova)
            #(sort (fn [x y]
                     (comp (get-> @x sel) (get-> @y sel))) %))
     ova))


(defn reverse! [ova]
  (alter (get-ref ova) reverse)
  ova)

(defn- delete-iwatches [ova idx]
  (map-indexed (fn [i obj]
                 (if-not (idx i)
                   obj
                   (do (remove-iwatch ova obj) obj)))
               @ova)
  ova)

(defn- delete-iobjs [ova indices]
  (->> ova
       (map-indexed (fn [i obj] (if-not (indices i) obj)))
       (filter (comp not nil?))
       vec))

(defn delete-indices [ova idx]
  (delete-iwatches ova idx)
  (alter (get-ref ova) delete-iobjs idx)
  ova)

(defn remove! [ova pchk]
  (let [idx (set (indices ova pchk))]
    (delete-indices ova idx))
  ova)

(defn filter! [ova pchk]
  (let [idx (set/difference
             (set (range (count ova)))
             (set (indices ova pchk)))]
    (delete-indices ova idx))
  ova)

(defn !! [ova pchk val]
  (smap! ova pchk (constantly val)))

(defmacro << [& forms]
  `(let [out# (dosync ~@forms)]
     (persistent! out#)))

(defmacro !>
  [ova pchk & forms]
  `(smap! ~ova ~pchk
          #(-> % ~@forms)))

(comment
  (<< (def ov (ova [1 2 3 4 5]))
    (append! ov 6 7 8 9))
  (def ov (ova [{}]))

  (dosync (!>> ov 0
               (assoc-in [:a :b] 1)
               (update-in [:a :b] inc)
               (assoc :c 3))
          )

  (<< (!> ov 0 assoc :d 1))
)
