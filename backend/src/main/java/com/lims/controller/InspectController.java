package com.lims.controller;

import com.lims.common.R;
import com.lims.service.InspectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 项目检验 - 任务查询 / 数据录入。
 */
@RestController
@RequestMapping("/inspect")
public class InspectController {

    private final InspectService inspectService;

    public InspectController(InspectService inspectService) {
        this.inspectService = inspectService;
    }

    /** 待检验检验任务列表，mine=true 只看当前用户负责的任务 */
    @GetMapping("/pending")
    public R<Map<String, Object>> pending(
            @RequestParam(value = "mine", defaultValue = "false") boolean mine,
            @RequestAttribute(value = "userId", required = false) String userId
    ) {
        return R.ok(inspectService.pendingTasks(mine, userId));
    }

    /** 已检验检验任务列表 */
    @GetMapping("/done")
    public R<Map<String, Object>> done(
            @RequestParam(value = "mine", defaultValue = "false") boolean mine,
            @RequestAttribute(value = "userId", required = false) String userId
    ) {
        return R.ok(inspectService.doneTasks(mine, userId));
    }

    /** 指定样品的检验项目明细 */
    @GetMapping("/items")
    public R<List<Map<String, Object>>> items(@RequestParam("bhs") List<String> bhs) {
        return R.ok(inspectService.items(bhs));
    }

    /** 保存检验数据，complete=true 时同时标记检验完成 */
    @PostMapping("/save")
    public R<Map<String, Object>> save(@RequestBody(required = false) SaveRequest request) {
        SaveRequest payload = request == null ? new SaveRequest(null, false) : request;
        return R.ok(inspectService.saveResults(payload.items(), Boolean.TRUE.equals(payload.complete())));
    }

    /** 转待检验 */
    @PostMapping("/revert")
    public R<Integer> revert(@RequestBody(required = false) BatchRequest request) {
        return R.ok(inspectService.revert(request == null ? List.of() : request.bhs()));
    }

    /** 退安排 */
    @PostMapping("/return-arrange")
    public R<Integer> returnArrange(@RequestBody(required = false) BatchRequest request) {
        return R.ok(inspectService.returnArrange(request == null ? List.of() : request.bhs()));
    }

    public record BatchRequest(List<String> bhs) {
    }

    public record SaveRequest(List<InspectService.ResultItem> items, Boolean complete) {
    }
}
