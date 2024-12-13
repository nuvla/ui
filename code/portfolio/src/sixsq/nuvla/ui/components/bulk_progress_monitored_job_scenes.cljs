(ns sixsq.nuvla.ui.components.bulk-progress-monitored-job-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.bulk-progress :refer [MonitoredJobDetail2]]))

(def job-with-async {:started "2024-12-03T10:44:48.701Z",
                     :tags ["/job/entries/entry-999-0000185258"],
                     :execution-mode "push",
                     :payload
                     "{\"dg-owner-authn-info\":{\"claims\":[\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"group\\/nuvla-anon\",\"group\\/nuvla-user\"],\"user-id\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"active-claim\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\"},\"dg-authn-info\":{\"claims\":[\"group\\/nuvla-anon\",\"group\\/nuvla-user\",\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\"],\"user-id\":\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\",\"active-claim\":\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\"},\"authn-info\":{\"user-id\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"active-claim\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"claims\":[\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"group\\/nuvla-anon\",\"session\\/4f6210de-ef34-4173-85e4-8fe9df922142\",\"group\\/nuvla-user\"]}}",
                     :updated "2024-12-03T10:45:20.003Z",
                     :status-message
                     "{\"ACTIONS_COUNT\": 21, \"SUCCESS_COUNT\": 1, \"FAILED_COUNT\": 3, \"SKIPPED_COUNT\": 14, \"QUEUED_COUNT\": 2, \"RUNNING_COUNT\": 1, \"SUCCESS\": [\"deployment/s\"], \"QUEUED\": [\"deployment/y\", \"deployment/z\"], \"RUNNING\": [\"deployment/x\"], \"JOBS_COUNT\": 4, \"SKIP_REASONS\": [{\"COUNT\": 14, \"CATEGORY\": \"Offline Edges\", \"IDS\": [{\"id\": \"nuvlabox/beta\", \"name\": \"ne-beta\", \"COUNT\": 12}, {\"id\": \"nuvlabox/alpha\", \"name\": \"ne-alpha\", \"COUNT\": 2}]}], \"FAIL_REASONS\": [{\"CATEGORY\": \"Error category foobar\", \"COUNT\": 3, \"IDS\": [{\"id\": \"deployment/abc\", \"name\": \"ne-alpha\", \"COUNT\": 3}]}]}",
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
                     [{:href "deployment-set/73277c43-7d85-43ca-8ec2-1e7eaccc297c"}]}
  )

(def job-sync {:started "2024-12-06T09:18:01.221Z",
               :tags ["/job/entries/entry-999-0000185731"],
               :execution-mode "push",
               :payload
               "{\"dg-owner-authn-info\":{\"claims\":[\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"group\\/nuvla-anon\",\"group\\/nuvla-user\"],\"user-id\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\",\"active-claim\":\"user\\/b9b4b6c4-61c4-406a-b5c5-e77703800131\"},\"dg-authn-info\":{\"claims\":[\"group\\/nuvla-anon\",\"group\\/nuvla-user\",\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\"],\"user-id\":\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\",\"active-claim\":\"deployment-set\\/73277c43-7d85-43ca-8ec2-1e7eaccc297c\"},\"authn-info\":{\"user-id\":\"group\\/nuvla-admin\",\"active-claim\":\"group\\/nuvla-admin\",\"claims\":[\"group\\/nuvla-admin\",\"group\\/nuvla-anon\",\"session\\/337357f0-dcd3-407b-8622-44949d6a13c6\",\"group\\/nuvla-user\"]}}",
               :updated "2024-12-06T09:18:01.971Z",
               :status-message "{\"ACTIONS_COUNT\": 21, \"SUCCESS_COUNT\": 1, \"FAILED_COUNT\": 3, \"SKIPPED_COUNT\": 14, \"QUEUED_COUNT\": 2, \"RUNNING_COUNT\": 1, \"SUCCESS\": [\"deployment/s\"], \"QUEUED\": [\"deployment/y\", \"deployment/z\"], \"RUNNING\": [\"deployment/x\"], \"JOBS_COUNT\": 4, \"SKIP_REASONS\": [{\"COUNT\": 14, \"CATEGORY\": \"Offline Edges\", \"IDS\": [{\"id\": \"nuvlabox/beta\", \"name\": \"ne-beta\", \"COUNT\": 12}, {\"id\": \"nuvlabox/alpha\", \"name\": \"ne-alpha\", \"COUNT\": 2}]}], \"FAIL_REASONS\": [{\"CATEGORY\": \"Error category foobar\", \"COUNT\": 3, \"IDS\": [{\"id\": \"deployment/abc\", \"name\": \"ne-alpha\", \"COUNT\": 3}]}]}"
               :created "2024-12-06T09:18:01.106Z",
               :duration 0,
               :state "SUCCESS",
               :return-code 0,
               :updated-by "group/nuvla-admin",
               :created-by "group/nuvla-admin",
               :id "job/30b522c9-39bf-4831-acea-956f740aca32",
               :resource-type "job",
               :acl
               {:view-acl ["user/b9b4b6c4-61c4-406a-b5c5-e77703800131"],
                :view-meta ["user/b9b4b6c4-61c4-406a-b5c5-e77703800131"],
                :view-data ["user/b9b4b6c4-61c4-406a-b5c5-e77703800131"],
                :manage ["user/b9b4b6c4-61c4-406a-b5c5-e77703800131"],
                :owners ["group/nuvla-admin"]},
               :action "bulk_deployment_set_update",
               :version 2,
               :progress 100,
               :target-resource
               {:href "deployment-set/73277c43-7d85-43ca-8ec2-1e7eaccc297c"},
               :time-of-status-change "2024-12-06T09:18:01.971Z",
               :affected-resources
               [{:href "deployment-set/73277c43-7d85-43ca-8ec2-1e7eaccc297c"}]})

(defscene monitored-job-without-progress
  [MonitoredJobDetail2
   job-with-async
   :with-progress? true])
