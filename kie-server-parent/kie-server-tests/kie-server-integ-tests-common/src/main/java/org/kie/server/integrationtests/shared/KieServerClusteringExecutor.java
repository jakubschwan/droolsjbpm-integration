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
package org.kie.server.integrationtests.shared;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.KieServerEnvironment;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.basetests.KieServerBaseIntegrationTest;
import org.kie.server.remote.rest.common.resource.KieServerRestImpl;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.impl.KieServerImpl;

public class KieServerClusteringExecutor extends KieServerExecutor {

    private Integer ALLOCATED_PORT;

    @Override
    public void startKieServer() {
        if (server != null) {
            throw new RuntimeException("Kie execution server is already created!");
        }

        System.setProperty(KieServerConstants.CFG_BYPASS_AUTH_USER, "true");
        System.setProperty(KieServerConstants.CFG_HT_CALLBACK, "custom");
        System.setProperty(KieServerConstants.CFG_HT_CALLBACK_CLASS, "org.kie.server.integrationtests.jbpm.util.FixedUserGroupCallbackImpl");
        System.setProperty(KieServerConstants.CFG_PERSISTANCE_DS, "jdbc/jbpm-ds");
        System.setProperty(KieServerConstants.CFG_PERSISTANCE_TM, "org.hibernate.service.jta.platform.internal.BitronixJtaPlatform");
        System.setProperty(KieServerConstants.CFG_PERSISTANCE_DIALECT, "org.hibernate.dialect.H2Dialect");
//        System.setProperty(KieServerConstants.CFG_PERSISTANCE_DEFAULT_SCHEMA,null);
        System.setProperty(KieServerConstants.KIE_SERVER_CONTROLLER, TestConfig.getControllerHttpUrl());
        System.setProperty(KieServerConstants.CFG_KIE_CONTROLLER_USER, TestConfig.getUsername());
        System.setProperty(KieServerConstants.CFG_KIE_CONTROLLER_PASSWORD, TestConfig.getPassword());
        System.setProperty(KieServerConstants.KIE_SERVER_LOCATION, getEmbeddedKieServerHttpUrl());
        System.setProperty(KieServerConstants.KIE_SERVER_STATE_REPO, "./target");
        System.setProperty(KieServerConstants.KIE_JBPM_UI_SERVER_EXT_DISABLED, "true");

        // Register server id if wasn't done yet
        if (KieServerEnvironment.getServerId() == null) {
            KieServerEnvironment.setServerId(KieServerBaseIntegrationTest.class.getSimpleName() + "@" + serverIdSuffixDateFormat.format(new Date()));
            KieServerEnvironment.setServerName("KieServer");
        }

        server = new TJWSEmbeddedJaxrsServer();
        server.setPort(getKieServerAllocatedPort());
        server.start();

        kieServer = new KieServerImpl();
        server.getDeployment().getRegistry().addSingletonResource(new KieServerRestImpl(kieServer));

        List<KieServerExtension> extensions = kieServer.getServerExtensions();

        for (KieServerExtension extension : extensions) {
            List<Object> components = extension.getAppComponents(SupportedTransports.REST);
            for (Object component : components) {
                server.getDeployment().getRegistry().addSingletonResource(component);
            }
        }

    }

    public String getEmbeddedKieServerHttpUrl() {
        return "http://localhost:" + getKieServerAllocatedPort() + "/server";
    }

    public Integer getKieServerAllocatedPort() {
        if (ALLOCATED_PORT == null) {
            try {
                ServerSocket server = new ServerSocket(0);
                ALLOCATED_PORT = server.getLocalPort();
                server.close();
            } catch (IOException e) {
                // failed to dynamically allocate port, try to use hard coded one
                ALLOCATED_PORT = 9789;
            }
        }

        return ALLOCATED_PORT;
    }

}
