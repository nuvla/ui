import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectMsg } from './job-cell';

test('basic-long-status-message', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'job-cell-scenes', 'basic-long-status-message');
  await expect(sceneRoot.getByRole('button', { name: 'Reset' })).toBeVisible();
  await expect(sceneRoot).toHaveScreenshot();
  await sceneRoot.getByRole('button', { name: '▼' }).click();
  await expectMsg(sceneRoot, 'Exception-Traceback (most recent call last): File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/executor/executor.py", line 57, in process_job return_code = action_instance.do_work() ^^^^^^^^^^^^^^^^^^^^^^^^^ File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/actions/deployment_state.py", line 59, in do_work raise ex File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/actions/deployment_state.py", line 54, in do_work self.get_application_state() File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/job/actions/deployment_state.py", line 34, in get_application_state services = connector.get_services(Deployment.uuid(self.deployment), ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/connector.py", line 16, in wrapper raise e File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/connector.py", line 14, in wrapper result = f(self, *f_args, **f_kwargs) ^^^^^^^^^^^^^^^^^^^^^^^^^^^^ File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/docker_stack.py", line 187, in get_services return self._stack_services(name) ^^^^^^^^^^^^^^^^^^^^^^^^^^ File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/docker_stack.py", line 182, in _stack_services for service in execute_cmd(cmd).stdout.splitlines()] ^^^^^^^^^^^^^^^^ File "/usr/local/lib/python3.11/site-packages/nuvla/job_engine/connector/utils.py", line 90, in execute_cmd raise Exception(result.stderr) Exception: Get "https://10.0.128.103:2376/v1.44/services?filters=%7B%22label%22%3A%7B%22com.docker.stack.namespace%3Dc050e406-389d-4ce5-bce0-eb5c1551106d%22%3Atrue%7D%7D&status=true": dial tcp 10.0.128.103:2376: i/o timeout');
  await expect(sceneRoot).toHaveScreenshot();
  await sceneRoot.getByRole('button', { name: '▲' }).click();
  await expect(sceneRoot).toHaveScreenshot();
});
