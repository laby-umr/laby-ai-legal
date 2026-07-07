package com.laby.module.legal.service.contract.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合同附件下载内容（文件名 + 字节），供 Controller 写入 HTTP 响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractFileDownloadBO {

    /** 下载展示文件名 */
    private String fileName;
    /** 文件二进制内容 */
    private byte[] content;

}
