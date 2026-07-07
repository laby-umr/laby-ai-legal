package com.laby.module.legal.service.agent;

import com.laby.framework.security.core.LoginUser;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.agent.LegalAgentProposalMapper;
import com.laby.module.legal.enums.agent.LegalAgentProposalActionEnum;
import com.laby.module.legal.enums.agent.LegalAgentProposalStatusEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.opinion.LegalAuditOpinionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationContractCreateExecutor;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationTypePackageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalAgentProposalServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final Long CONVERSATION_ID = 999L;

    @InjectMocks
    private LegalAgentProposalService proposalService;

    @Mock
    private LegalAgentProposalMapper proposalMapper;
    @Mock
    private LegalOrchestrationSessionService orchestrationSessionService;
    @Mock
    private LegalOrchestrationContractCreateExecutor orchestrationContractCreateExecutor;
    @Mock
    private LegalOrchestrationTypePackageService orchestrationTypePackageService;
    @Mock
    private LegalAgentStepLogService agentStepLogService;
    @Mock
    private LegalAuditOpinionService opinionService;
    @Mock
    private LegalContractService contractService;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser().setId(USER_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, "token"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executeProposal_rejectsWhenOrchestrationSessionNotOwned() {
        LegalAgentProposalDO proposal = pendingOrchestrationProposal("p-no-1");
        when(proposalMapper.selectByProposalNo("p-no-1")).thenReturn(proposal);
        when(orchestrationSessionService.getByConversationId(CONVERSATION_ID))
                .thenReturn(LegalOrchestrationSessionDO.builder()
                        .conversationId(CONVERSATION_ID)
                        .userId(OTHER_USER_ID)
                        .build());

        assertThrows(Exception.class, () -> proposalService.executeProposal("p-no-1"));
    }

    @Test
    void executeProposal_allowsWhenOrchestrationSessionOwned() {
        LegalAgentProposalDO proposal = pendingOrchestrationProposal("p-no-2");
        when(proposalMapper.selectByProposalNo("p-no-2")).thenReturn(proposal);
        when(orchestrationSessionService.getByConversationId(CONVERSATION_ID))
                .thenReturn(LegalOrchestrationSessionDO.builder()
                        .conversationId(CONVERSATION_ID)
                        .userId(USER_ID)
                        .build());

        proposalService.executeProposal("p-no-2");

        verify(orchestrationContractCreateExecutor).executeClassifyConfirm(proposal);
        verify(proposalMapper).updateById(any(LegalAgentProposalDO.class));
    }

    private static LegalAgentProposalDO pendingOrchestrationProposal(String proposalNo) {
        return LegalAgentProposalDO.builder()
                .id(1L)
                .proposalNo(proposalNo)
                .conversationId(CONVERSATION_ID)
                .userId(USER_ID)
                .action(LegalAgentProposalActionEnum.CLASSIFY_CONFIRM.getAction())
                .status(LegalAgentProposalStatusEnum.PENDING.getStatus())
                .payloadJson("{}")
                .expireTime(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
