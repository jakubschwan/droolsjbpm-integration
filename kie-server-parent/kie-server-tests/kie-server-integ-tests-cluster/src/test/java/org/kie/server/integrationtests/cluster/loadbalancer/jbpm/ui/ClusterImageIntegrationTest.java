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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.CONTAINER_ID;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.PROCESS_ID_HIRING;
import org.kie.server.integrationtests.cluster.loadbalancer.ClusterLoadbalancerBaseTest;


public class ClusterImageIntegrationTest extends ClusterLoadbalancerBaseTest {

    @Test
    public void testGetProcessImageViaUIClientTest() throws Exception {
        String result = uiServicesClient.getProcessImage(CONTAINER_ID, PROCESS_ID_HIRING);
        logger.debug("Image content is '{}'", result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGetProcessInstanceImageViaUIClientTest() throws Exception {
        long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_HIRING);
        assertTrue(processInstanceId > 0);
        try {
            String result = uiServicesClient.getProcessInstanceImage(CONTAINER_ID, processInstanceId);
            logger.debug("Image content is '{}'", result);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

}
