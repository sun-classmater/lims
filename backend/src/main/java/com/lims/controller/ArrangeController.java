package com.lims.controller;

import com.lims.common.R;
import com.lims.service.ArrangeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 分解排样 - 任务安排 / 批量安排。
 */
@RestController
@RequestMapping("/arrange")
public class ArrangeController {

    private final ArrangeService arrangeService;

    public ArrangeController(ArrangeService arrangeService) {
        this.arrangeService = arrangeService;
    }

    /** 待安排检验项目，可按任务编号筛选 */
    @GetMapping("/pending")
    public R<Map<String, Object>> pending(
            @RequestParam(value = "taskno", required = false) String taskno
    ) {
        return R.ok(arrangeService.pending(taskno));
    }

    /** 已安排检验项目 */
    @GetMapping("/done")
    public R<Map<String, Object>> done() {
        return R.ok(arrangeService.done());
    }

    /** 安排检验任务：单条或批量 */
    @PostMapping("/assign")
    public R<Map<String, Object>> assign(@RequestBody(required = false) AssignRequest request) {
        AssignRequest payload = request == null ? new AssignRequest(null, null) : request;
        return R.ok(arrangeService.assign(payload.bhs(), payload.taskno()));
    }

    /** 转待安排 */
    @PostMapping("/revert")
    public R<Integer> revert(@RequestBody(required = false) BatchRequest request) {
        return R.ok(arrangeService.revert(request == null ? List.of() : request.bhs()));
    }

    /** 退分解 */
    @PostMapping("/return-decompose")
    public R<Integer> returnDecompose(@RequestBody(required = false) BatchRequest request) {
        return R.ok(arrangeService.returnDecompose(request == null ? List.of() : request.bhs()));
    }

    /** 退登记 */
    @PostMapping("/return-register")
    public R<Integer> returnRegister(@RequestBody(required = false) BatchRequest request) {
        return R.ok(arrangeService.returnToRegister(request == null ? List.of() : request.bhs()));
    }

    public record BatchRequest(List<String> bhs) {
    }

    public record AssignRequest(List<String> bhs, String taskno) {
    }
}
