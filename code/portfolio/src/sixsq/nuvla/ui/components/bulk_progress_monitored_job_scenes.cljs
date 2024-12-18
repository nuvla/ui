(ns sixsq.nuvla.ui.components.bulk-progress-monitored-job-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.bulk-progress :as bp]))

(def job-with-async {:started "2024-12-03T10:44:48.701Z",
                     :tags ["/job/entries/entry-999-0000185258"],
                     :execution-mode "push",
                     :payload
                     "{\"dg-owner-authn-info\":{\"claims\":[\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"group\\/nuvla-anon\",\"group\\/nuvla-user\"],\"user-id\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"active-claim\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\"},\"dg-authn-info\":{\"claims\":[\"group\\/nuvla-anon\",\"group\\/nuvla-user\",\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\"],\"user-id\":\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\",\"active-claim\":\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\"},\"authn-info\":{\"user-id\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"active-claim\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"claims\":[\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"group\\/nuvla-anon\",\"session\\/4f6210de-ef34-4173-85e4-8fe9df922142\",\"group\\/nuvla-user\"]}}",
                     :updated "2024-12-03T10:45:20.003Z",
                     :status-message
                     "{\"total_actions\": 21, \"success_count\": 1, \"failed_count\": 3, \"skipped_count\": 14, \"queued_count\": 2, \"running_count\": 1, \"success\": [\"deployment/s\"], \"queued\": [\"deployment/y\", \"deployment/z\"], \"running\": [\"deployment/x\"], \"jobs_count\": 4, \"error_reasons\": [{\"count\": 14, \"reason\": \"Offline Edges\", \"category\": \"skipped\", \"data\": [{\"id\": \"nuvlabox/beta\", \"name\": \"ne-beta\", \"count\": 12}, {\"id\": \"nuvlabox/alpha\", \"name\": \"ne-alpha\", \"count\": 2}]}, {\"reason\": \"Error reason foobar\", \"count\": 3, \"category\": \"failed\", \"data\": [{\"id\": \"deployment/abc\", \"name\": \"ne-alpha\", \"count\": 3}]}]}",
                     :created "2024-12-03T10:44:48.642Z",
                     :duration 31,
                     :state "SUCCESS",
                     :return-code 0,
                     :updated-by "group/nuvla-admin",
                     :created-by "user/b9b4b6c4-61c4-406a-b5c5-e77703800131",
                     :id "job/01f5b8eb-cef7-4e7f-bd54-f7c63c6609ed",
                     :resource-type "job",
                     :acl
                     {:view-acl ["user/b9b4b6c4-61c4-406a-b5c5-e77703800131"],
                      :view-meta ["user/b9b4b6c4-61c4-406a-b5c5-e77703800131"],
                      :view-data ["user/b9b4b6c4-61c4-406a-b5c5-e77703800131"],
                      :owners ["group/nuvla-admin"]},
                     :action "bulk_deployment_set_update",
                     :version 2,
                     :progress 100,
                     :target-resource
                     {:href "deployment-set/73277c43-7d85-43ca-8ec2-1e7eaccc297c"},
                     :time-of-status-change "2024-12-03T10:45:20.003Z",
                     :affected-resources
                     [{:href "deployment-set/73277c43-7d85-43ca-8ec2-1e7eaccc297c"}]})

(defscene monitored-job-without-progress
  [bp/JobDetail (bp/append-parsed-job-status-message job-with-async)])
