package com.cms.service;

import com.cms.entity.*;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock FileRepository fileRepository;
    @Mock UserRepository userRepository;
    @Mock WorkflowTransitionRepository workflowTransitionRepository;
    @Mock ApprovalRequestRepository approvalRequestRepository;
    @Mock TriggerService triggerService;
    @Mock ActivityEventService activityEventService;

    @InjectMocks WorkflowService workflowService;

    private FileEntity file;
    private User actor;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("Org");
        org.setSlug("org");
        org.setBillingContactEmail("a@b.com");
        org.setStatus(Organization.OrganizationStatus.ACTIVE);

        Workspace ws = new Workspace();
        ws.setId(1L);
        ws.setOrganization(org);

        file = new FileEntity();
        file.setId(1L);
        file.setUuid("file-uuid-1");
        file.setName("test.pdf");
        file.setWorkflowState(WorkflowState.DRAFT);
        file.setWorkspace(ws);
        file.setOrganization(org);
        file.setStatus(FileEntity.FileStatus.ACTIVE);

        actor = new User();
        actor.setId(5L);
        actor.setEmail("actor@test.com");
        actor.setFirstName("Actor");
        actor.setLastName("User");
        actor.setStatus(User.UserStatus.ACTIVE);
        actor.setOrganization(org);

        lenient().when(workflowTransitionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void transition_fromDraftToReview_succeeds() {
        when(fileRepository.findByUuid("file-uuid-1")).thenReturn(Optional.of(file));
        when(userRepository.findById(5L)).thenReturn(Optional.of(actor));
        when(fileRepository.save(any())).thenReturn(file);

        var result = workflowService.transition("file-uuid-1", "REVIEW", "moving to review", 5L);

        assertThat(result).isNotNull();
        verify(fileRepository).save(argThat(f -> f.getWorkflowState() == WorkflowState.REVIEW));
    }

    @Test
    void transition_toApprovedState_throwsRequiresApproval() {
        file.setWorkflowState(WorkflowState.REVIEW);
        when(fileRepository.findByUuid("file-uuid-1")).thenReturn(Optional.of(file));
        when(userRepository.findById(5L)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> workflowService.transition("file-uuid-1", "APPROVED", null, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("approval");
    }

    @Test
    void transition_withInvalidTransition_throwsBusinessRuleException() {
        // DRAFT → PUBLISHED is not a valid transition
        when(fileRepository.findByUuid("file-uuid-1")).thenReturn(Optional.of(file));
        when(userRepository.findById(5L)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> workflowService.transition("file-uuid-1", "PUBLISHED", null, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    void transition_withUnknownState_throwsBusinessRuleException() {
        when(fileRepository.findByUuid("file-uuid-1")).thenReturn(Optional.of(file));
        when(userRepository.findById(5L)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> workflowService.transition("file-uuid-1", "BANANA", null, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid workflow state");
    }

    @Test
    void transition_withUnknownFile_throwsResourceNotFoundException() {
        when(fileRepository.findByUuid("no-file")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowService.transition("no-file", "REVIEW", null, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── WorkflowStateMachine unit tests ───────────────────────────────────────

    @Test
    void stateMachine_draftAllowsOnlyReview() {
        var allowed = WorkflowStateMachine.getAllowedTransitions(WorkflowState.DRAFT);
        assertThat(allowed).containsExactly(WorkflowState.REVIEW);
    }

    @Test
    void stateMachine_approvedRequiresApproval() {
        assertThat(WorkflowStateMachine.requiresApproval(WorkflowState.APPROVED)).isTrue();
        assertThat(WorkflowStateMachine.requiresApproval(WorkflowState.REVIEW)).isFalse();
    }

    @Test
    void stateMachine_archivedCanReturnToDraft() {
        assertThat(WorkflowStateMachine.isValidTransition(WorkflowState.ARCHIVED, WorkflowState.DRAFT)).isTrue();
    }
}
