(ns bucks.domain
  (:require [cljs.spec.alpha :as s]
            [cljs-time.core :as time]
            [cljs-time.coerce :as time.coerce]))


;;;; DEFAULTS

(def asset-types #{"TFSA" "RA" "Crypto" "Savings" "CFD" "ETF" "UnitTrust"})
(def yes "y")
(def no "n")


;;;; PREDICATES

(defn not-empty-string? [s] (and (string? s)
                                 (not-empty s)))


(defn year? [n] (boolean (and (number? n) (>= n 1900) (> 3000 n))))


(defn month? [n] (boolean (and (number? n) (>= n 1) (> 13 n))))


(defn day? [n] (boolean (and (number? n) (>= n 1) (> 32 n))))


(defn percentage? [n] (boolean (and (number? n) (>= n 0) (>= 100 n))))


;;;; SPECS

(s/def :d/not-empty-string not-empty-string?)

(s/def :d/name :d/not-empty-string)

(s/def :d/year year?)

(s/def :d/month month?)

(s/def :d/day day?)

(s/def :d/amount number?)

(s/def :d/value number?)

(s/def :d/age pos?)

(s/def :d/percentage percentage?)

(s/def :d/units number?)

(s/def :d/wealth-index pos?)

(s/def :d/asset-type #(contains? asset-types %))

(s/def :d/exclude-from-net #(contains? #{yes no} %))


;;;; DATA TYPES

(def data-types-config
  [["date-of-birth"
    [:d/year :d/month :d/day]
    "Your date of birth to use in wealth index calculations."]
   ["salary"
    [:d/name :d/year :d/month :d/day :d/amount]
    "A Salary Change. Name is the Name Of Employer. Amount the Monthly Salary Before Tax."]
   ["open-asset"
    [:d/name :d/year :d/month :d/day :d/asset-type :d/value :d/units :d/exclude-from-net]
    (str "A new Asset. Name is the name of the asset. Asset-type should be one of the following " asset-types
         ". Requires opening value and units. Exclude from Net indicates if an asset should be excluded for the wealth index and total asset value calculations (can be y or n). Asset value is defaulted to 0 and an initial transaction can be added using 'transaction'.")]
   ["close-asset"
    [:d/name :d/year :d/month :d/day :d/value]
    "Close an existing asset. Name is the name of the asset. Value is the value of the asset before it closed"]
   ["transaction"
    [:d/name :d/year :d/month :d/day :d/amount :d/value :d/units]
    "An Asset Deposit or Withdrawal. Name is the Asset Name. Amount is the Amount of the deposit or Withdrawal. Value is the value of the asset After the transaction was made. Units are the amount of units transacted if applicable (like x bitcoin)"]
   ["value"
    [:d/name :d/year :d/month :d/day :d/value]
    "The Value of an asset at a certain point in time. Used for tracking asset growth ideally on a monthly or weekly basis. Name is the Asset Name. Value is the value of the asset at that point in time."]
   ["wi-goal"
    [:d/wealth-index :d/age]
    "A goal wealth index to reach at an age"]
   ["year-goal"
    [:d/year :d/name :d/percentage]
    "A growth percentage goal for a given year"]])


(def data-types (->> data-types-config
                     (map (juxt first second))
                     (into {})))


;;;; Query

(defn type-of? [data-type m]
  (= data-type (:data-type m)))


(defn type-of-f? [data-type] (partial type-of? data-type))


(defn types-of-f? [& types]
  (fn [m] (->> types
               (map #(type-of? % m))
               (filter true?)
               not-empty)))


(defn timestamped [{:keys [year month day] :as m}]
  (let [date (js/Date. year month day)]
    (assoc m
           :date date
           :timestamp (.getTime date))))


(defn wrap-age [{:keys [timestamp] :as birthday}]
  (assoc birthday
         :age (time/in-years (time/interval
                              (time.coerce/from-long timestamp)
                              (time/now)))))


(defn birthday [coll]
  (->> coll
       (filter (type-of-f? :date-of-birth))
       first
       (#(or % {:year 1970 :month 1 :day 1}))
       timestamped
       wrap-age))


(defn salaries [coll]
  (->> coll
       (filter (type-of-f? :salary))
       (map timestamped)
       (sort-by :timestamp)))


(defn wi-goals [coll]
  (->> coll
       (filter (type-of-f? :wi-goal))))


(defn year-goals [coll]
  (->> coll
       (filter (type-of-f? :year-goal))
       (sort-by :year)
       reverse))



(defn assets [coll]
  (->> coll
       (filter (types-of-f? :open-asset :transaction :value :close-asset))
       (group-by :name)))