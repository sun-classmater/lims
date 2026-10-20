package com.lims;

import org.junit.jupiter.api.Test;

import com.lims.common.R;
import com.lims.common.ResultCode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LimsApplicationTests {

    @Test
    void successResponseHasExpectedCode() {
        R<String> response = R.ok("framework");

        assertEquals(ResultCode.SUCCESS.getCode(), response.getCode());
        assertEquals("framework", response.getData());
    }
}
