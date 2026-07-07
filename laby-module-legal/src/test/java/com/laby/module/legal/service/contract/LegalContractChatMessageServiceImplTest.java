package com.laby.module.legal.service.contract;

import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalContractChatMessageServiceImplTest {

    @Mock
    private LegalContractChatMessageMapper chatMessageMapper;
    @InjectMocks
    private LegalContractChatMessageServiceImpl chatMessageService;

    @Test
    void listMessages_shouldFilterBySessionId() {
        when(chatMessageMapper.selectListByContractIdAndUserIdAndSessionId(1L, 9L, "sess-a"))
                .thenReturn(List.of(LegalContractChatMessageDO.builder()
                        .id(1L)
                        .contractId(1L)
                        .sessionId("sess-a")
                        .type("user")
                        .content("only sess-a")
                        .build()));

        var result = chatMessageService.listMessages(1L, 9L, "sess-a");

        assertEquals(1, result.size());
        assertEquals("only sess-a", result.get(0).getContent());
        verify(chatMessageMapper).selectListByContractIdAndUserIdAndSessionId(1L, 9L, "sess-a");
    }

}
