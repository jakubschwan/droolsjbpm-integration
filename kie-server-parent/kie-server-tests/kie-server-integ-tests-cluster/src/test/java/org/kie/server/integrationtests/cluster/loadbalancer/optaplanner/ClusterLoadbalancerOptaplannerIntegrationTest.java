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
package org.kie.server.integrationtests.cluster.loadbalancer.optaplanner;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.instance.ScoreWrapper;
import org.kie.server.api.model.instance.SolverInstance;
import org.kie.server.integrationtests.cluster.loadbalancer.ClusterLoadbalancerBaseTest;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;


public class ClusterLoadbalancerOptaplannerIntegrationTest extends ClusterLoadbalancerBaseTest {
 
    
    
    @Test(timeout = 60000)
    @Ignore
    public void testGetBestSolution() throws Exception {
//        KieServerAssert.assertSuccess( solverClient.createSolver( CONTAINER_1_ID, SOLVER_1_ID, SOLVER_1_CONFIG ) );
//
//        // the following status starts the solver
//        SolverInstance instance = new SolverInstance();
//        instance.setStatus( SolverInstance.SolverStatus.SOLVING );
//        instance.setPlanningProblem( loadPlanningProblem( 10, 30 ) );
//        ServiceResponse<SolverInstance> response = solverClient.updateSolverState( CONTAINER_1_ID, SOLVER_1_ID, instance );
//        KieServerAssert.assertSuccess( response );
//        assertEquals( SolverInstance.SolverStatus.SOLVING, response.getResult().getStatus() );
//
//        Object solution = null;
//        HardSoftScore score = null;
//        // It can take a while for the Construction Heuristic to initialize the solution
//        // The test timeout will interrupt this thread if it takes too long
//        while (!Thread.currentThread().isInterrupted()) {
//            ServiceResponse<SolverInstance> solutionResponse = solverClient.getSolverBestSolution(CONTAINER_1_ID, SOLVER_1_ID);
//            KieServerAssert.assertSuccess(solutionResponse);
//            solution = solutionResponse.getResult().getBestSolution();
//
//            ScoreWrapper scoreWrapper = solutionResponse.getResult().getScoreWrapper();
//            assertNotNull( scoreWrapper );
//
//            if ( scoreWrapper.toScore() != null ) {
//                assertEquals( HardSoftScore.class, scoreWrapper.getScoreClass() );
//                score = (HardSoftScore) scoreWrapper.toScore();
//            }
//
//            // Wait until the solver finished initializing the solution
//            if (solution != null && score != null && score.isSolutionInitialized()) {
//                break;
//            }
//            Thread.sleep(1000);
//        }
//        assertNotNull(score);
//        assertTrue(score.isSolutionInitialized());
//        assertTrue(score.getHardScore() <= 0);
//        // A soft score of 0 is impossible because we'll always need at least 1 computer
//        assertTrue(score.getSoftScore() < 0);
//
//        List<?> computerList = (List<?>) valueOf(solution, "computerList");
//        assertEquals(10, computerList.size());
//        List<?> processList = (List<?>) valueOf(solution, "processList");
//        assertEquals(30, processList.size());
//        for(Object process : processList) {
//            Object computer = valueOf(process, "computer");
//            assertNotNull(computer);
//            // TODO: Change to identity comparation after @XmlID is implemented
//            assertTrue(computerList.contains(computer));
//        }
//
//        KieServerAssert.assertSuccess( solverClient.disposeSolver( CONTAINER_1_ID, SOLVER_1_ID ) );
    }
}
