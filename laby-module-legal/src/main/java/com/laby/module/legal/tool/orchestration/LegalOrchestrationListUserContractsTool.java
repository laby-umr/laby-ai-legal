package com.laby.module.legal.tool.orchestration;

import com.laby.framework.common.pojo.PageParam;
import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractRespVO;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("legal_orchestration_list_user_contracts")
public class LegalOrchestrationListUserContractsTool {

    @Resource
    private LegalContractService contractService;

    @Data
    public static class ContractItem {
        private Long id;
        private String title;
        private Integer status;
        private String statusName;
        private Integer riskHighCount;
        private String createSource;
        private Long createConversationId;
    }

    @Data
    public static class Response {
        private List<ContractItem> contracts;
        private Long total;
    }

    @Tool(name = "legal_orchestration_list_user_contracts",
            description = "查询当前用户合同列表，可按标题模糊搜索，用于跟踪审核进度")
    public Response listUserContracts(
            @ToolParam(name = "title", description = "标题关键词", required = false) String title,
            @ToolParam(name = "conversationId", description = "仅查本对话创建的合同", required = false) Long conversationId,
            LegalOrchestrationToolRuntimeContext toolContext) {
        Long userId = LegalOrchestrationToolSupport.requireUserId(toolContext);

        LegalContractPageReqVO pageReqVO = new LegalContractPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        pageReqVO.setTitle(title);

        PageResult<LegalContractRespVO> page = contractService.getContractRespPage(userId, pageReqVO);
        List<ContractItem> items = page.getList().stream()
                .filter(vo -> conversationId == null || conversationId.equals(vo.getCreateConversationId()))
                .map(vo -> {
                    ContractItem item = new ContractItem();
                    item.setId(vo.getId());
                    item.setTitle(vo.getTitle());
                    item.setStatus(vo.getStatus());
                    item.setStatusName(resolveStatusName(vo.getStatus()));
                    item.setRiskHighCount(vo.getRiskHighCount());
                    item.setCreateSource(vo.getCreateSource());
                    item.setCreateConversationId(vo.getCreateConversationId());
                    return item;
                })
                .collect(Collectors.toList());

        Response response = new Response();
        response.setContracts(items);
        response.setTotal((long) items.size());
        return response;
    }

    private static String resolveStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        for (LegalContractStatusEnum item : LegalContractStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item.getName();
            }
        }
        return String.valueOf(status);
    }

}
