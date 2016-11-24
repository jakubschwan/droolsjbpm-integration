/*
 * Copyright 2016 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.server.integrationtests.cluster.client;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.internal.executor.api.STATUS;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.JobRequestInstance;
import org.kie.server.api.model.instance.RequestInfoInstance;
import org.kie.server.client.JobServicesClient;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

public class ClusterJobServiceIntegrationTest extends ClusterClientBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    protected static final String BUSINESS_KEY = "test key";
    protected static final String PRINT_OUT_COMMAND = "org.jbpm.executor.commands.PrintOutCommand";

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

    }

    @Before
    public void finishAllJobs() throws Exception  {
        List<String> status = new ArrayList<String>();
        status.add(STATUS.QUEUED.toString());
        status.add(STATUS.RUNNING.toString());
        status.add(STATUS.RETRYING.toString());
        finishAllJobsOnClient(status, jobServicesBravoClient, jobServicesCharlieClient);
    }

    @Test
    public void testScheduleAndRunJob() throws Exception {
        JobRequestInstance jobRequestInstance = createJobRequestInstance();

        Long jobId = jobServicesBravoClient.scheduleRequest(jobRequestInstance);
        assertNotNull(jobId);
        assertTrue(jobId.longValue() > 0);

        RequestInfoInstance jobRequest = jobServicesBravoClient.getRequestById(jobId, false, false);
        assertNotNull(jobRequest);
        assertEquals(jobId, jobRequest.getId());
        assertEquals(BUSINESS_KEY, jobRequest.getBusinessKey());
        assertThat(jobRequest.getStatus(), anyOf(
                equalTo(STATUS.QUEUED.toString()),
                equalTo(STATUS.RUNNING.toString()),
                equalTo(STATUS.DONE.toString())));
        assertEquals(PRINT_OUT_COMMAND, jobRequest.getCommandName());

        jobRequest = jobServicesCharlieClient.getRequestById(jobId, false, false);
        assertNotNull(jobRequest);
        assertEquals(jobId, jobRequest.getId());
        assertEquals(BUSINESS_KEY, jobRequest.getBusinessKey());
        assertThat(jobRequest.getStatus(), anyOf(
                equalTo(STATUS.QUEUED.toString()),
                equalTo(STATUS.RUNNING.toString()),
                equalTo(STATUS.DONE.toString())));
        assertEquals(PRINT_OUT_COMMAND, jobRequest.getCommandName());

        turnOffBravoServer();

        KieServerSynchronization.waitForJobToFinish(jobServicesCharlieClient, jobId);

        jobRequest = jobServicesCharlieClient.getRequestById(jobId, false, false);
        assertNotNull(jobRequest);
        assertEquals(jobId, jobRequest.getId());
        assertEquals(BUSINESS_KEY, jobRequest.getBusinessKey());
        assertEquals(STATUS.DONE.toString(), jobRequest.getStatus());
        assertEquals(PRINT_OUT_COMMAND, jobRequest.getCommandName());

    }

    @Test
    public void testScheduleSearchByStatusAndCancelJob() {
        int currentNumberOfCancelled = jobServicesCharlieClient.getRequestsByStatus(Collections.singletonList(STATUS.CANCELLED.toString()), 0, 100).size();

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        JobRequestInstance jobRequestInstance = createJobRequestInstance();
        jobRequestInstance.setScheduledDate(tomorrow.getTime());

        Long jobId = jobServicesCharlieClient.scheduleRequest(jobRequestInstance);
        assertNotNull(jobId);
        assertTrue(jobId.longValue() > 0);

        List<String> status = new ArrayList<String>();
        status.add(STATUS.QUEUED.toString());

        List<RequestInfoInstance> result = jobServicesCharlieClient.getRequestsByStatus(status, 0, 100);
        assertNotNull(result);
        assertEquals(1, result.size());

        RequestInfoInstance jobRequest = result.get(0);
        RequestInfoInstance expected = createExpectedRequestInfoInstance(jobId, STATUS.QUEUED);
        assertRequestInfoInstance(expected, jobRequest);
        assertNotNull(jobRequest.getScheduledDate());

        turnOffCharlieServer();

        jobServicesBravoClient.cancelRequest(jobId);

        result = jobServicesBravoClient.getRequestsByStatus(status, 0, 100);
        assertNotNull(result);
        assertEquals(0, result.size());

        // clear status to search only for canceled
        status.clear();
        status.add(STATUS.CANCELLED.toString());

        result = jobServicesBravoClient.getRequestsByStatus(status, 0, 100);
        assertNotNull(result);
        assertEquals(1 + currentNumberOfCancelled, result.size());
    }

    private void finishAllJobsOnClient(List<String> status, JobServicesClient... clients) throws Exception {
        for (JobServicesClient jobServicesClient : clients) {
            List<RequestInfoInstance> requests = jobServicesClient.getRequestsByStatus(status, 0, 100);
            for (RequestInfoInstance instance : requests) {
                jobServicesClient.cancelRequest(instance.getId());
                KieServerSynchronization.waitForJobToFinish(jobServicesClient, instance.getId());
            }
        }
    }

    private void assertRequestInfoInstance(RequestInfoInstance expected, RequestInfoInstance actual) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getBusinessKey(), actual.getBusinessKey());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getCommandName(), actual.getCommandName());
    }

    private RequestInfoInstance createExpectedRequestInfoInstance(Long jobId, STATUS expected) {
        return RequestInfoInstance.builder()
                .id(jobId)
                .businessKey(BUSINESS_KEY)
                .status(expected.toString())
                .command(PRINT_OUT_COMMAND)
                .build();
    }

    private JobRequestInstance createJobRequestInstance() {
        Map<String, Object> data = new HashMap<>();
        data.put("businessKey", BUSINESS_KEY);

        JobRequestInstance jobRequestInstance = new JobRequestInstance();
        jobRequestInstance.setCommand(PRINT_OUT_COMMAND);
        jobRequestInstance.setData(data);
        return jobRequestInstance;
    }

}
