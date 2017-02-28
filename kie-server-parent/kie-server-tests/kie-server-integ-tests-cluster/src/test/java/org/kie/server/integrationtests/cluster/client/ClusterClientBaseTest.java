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

import org.junit.Before;
import org.kie.api.runtime.KieContainer;
import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.JobServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.integrationtests.cluster.ClusterBaseTest;

public abstract class ClusterClientBaseTest extends ClusterBaseTest {

    protected static KieContainer kieContainer;

    protected ProcessServicesClient primaryProcessClient;
    protected UserTaskServicesClient primaryTaskClient;
    protected QueryServicesClient primaryQueryClient;
    protected JobServicesClient primaryJobServicesClient;
    protected DocumentServicesClient primaryDocumentClient;

    protected ProcessServicesClient secondaryProcessClient;
    protected UserTaskServicesClient secondaryTaskClient;
    protected QueryServicesClient secondaryQueryClient;
    protected JobServicesClient secondaryJobServicesClient;
    protected DocumentServicesClient secondaryDocumentClient;

    @Before
    public void setupClusterClients() {
        primaryProcessClient = primaryClient.getServicesClient(ProcessServicesClient.class);
        primaryTaskClient = primaryClient.getServicesClient(UserTaskServicesClient.class);
        primaryQueryClient = primaryClient.getServicesClient(QueryServicesClient.class);
        primaryJobServicesClient = primaryClient.getServicesClient(JobServicesClient.class);
        primaryDocumentClient = primaryClient.getServicesClient(DocumentServicesClient.class);

        secondaryProcessClient = secondaryClient.getServicesClient(ProcessServicesClient.class);
        secondaryTaskClient = secondaryClient.getServicesClient(UserTaskServicesClient.class);
        secondaryQueryClient = secondaryClient.getServicesClient(QueryServicesClient.class);
        secondaryJobServicesClient = secondaryClient.getServicesClient(JobServicesClient.class);
        secondaryDocumentClient = secondaryClient.getServicesClient(DocumentServicesClient.class);
    }
}
