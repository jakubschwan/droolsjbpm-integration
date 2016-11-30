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
package org.kie.server.integrationtests.cluster.loadbalancer.jbpm.ui;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.UIServicesClient;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.CONTAINER_ID;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.PROCESS_ID_HIRING;
import org.kie.server.integrationtests.cluster.loadbalancer.ClusterLoadbalancerBaseTest;


public class ClusterFormIntegrationTest extends ClusterLoadbalancerBaseTest {

    @Test
    public void testGetTaskFormViaUIClientTest() throws Exception {
        long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_HIRING);
        runGetTaskFormTest( processInstanceId );
    }

    protected void runGetTaskFormTest( long processInstanceId ) {
        assertTrue(processInstanceId > 0);
        try {
            List<TaskSummary> tasks = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, null, 0, 10);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());

            Long taskId = tasks.get(0).getId();

            String result = uiServicesClient.getTaskForm(CONTAINER_ID, taskId, "en");
            logger.debug("Form content is '{}'", result);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testGetProcessFormViaUIClientTestByType() throws Exception {
        String result = uiServicesClient.getProcessFormByType(CONTAINER_ID, PROCESS_ID_HIRING, "en", UIServicesClient.FORM_TYPE);
        logger.debug("Form content is '{}'", result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("hiring-taskform.frm"));
    }

   
}
