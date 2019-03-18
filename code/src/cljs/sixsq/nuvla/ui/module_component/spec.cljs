(ns sixsq.nuvla.ui.module-component.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(def defaults {::port-mappings       {1 {}}                 ; create an initial entry for new components
               ::volumes             {1 {}}                 ; create an initial entry for new components
               })

(s/def ::port-mappings any?)
(s/def ::volumes any?)

;{
; "description": "Jupyter notebook application integrated with Nuvla data management",
;              "path": "my-top-project/my-project/my-moduleXXX",
; "content": {
;             "architecture": "x86",
;                           "updated": "2019-03-15T08:36:58.039Z",
;             "created": "2019-03-15T08:36:58.039Z",
;                           "author": "meb",
;             "id": "module-component/0c0c5729-39b2-43c9-9608-b860820785d6",
;                           "commit": "initial commit",
;             "image": "sixsq/gssc-jupyter:latest"
;             },
;              "updated": "2019-03-15T08:36:58.072Z",
; "name": "my-moduleXXX",
;              "type": "COMPONENT",
; "created": "2019-03-15T08:36:58.072Z",
;              "parent-path": "my-top-project/my-project",
; "data-accept-content-types": [
;                               "application/x-hdr",
;                               "application/x-clk"
;                               ],
; "id": "module/b67aee64-e3ab-4f59-a0e4-f52ec48b8421",
; "resource-type": "module",
; "acl": {
;         "owner": {
;                   "principal": "ADMIN",
;                              "type": "ROLE"
;                   },
;                "rules": [
;                          {
;                           "type": "ROLE",
;                                 "principal": "ADMIN",
;                           "right": "ALL"
;                           }
;                          ]
;         },
; "operations": [
;                {
;                 "rel": "edit",
;                      "href": "module/b67aee64-e3ab-4f59-a0e4-f52ec48b8421"
;                 },
;                {
;                 "rel": "delete",
;                      "href": "module/b67aee64-e3ab-4f59-a0e4-f52ec48b8421"
;                 }
;                ],
; "versions": [
;              {
;               "href": "module-component/0c0c5729-39b2-43c9-9608-b860820785d6",
;                     "author": "meb",
;               "commit": "initial commit"
;               }
;              ]
; }
