# Lab: 4 eyes principle with User Task!

The four-eyes principle is a control mechanism that requires two people to review and approve a decision or transaction. It's used to reduce risk, increase transparency, and prevent fraud. 

## How it works 

- A second, independent person reviews and verifies an activity
- Different people perform each important activity
- A person who submits a workflow can't also approve it

## Where it's used 

- Finance: To reduce the risk of payment fraud
- Procurement: To ensure that multiple people are involved in purchasing
- System access: To ensure that sensitive systems are monitored
- Software development: To review proposed changes to a branch

## Lab Overview

In this Lab we’ll apply the four-eyes principle to a simple travel approval process, that if not reviewed in time will result in an escalation to senior management.

1. Review travel request by first line manager
2. Review travel request by second line manager

If any manager takes longer than expected time, it'll escalate to upper management, so for that we'll use a simple script task to illustrate.

- Escalate travel using Script

![4 eyes principle Lab](images/4eyes-lab.png){: .screenshot }

### Supporting Files

The Java services provided in the repository https://github.com/aletyx-labs/kie-10.0.0-lab-4eyes contain the infrastructure to support this process.

#### TravelRequest.java

Information about the travel being requested, including the requester and destination information.

### Instructions

#### Step 1: Clone the Repository

Clone the lab project from this repository, which contains all the necessary files and infrastructure for the lab.

#### Step 2: Import the Project

Import the project into VS Code and explore the provided Java classes.

#### Step 3: Create the BPMN file

Create a new file name travel-request.bpmn under src/main/resources and open it using the Apache KIE BPMN Editor.

Once file is created, create the process variables that we'll need.


| Name            | Data Type                                      | Tags |
|---------------|----------------------------------|------|
| firstApprover  | String                           | output |
| secondApprover | String                           | output |
| request       | ai.aletyx.kie.workshop.foureyes.TravelRequest | input |
| firstApproved  | String                           | output |
| secondApproved | String                           | output |

#### Step 4: Create the process diagram

1. Start Event: Initiates the process.
2. User Task: Approval 1

     Task Name: Approval 1

     Groups: managers

     Variables - Input Mapping:

     | Name   | Data Type | Source     |
     |--------|----------|------------|
     | request  | TravelRequest [ai.aletyx.kie.workshop.foureyes]    | request   |
     | approved | String    | firstApprover   |

     Variables - Output Mapping:

     | Name   | Data Type                                           | Target |
     |--------|-----------------------------------------------------|--------|
     | ActorId | String | firstApprover |
     | approved | String | firstApproved |

2. User Task: Approval 2

     Task Name: Approval 2

     Groups: managers

     Variables - Input Mapping:

     | Name   | Data Type | Source     |
     |--------|----------|------------|
     | ExcludedOwnerId  | String    | firstApprover   |
     | request  | TravelRequest [ai.aletyx.kie.workshop.foureyes]    | request   |
     | firstApproved | String    | firstApproved   |
     | approved | String    | secondApproved   |

     Variables - Output Mapping:

     | Name   | Data Type                                           | Target |
     |--------|-----------------------------------------------------|--------|
     | ActorId | String | secondApprover |
     | approved | String | secondApproved |

3. End
4. Drag a Timer and bind it to Approval 1

     Fire once after duration: PT1H

5. Drag a Timer and bind it to Approval 2

     Fire once after duration: PT20S

6. Create an Inclusive Gateway and connect both timers to it

7. Create a Script Task and connect the Incluse Gateway to it

     Script:
        ```java
            System.out.println("Escalated!");
        ```

8. End

#### Step 5: Test the Process

- Run the process in different scenarios.

You can also use the following code to test:

```java

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

```
