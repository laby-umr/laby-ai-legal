package com.laby.module.legal.tool.orchestration;

import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.service.contracttype.LegalContractTypeService;
import io.agentscope.core.tool.Tool;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("legal_orchestration_list_contract_types")
public class LegalOrchestrationListContractTypesTool {

    @Resource
    private LegalContractTypeService contractTypeService;

    @Data
    public static class TypeItem {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    public static class Response {
        private List<TypeItem> types;
    }

    @Tool(name = "legal_orchestration_list_contract_types",
            description = "列出租户可用合同类型（只读），用于分类映射参考")
    public Response listContractTypes(LegalOrchestrationToolRuntimeContext toolContext) {
        List<LegalContractTypeDO> types = contractTypeService.getContractTypeSimpleList();
        Response response = new Response();
        response.setTypes(types.stream().map(type -> {
            TypeItem item = new TypeItem();
            item.setId(type.getId());
            item.setCode(type.getCode());
            item.setName(type.getName());
            return item;
        }).collect(Collectors.toList()));
        return response;
    }

}
