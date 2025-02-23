package ai.aletyx.kie.workshop.foureyes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.kogito.Model;
import org.kie.kogito.auth.IdentityProviders;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class ProcessTest {

    @Named("travel")
    @Inject
    Process<? extends Model> travelProcess;

    @Test
    public void testApprovalProcess() {

        assertNotNull(travelProcess);

        Model m = travelProcess.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("traveller", new TravelRequest("John", "Doe", "john.doe@example.com", "American", "Brazil"));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = travelProcess.createInstance(m);
        processInstance.start();
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.status());

        SecurityPolicy policy = SecurityPolicy.of(IdentityProviders.of("admin", Collections.singletonList("managers")));

        processInstance.workItems(policy);

        List<WorkItem> workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());
        Map<String, Object> results = new HashMap<>();
        results.put("approved", "yes");
        processInstance.completeWorkItem(workItems.get(0).getId(), results, policy);

        workItems = processInstance.workItems(policy);
        assertEquals(0, workItems.size());

        policy = SecurityPolicy.of(IdentityProviders.of("john", Collections.singletonList("managers")));

        processInstance.workItems(policy);

        workItems = processInstance.workItems(policy);
        assertEquals(1, workItems.size());

        results.put("approved", "no");
        processInstance.completeWorkItem(workItems.get(0).getId(), results, policy);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.status());

        Model result = (Model) processInstance.variables();
        assertEquals(5, result.toMap().size());
        assertEquals(result.toMap().get("firstApprover"), "admin");
        assertEquals(result.toMap().get("secondApprover"), "john");
        assertEquals(result.toMap().get("firstApproved"), "yes");
        assertEquals(result.toMap().get("secondApproved"), "no");
    }
}
