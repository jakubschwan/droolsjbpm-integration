package org.kie.server.integrationtests.clustering.client.loadbalancer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.JobServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.integrationtests.clustering.ClusterBaseTest;
import org.kie.server.integrationtests.shared.KieServerDeployer;

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
public abstract class ClusterLoadbalancerBaseTest extends ClusterBaseTest {
    
    protected static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project", "1.0.0.Final");
    protected static KieContainer kieContainer;

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);
    }

    //these tests use defaultLoadBalancer in client
    protected ProcessServicesClient processClient;
    protected UserTaskServicesClient taskClient;
    protected QueryServicesClient queryClient;
    protected JobServicesClient jobServicesClient;
    protected DocumentServicesClient documentClient;

    @Before
    public void setupClusterClients() throws Exception {
        client = createDefaultClient(serversUrl);
        processClient = client.getServicesClient(ProcessServicesClient.class);
        taskClient = client.getServicesClient(UserTaskServicesClient.class);
        queryClient = client.getServicesClient(QueryServicesClient.class);
        jobServicesClient = client.getServicesClient(JobServicesClient.class);
        documentClient = client.getServicesClient(DocumentServicesClient.class);
    }

    @After
    public void cleanAfterTest() {
        disposeAllClusterContainers();
    }

    protected Object createInstance(String objectClassIdentifier, Object... constructorParameters) {
        Class<?>[] parameterClasses = new Class[constructorParameters.length];
        for (int i = 0; i < constructorParameters.length; i++) {
            parameterClasses[i] = constructorParameters[i].getClass();
        }

        try {
            Class<?> clazz = extraClasses.get(objectClassIdentifier);
            if (clazz != null) {
                Object object = clazz.getConstructor(parameterClasses).newInstance(constructorParameters);
                return object;
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create object due " + e.getMessage(), e);
        }
        throw new RuntimeException("Instantiated class isn't defined in extraClasses set. Please define it first.");
    }
}
