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
package org.kie.server.integrationtests.cluster.loadbalancer.jbpm;

import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.client.KieServicesClient;
import org.kie.server.integrationtests.cluster.loadbalancer.ClusterLoadbalancerBaseTest;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class ClusterLoadbalancerQueryServiceIntegrationTests extends ClusterLoadbalancerBaseTest {

    private static final String CONTAINER_ID = "query-definition-project";

    private static final long EXTENDED_TIMEOUT = 300000;

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/query-definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        // Having timeout issues due to kjar dependencies -> raised timeout.
        KieServicesClient client = createDefaultStaticClient(EXTENDED_TIMEOUT);
        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_ID, new KieContainerResource(CONTAINER_ID, releaseId));
        Assume.assumeTrue(reply.getType().equals(ServiceResponse.ResponseType.SUCCESS));
    }

//    @Override
//    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
//        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
//    }

    @Test
    @Ignore
    public void testQueryDefinitionsFromKjar() throws Exception {
        String expectedResolvedDS = System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds");

        List<QueryDefinition> queries = queryClient.getQueries(0, 10);
        assertNotNull(queries);
        assertEquals(2, queries.size());

        Map<String, QueryDefinition> mapped = queries.stream().collect(toMap(QueryDefinition::getName, q -> q));

        QueryDefinition registeredQuery = mapped.get("first-query");
        assertNotNull(registeredQuery);
        assertEquals("first-query", registeredQuery.getName());
        assertEquals(expectedResolvedDS, registeredQuery.getSource());
        assertEquals("select * from ProcessInstanceLog", registeredQuery.getExpression());
        assertEquals("PROCESS", registeredQuery.getTarget());

        registeredQuery = mapped.get("second-query");
        assertNotNull(registeredQuery);
        assertEquals("second-query", registeredQuery.getName());
        assertEquals(expectedResolvedDS, registeredQuery.getSource());
        assertEquals("select * from NodeInstanceLog", registeredQuery.getExpression());
        assertEquals("CUSTOM", registeredQuery.getTarget());
    }
    
}
