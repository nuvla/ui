(ns sixsq.slipstream.webui.dashboard.views-vms
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.slipstream.webui.client.subs :as client-subs]
    [sixsq.slipstream.webui.dashboard.events :as dashboard-events]
    [sixsq.slipstream.webui.dashboard.subs :as dashboard-subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))


(defn table-vm-row
  [vm]
  (let [slipstream-url (subscribe [::client-subs/slipstream-url])]
    (fn [{:keys [deployment-href state ip vcpu ram disk instance-type instance-id connector-href user-href] :as vm}]
      [ui/TableRow {:error (empty? deployment-href)}
       [ui/TableCell {:collapsing true}
        (when (empty? deployment-href)
          [ui/Popup {:trigger  (r/as-element [:div [ui/Icon {:name "exclamation circle"}] "Unknown"])
                     :inverted true
                     :size     "mini" :content "Deployment UUID unknown" :position "left center"}])
        [:a {:href (str @slipstream-url "/" deployment-href)} (or (-> deployment-href
                                                                      (str/replace #"^run/" "")
                                                                      (str/split #"-")
                                                                      (first)) "")]]
       [ui/TableCell {:collapsing true} state]
       [ui/TableCell {:collapsing true} ip]
       [ui/TableCell {:collapsing true :textAlign "center"} vcpu]
       [ui/TableCell {:collapsing true :textAlign "center"} ram]
       [ui/TableCell {:collapsing true :textAlign "center"} disk]
       [ui/TableCell
        {:collapsing true :style {:max-width "50px" :overflow "hidden" :text-overflow "ellipsis"}} instance-type]
       [ui/TableCell
        {:collapsing true :style {:max-width "150px" :overflow "hidden" :text-overflow "ellipsis"}} instance-id]
       [ui/TableCell {:collapsing true :style {:max-width "150px" :overflow "hidden" :text-overflow "ellipsis"}}
        (or (str/replace connector-href #"^connector/" "") "")]
       [ui/TableCell {:style {:max-width "250px" :overflow "hidden" :text-overflow "ellipsis"}}
        [:a {:href (str @slipstream-url "/" user-href)} (str/replace user-href #"^user/" "")]]])))


(defn extract-vms-data
  [vms-response]
  (let [vms (:virtualMachines vms-response)]
    (map (fn [{:keys [ip state instanceID deployment serviceOffer connector]}]
           {:deployment-href (get deployment :href "")
            :state           (or state "")
            :ip              (or ip "")
            :vcpu            (get serviceOffer :resource:vcpu "")
            :ram             (get serviceOffer :resource:ram "")
            :disk            (get serviceOffer :resource:disk "")
            :instance-type   (get serviceOffer :resource:instanceType "")
            :instance-id     (or instanceID "")
            :connector-href  (get connector :href "")
            :user-href       (get-in deployment [:user :href] "")}) vms)))


(defn vms-table
  []
  (let [tr (subscribe [::i18n-subs/tr])
        virtual-machines (subscribe [::dashboard-subs/virtual-machines])
        headers ["ID" "State" "IP" "CPU" "RAM [MB]" "DISK [GB]" "Instance type" "Cloud Instance ID" "Cloud" "Owner"]
        page (subscribe [::dashboard-subs/page])
        total-pages (subscribe [::dashboard-subs/total-pages])
        loading? (subscribe [::dashboard-subs/loading-tab?])
        set-page #(dispatch [::dashboard-events/set-page %])]
    (fn []
      (let [vms-count (get @virtual-machines :count 0)
            vms-data (extract-vms-data @virtual-machines)]
        [ui/Segment {:basic true :loading @loading?}
         [ui/Table
          {:compact     "very"
           :size        "small"
           :selectable  true
           :unstackable true
           :celled      false
           :single-line true
           :collapsing  false
           :padded      false}

          [ui/TableHeader
           (vec (concat [ui/TableRow]
                        (mapv (fn [label] ^{:key label} [ui/TableHeaderCell label]) headers)))]


          (vec (concat [ui/TableBody]
                       (mapv (fn [{:keys [instance-id connector-href] :as vm}]
                               ^{:key (str connector-href "/" instance-id)}
                               [table-vm-row vm]) vms-data)))

          [ui/TableFooter
           [ui/TableRow
            [ui/TableHeaderCell {:col-span (str 3)}
             [ui/Label (@tr [:total]) [ui/LabelDetail vms-count]]]
            [ui/TableHeaderCell {:textAlign "right"
                                 :col-span  (str (- (count headers) 3))}
             [uix/Pagination
              {:size         "tiny"
               :totalPages   @total-pages
               :activePage   @page
               :onPageChange (ui-callback/callback :activePage set-page)}]]]]]]))))
