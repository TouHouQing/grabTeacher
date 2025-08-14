package com.touhouqing.grabteacherbackend.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResult<T> {
    private boolean success;
    private String message;
    private T data;
    private int code;

    public CommonResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public CommonResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(true, "操作成功", data);
    }

    public static <T> CommonResult<T> success(String message, T data) {
        return new CommonResult<>(true, message, data);
    }

    public static <T> CommonResult<T> error(String message) {
        return new CommonResult<>(false, message);
    }

    public static <T> CommonResult<T> error(int code, String message) {
        CommonResult<T> response = new CommonResult<>(false, message);
        response.setCode(code);
        return response;
    }
} 