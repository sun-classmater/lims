package com.lims.controller;

import com.lims.common.R;
import com.lims.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// 审核签发 - 报告签发 / 检验汇总查询
@RestController
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/pending")
    public R<Map<String, Object>> pending(@RequestParam Map<String, String> params) {
        return R.ok(reviewService.pending(params));
    }

    @PostMapping("/issue")
    public R<Integer> issue(@RequestBody(required = false) BatchRequest request) {
        return R.ok(reviewService.issue(request == null ? List.of() : request.bhs()));
    }

    @PostMapping("/return-register")
    public R<Integer> returnRegister(@RequestBody(required = false) BatchRequest request) {
        return R.ok(reviewService.returnToRegister(request == null ? List.of() : request.bhs()));
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestParam Map<String, String> params) {
        return R.ok(reviewService.summary(params));
    }

    public record BatchRequest(List<String> bhs) {
    }
}
