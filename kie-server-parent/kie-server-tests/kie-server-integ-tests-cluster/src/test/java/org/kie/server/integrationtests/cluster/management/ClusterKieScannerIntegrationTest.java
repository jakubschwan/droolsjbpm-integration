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
package org.kie.server.integrationtests.cluster.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class ClusterKieScannerIntegrationTest extends ClusterManagementBaseTest {

    //private static ReleaseId scannerReleaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "LATEST");
    private static ReleaseId newReleaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "2.0.0-SNAPSHOT");

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.createAndDeployKJar(newReleaseId);
    }

    //maybe try to deploying new version of project befor scanner use...
    @Test
    @Ignore
    public void testUpdateContianerByScannerScanNow() {
        //create default container with old release ID
        ContainerSpec containerSpec = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerSpec);
        mgmtControllerClient.startContainer(templateOne.getId(), containerSpec.getId());

        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientBravo, clientCharlie);

        //update releaseId version to LATEST to allow automatic update by kie scanner 
//        clientBravo.updateReleaseId(CONTAINER_ID, scannerReleaseId);
//        checkContainerOnServiceInstaces(releaseId, clientBravo, clientCharlie);

        //scan now to update container
        KieScannerResource kieScanner = new KieScannerResource(KieScannerStatus.SCANNING, 0l);
        ServiceResponse<KieScannerResource> scanNow = clientCharlie.updateScanner(CONTAINER_ID, kieScanner);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, scanNow.getType());
        assertEquals(KieScannerStatus.STOPPED, scanNow.getResult().getStatus());

        //check that scanner change version of container
        checkContainerOnServiceInstaces(newReleaseId, clientBravo, clientCharlie);
    }

    @Test
    @Ignore
    public void testUpdateContainerByScannerStarted() throws Exception {
        turnOffBravoServer();
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), createDefaultContainer(templateOne));
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie);

//        clientCharlie.updateReleaseId(CONTAINER_ID, scannerReleaseId);
//        checkContainerOnServiceInstaces(releaseId, clientCharlie);

        KieScannerResource kieScannerResource = new KieScannerResource(KieScannerStatus.STARTED, 100l);
        ServiceResponse<KieScannerResource> started = clientCharlie.updateScanner(CONTAINER_ID, kieScannerResource);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, started.getType());
        assertEquals(KieScannerStatus.STARTED, started.getResult().getStatus());

        checkContainerOnServiceInstaces(newReleaseId, clientCharlie);

        turnOffCharlieServer();
        // deploy new version of contianer
        ReleaseId newNewReleaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "2.1.0-SNAPSHOT");
        KieServerDeployer.createAndDeployKJar(newNewReleaseId);

        turnOnBravoServer();
        //container should be autoupdate by started scanner
        checkContainerOnServiceInstaces(newNewReleaseId, clientBravo);
    }

    private void checkContainerOnServiceInstaces(ReleaseId resolvedReleaseId, KieServicesClient... clients) {
        for (KieServicesClient client : clients) {
            ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
            assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfo.getType());
            assertNotNull(containerInfo.getResult());
            checkContainerReleaseAndResolvedReleaseId(containerInfo.getResult(), releaseId, resolvedReleaseId);
        }
    }

    private void checkContainerReleaseAndResolvedReleaseId(KieContainerResource containerInfo, ReleaseId releaseId, ReleaseId resolvedReleaseId) {
        assertEquals(CONTAINER_ID, containerInfo.getContainerId());
        assertEquals(releaseId, containerInfo.getReleaseId());
        assertEquals(resolvedReleaseId, containerInfo.getResolvedReleaseId());
    }

}
