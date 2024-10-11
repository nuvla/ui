(ns sixsq.nuvla.ui.components.job-cell-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.job.views :refer [JobCell]]))

(defscene simple
          [:<>
           [JobCell {:status-message "hello-world"}]
           ])

(defscene coe
          [:<>
           [JobCell {:action         "coe_resource_actions"
                     :status-message "status message that is not json should be rendered as default"}]
           [JobCell {:status-message
                     "{\n    \"docker\": [\n        {\n            \"success\": false,\n            \"return-code\": 403,\n            \"content\": \"403 Client Error for http+docker://localhost/v1.42/networks/085f8229d6dd9b9e7e99513bfd6c20f35c75057382f6d5c119e1d29a1172c667: Forbidden (\\\"none is a pre-defined network and cannot be removed\\\")\",\n            \"message\": \"Operation not supported for network -- 085f8229d6dd9b9e7e99513bfd6c20f35c75057382f6d5c119e1d29a1172c667\"\n        },\n        {\n            \"success\": false,\n            \"return-code\": 403,\n            \"content\": \"403 Client Error for http+docker://localhost/v1.42/networks/cd4adbc05d7daa22be710a7c2e43a7242d9e212a5be66fb917261dd387911f5f: Forbidden (\\\"host is a pre-defined network and cannot be removed\\\")\",\n            \"message\": \"Operation not supported for network -- cd4adbc05d7daa22be710a7c2e43a7242d9e212a5be66fb917261dd387911f5f\"\n        },\n        {\n            \"success\": false,\n            \"return-code\": 400,\n            \"content\": \"400 Client Error for http+docker://localhost/v1.42/networks/maxhxtw6fs8lvji11gtvhml3y: Bad Request (\\\"rpc error: code = FailedPrecondition desc = ingress network cannot be removed because service 1hnbk87zpcbc3754qfhksa5iq depends on it\\\")\",\n            \"message\": \"Unknown error: {\\\"message\\\":\\\"rpc error: code = FailedPrecondition desc = ingress network cannot be removed because service 1hnbk87zpcbc3754qfhksa5iq depends on it\\\"}\\n\"\n        },\n        {\n            \"success\": false,\n            \"return-code\": 403,\n            \"content\": \"403 Client Error for http+docker://localhost/v1.42/networks/cb0f0eff592ea8f6cafa11e31e3c824c2e2ec4d22fc91511090bf59b82f5a418: Forbidden (\\\"error while removing network: network docker_gwbridge id cb0f0eff592ea8f6cafa11e31e3c824c2e2ec4d22fc91511090bf59b82f5a418 has active endpoints\\\")\",\n            \"message\": \"Operation not supported for network -- cb0f0eff592ea8f6cafa11e31e3c824c2e2ec4d22fc91511090bf59b82f5a418\"\n        },\n        {\n            \"success\": false,\n            \"return-code\": 403,\n            \"content\": \"403 Client Error for http+docker://localhost/v1.42/networks/95eabb9db13dde00285c0f384b0e8e12a796b69ef3ef7ae7f5ccbd1f68b4908f: Forbidden (\\\"bridge is a pre-defined network and cannot be removed\\\")\",\n            \"message\": \"Operation not supported for network -- 95eabb9db13dde00285c0f384b0e8e12a796b69ef3ef7ae7f5ccbd1f68b4908f\"\n        },\n        {\n            \"success\": false,\n            \"return-code\": 400,\n            \"content\": \"400 Client Error for http+docker://localhost/v1.42/networks/r5ka10cb2l7qm9sd9815ih9ke: Bad Request (\\\"rpc error: code = FailedPrecondition desc = network r5ka10cb2l7qm9sd9815ih9ke is in use by service 1d7qsrxflszhgv3pq5maa3a62\\\")\",\n            \"message\": \"Unknown error: {\\\"message\\\":\\\"rpc error: code = FailedPrecondition desc = network r5ka10cb2l7qm9sd9815ih9ke is in use by service 1d7qsrxflszhgv3pq5maa3a62\\\"}\\n\"\n        }\n    ]\n}",
                     :id     "job/e4986f52-dadb-4ecf-8119-6de5a978cdf5"
                     :action "coe_resource_actions"}]
           [JobCell {:status-message
                     "{\n    \"docker\": [\n        {\n            \"success\": true,\n            \"return-code\": 200,\n            \"message\": \"Image hello-world:latest was already present and updated\"\n        }\n    ]\n}",
                     :id     "job/e4986f52-dadb-4ecf-8119-6de5a978cdf5"
                     :action "coe_resource_actions"}]])
