package com.lims.controller;

import com.lims.common.PageResult;
import com.lims.common.R;
import com.lims.service.LimsDataService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/data")
public class LimsDataController {

    private final LimsDataService limsDataService;

    public LimsDataController(LimsDataService limsDataService) {
        this.limsDataService = limsDataService;
    }

    @GetMapping("/{entity}")
    public R<PageResult<Map<String, Object>>> list(
            @PathVariable String entity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam Map<String, String> params
    ) {
        Map<String, String> filters = new HashMap<>(params);
        filters.remove("page");
        filters.remove("size");
        filters.remove("keyword");
        return R.ok(limsDataService.list(entity, filters, page, size, keyword));
    }

    @PostMapping("/{entity}")
    public R<Integer> create(
            @PathVariable String entity,
            @RequestBody Map<String, Object> body
    ) {
        return R.ok(limsDataService.create(entity, body));
    }

    @PutMapping("/{entity}")
    public R<Integer> update(
            @PathVariable String entity,
            @RequestBody Map<String, Object> body
    ) {
        return R.ok(limsDataService.update(entity, body));
    }

    @DeleteMapping("/{entity}")
    public R<Integer> delete(
            @PathVariable String entity,
            @RequestParam Map<String, String> params
    ) {
        return R.ok(limsDataService.delete(entity, params));
    }

    @GetMapping("/{entity}/export")
    public R<List<Map<String, Object>>> export(
            @PathVariable String entity,
            @RequestParam(required = false) String keyword,
            @RequestParam Map<String, String> params
    ) {
        Map<String, String> filters = new HashMap<>(params);
        filters.remove("keyword");
        return R.ok(limsDataService.export(entity, filters, keyword));
    }
}
