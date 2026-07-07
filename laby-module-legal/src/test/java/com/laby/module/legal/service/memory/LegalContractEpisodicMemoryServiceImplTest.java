package com.laby.module.legal.service.memory;

import com.laby.module.legal.dal.dataobject.memory.LegalContractMemoryDO;
import com.laby.module.legal.dal.mysql.memory.LegalContractMemoryMapper;
import com.laby.module.legal.framework.config.LegalContractMemoryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalContractEpisodicMemoryServiceImplTest {

    @Mock
    private LegalContractMemoryMapper memoryMapper;
    @InjectMocks
    private LegalContractEpisodicMemoryServiceImpl episodicMemoryService;

    private final LegalContractMemoryProperties properties = new LegalContractMemoryProperties();

    @BeforeEach
    void setUp() {
        properties.setMaxAppendixItems(5);
        ReflectionTestUtils.setField(episodicMemoryService, "memoryProperties", properties);
    }

    @Test
    void buildMemoryAppendix_shouldFormatMemories() {
        when(memoryMapper.selectListByContractId(eq(1L), eq(null), eq(5)))
                .thenReturn(List.of(LegalContractMemoryDO.builder()
                        .memoryType("fact")
                        .content("付款节点为 30 天")
                        .build()));

        String appendix = episodicMemoryService.buildMemoryAppendix(1L, null);

        assertTrue(appendix.contains("<ContractMemory>"));
        assertTrue(appendix.contains("付款节点为 30 天"));
    }

    @Test
    void listMemories_returnsMapperResult() {
        when(memoryMapper.selectListByContractId(1L, "s1", 50))
                .thenReturn(List.of(LegalContractMemoryDO.builder().id(9L).content("fact").build()));

        List<LegalContractMemoryDO> result = episodicMemoryService.listMemories(1L, "s1");

        assertEquals(1, result.size());
        assertEquals("fact", result.get(0).getContent());
    }

    @Test
    void createMemory_shouldInsertRow() {
        episodicMemoryService.createMemory(1L, "s1", "milestone", "付款周期 30 天");

        verify(memoryMapper).insert(any(LegalContractMemoryDO.class));
    }

    @Test
    void createMemory_shouldRejectFactType() {
        assertThrows(Exception.class,
                () -> episodicMemoryService.createMemory(1L, "s1", "fact", "付款周期 30 天"));
    }

    @Test
    void updateMemory_shouldUpdateExistingRow() {
        when(memoryMapper.selectById(5L)).thenReturn(LegalContractMemoryDO.builder()
                .id(5L)
                .contractId(1L)
                .contentHash("old")
                .build());
        when(memoryMapper.selectByContractIdAndHash(eq(1L), any())).thenReturn(null);

        episodicMemoryService.updateMemory(5L, 1L, "risk", "存在违约金争议");

        verify(memoryMapper).updateById(any(LegalContractMemoryDO.class));
    }

    @Test
    void deleteMemory_shouldRemoveWhenContractMatches() {
        when(memoryMapper.selectById(5L)).thenReturn(LegalContractMemoryDO.builder()
                .id(5L)
                .contractId(1L)
                .build());

        episodicMemoryService.deleteMemory(5L, 1L);

        verify(memoryMapper).deleteById(5L);
    }

    @Test
    void deleteMemory_shouldThrowWhenContractMismatch() {
        when(memoryMapper.selectById(5L)).thenReturn(LegalContractMemoryDO.builder()
                .id(5L)
                .contractId(2L)
                .build());

        assertThrows(Exception.class, () -> episodicMemoryService.deleteMemory(5L, 1L));
    }

    @Test
    void saveFact_shouldInsertWhenHashNotExists() {
        when(memoryMapper.selectByContractIdAndHash(eq(1L), any())).thenReturn(null);

        episodicMemoryService.saveFact(1L, "sess-1", "用户关注：付款周期；结论：30天", 10L);

        verify(memoryMapper).insert(any(LegalContractMemoryDO.class));
    }

}
