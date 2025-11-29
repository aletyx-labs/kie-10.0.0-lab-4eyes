package ai.aletyx.kie.workshop.foureyes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.kogito.Model;
import org.kie.kogito.auth.IdentityProvider;
import org.kie.kogito.auth.IdentityProviders;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.usertask.UserTaskService;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class ProcessTest {

    @Named("travel")
    @Inject
    Process<? extends Model> travelProcess;

    @Inject
    UserTaskService userTaskService;

    @Test
    public void testApprovalProcess() {
        assertNotNull(travelProcess);
        Model m = travelProcess.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("traveller", new TravelRequest("John", "Doe", "john.doe@example.com", "American", "Brazil"));
        m.fromMap(parameters);

        IdentityProvider wrong = IdentityProviders.of("john", Collections.singletonList("something"));
        IdentityProvider correct1 = IdentityProviders.of("admin", Collections.singletonList("managers"));
        IdentityProvider correct2 = IdentityProviders.of("john", Collections.singletonList("managers"));

        ProcessInstance<?> processInstance = travelProcess.createInstance(m);
        processInstance.start();

        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        assertEquals(1, processInstance.workItems(SecurityPolicy.of(correct1)).size());
        assertEquals(0, processInstance.workItems(SecurityPolicy.of(wrong)).size());
        assertEquals(0, userTaskService.list(wrong).size());

        {
            var userTaskViews = userTaskService.list(correct1);
            assertEquals(1, userTaskViews.size());
            userTaskService.transition(userTaskViews.get(0).getId(), "claim", emptyMap(), correct1);
            processInstance.completeWorkItem(processInstance.workItems(SecurityPolicy.of(correct1)).get(0).getId(), Map.of("approved", "yes"), SecurityPolicy.of(correct1));
        }

        {
            var userTaskViews = userTaskService.list(correct2);
            assertEquals(1, userTaskViews.size());
            userTaskService.transition(userTaskViews.get(0).getId(), "claim", emptyMap(), correct2);
            processInstance.completeWorkItem(processInstance.workItems(SecurityPolicy.of(correct2)).get(0).getId(), Map.of("approved", "no"), SecurityPolicy.of(correct2));
        }

        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.status());

        Model result = (Model) processInstance.variables();
        assertEquals(5, result.toMap().size());
        assertEquals(result.toMap().get("firstApprover"), "admin");
        assertEquals(result.toMap().get("secondApprover"), "john");
        assertEquals(result.toMap().get("firstApproved"), "yes");
        assertEquals(result.toMap().get("secondApproved"), "no");
    }
}
