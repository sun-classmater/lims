package com.lims.controller;

import com.lims.common.R;
import com.lims.service.DecomposeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 分解排样 - 单个样品分解 / 批量样品分解。
 *
 * 左侧待分解样品检验项目：全部未确认分解完毕的样品；
 * 右侧已分解样品检验项目：已确认分解完毕的样品。
 */
@RestController
@RequestMapping("/decompose")
public class DecomposeController {

    private final DecomposeService decomposeService;

    public DecomposeController(DecomposeService decomposeService) {
        this.decomposeService = decomposeService;
    }

    /** 待分解样品列表，可按任务号、样品名称筛选 */
    @GetMapping("/pending")
    public R<Map<String, Object>> pending(
            @RequestParam(value = "taskno", required = false) String taskno,
            @RequestParam(value = "productName", required = false) String productName
    ) {
        return R.ok(decomposeService.pending(taskno, productName));
    }

    /** 已分解样品列表 */
    @GetMapping("/done")
    public R<Map<String, Object>> done() {
        return R.ok(decomposeService.done());
    }

    /** 批量分解：勾选样品，或按任务号/样品名称批量生成检验明细 */
    @PostMapping("/generate")
    public R<Map<String, Object>> generate(@RequestBody(required = false) GenerateRequest request) {
        GenerateRequest payload = request == null ? new GenerateRequest(null, null, null, null) : request;
        return R.ok(decomposeService.generate(
                payload.bhs(),
                payload.library(),
                payload.taskno(),
                payload.keyword()
        ));
    }

    /** 分解完毕：支持单条与批量 */
    @PostMapping("/complete")
    public R<Map<String, Object>> complete(@RequestBody(required = false) CompleteRequest request) {
        CompleteRequest payload = request == null ? new CompleteRequest(null, null) : request;
        return R.ok(decomposeService.complete(payload.bhs(), payload.library()));
    }

    /** 转待分解 */
    @PostMapping("/revert")
    public R<Map<String, Object>> revert(@RequestBody(required = false) BatchRequest request) {
        return R.ok(decomposeService.revert(request == null ? List.of() : request.bhs()));
    }

    /** 退登记 */
    @PostMapping("/return-register")
    public R<Integer> returnRegister(@RequestBody(required = false) BatchRequest request) {
        return R.ok(decomposeService.returnToRegister(request == null ? List.of() : request.bhs()));
    }

    public record BatchRequest(List<String> bhs) {
    }

    public record CompleteRequest(List<String> bhs, String library) {
    }

    public record GenerateRequest(List<String> bhs, String library, String taskno, String keyword) {
    }
}
