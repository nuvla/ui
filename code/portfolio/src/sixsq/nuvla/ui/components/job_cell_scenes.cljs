(ns sixsq.nuvla.ui.components.job-cell-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.job.views :refer [JobCell]]))

(defscene basic
          [JobCell {:status-message "hello-world"}])

(defscene basic-long-status-message
          [JobCell {:status-message "Exception-Traceback (most recent call last):\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/executor/executor.py\", line 57, in process_job\n    return_code = action_instance.do_work()\n                  ^^^^^^^^^^^^^^^^^^^^^^^^^\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/actions/deployment_state.py\", line 59, in do_work\n    raise ex\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/actions/deployment_state.py\", line 54, in do_work\n    self.get_application_state()\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/actions/deployment_state.py\", line 34, in get_application_state\n    services = connector.get_services(Deployment.uuid(self.deployment),\n               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/connector.py\", line 16, in wrapper\n    raise e\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/connector.py\", line 14, in wrapper\n    result = f(self, *f_args, **f_kwargs)\n             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/docker_stack.py\", line 187, in get_services\n    return self._stack_services(name)\n           ^^^^^^^^^^^^^^^^^^^^^^^^^^\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/docker_stack.py\", line 182, in _stack_services\n    for service in execute_cmd(cmd).stdout.splitlines()]\n                   ^^^^^^^^^^^^^^^^\n  File \"/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/utils.py\", line 90, in execute_cmd\n    raise Exception(result.stderr)\nException: Get \"https://10.0.128.103:2376/v1.44/services?filters=%7B%22label%22%3A%7B%22com.docker.stack.namespace%3Dc050e406-389d-4ce5-bce0-eb5c1551106d%22%3Atrue%7D%7D&status=true\": dial tcp 10.0.128.103:2376: i/o timeout\n\n"}])

(defscene coe-resource-actions-fail-parsing
          [JobCell {:action         "coe_resource_actions"
                    :status-message "status message that fail to be parsed as json should be rendered as default"}])

(defscene coe-resource-actions-missing-docker-entry
          [JobCell {:action         "coe_resource_actions"
                    :status-message "{\"msg\": \"json but not docker key inside rendered also as default\"}"}])

(defscene coe-resource-actions-single-success
          [JobCell {:status-message "{\n    \"docker\": [\n        {\n            \"success\": true,\n            \"return-code\": 200,\n            \"message\": \"Image hello-world:latest was already present and updated\"\n        }\n    ]\n}",
                    :id     "job/e4986f52-dadb-4ecf-8119-6de5a978cdf5"
                    :action "coe_resource_actions"}])

(defscene coe-resource-actions-multiple
          [JobCell {:status-message "{\n    \"docker\": [\n        {\n            \"success\": false,\n            \"return-code\": 409,\n            \"content\": \"{\\\"message\\\":\\\"conflict: unable to delete 4b03fe5b3f64 (cannot be forced) - image is being used by running container 7de9a7318c82\\\"}\\n\",\n            \"message\": \"conflict: unable to delete 4b03fe5b3f64 (cannot be forced) - image is being used by running container 7de9a7318c82\"\n        },\n        {\n            \"success\": false,\n            \"return-code\": 409,\n            \"content\": \"{\\\"message\\\":\\\"conflict: unable to delete 248ba48e3e90 (cannot be forced) - image is being used by running container 03702ff1f06e\\\"}\\n\",\n            \"message\": \"conflict: unable to delete 248ba48e3e90 (cannot be forced) - image is being used by running container 03702ff1f06e\"\n        },\n        {\n            \"success\": true,\n            \"return-code\": 200,\n            \"message\": \"Image 6a0f31c0b30b deleted successfully\"\n        },\n        {\n            \"success\": true,\n            \"return-code\": 200,\n            \"message\": \"Image 9d00c9a3e6ae deleted successfully\"\n        },\n        {\n            \"success\": true,\n            \"return-code\": 200,\n            \"message\": \"Image e94a611cbcdf deleted successfully\"\n        }\n    ]\n}",
                    :id     "job/e4986f52-dadb-4ecf-8119-6de5a978cdf5"
                    :action "coe_resource_actions"}])

