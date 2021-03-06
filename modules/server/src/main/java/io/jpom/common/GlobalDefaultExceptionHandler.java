/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.common;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.system.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;

/**
 * ??????????????????
 *
 * @author jiangzeyin
 * @date 2019/04/17
 */
@ControllerAdvice
public class GlobalDefaultExceptionHandler {

    /**
     * ????????????????????????
     *
     * @param request  ??????
     * @param response ??????
     * @param e        ??????
     */
    @ExceptionHandler({AuthorizeException.class, RuntimeException.class, Exception.class})
    public void delExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        if (e instanceof AuthorizeException) {
            AuthorizeException authorizeException = (AuthorizeException) e;
            ServletUtil.write(response, authorizeException.getJsonMessage().toString(), MediaType.APPLICATION_JSON_VALUE);
        } else if (e instanceof JpomRuntimeException) {
            DefaultSystemLog.getLog().error("global handle exception: {}", request.getRequestURI(), e.getCause());
            ServletUtil.write(response, JsonMessage.getString(500, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        } else {
            DefaultSystemLog.getLog().error("global handle exception: {}", request.getRequestURI(), e);
            boolean causedBy = ExceptionUtil.isCausedBy(e, AccessDeniedException.class);
            if (causedBy) {
                ServletUtil.write(response, JsonMessage.getString(500, "????????????????????????,??????????????????" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
                return;
            }
            ServletUtil.write(response, JsonMessage.getString(500, "???????????????" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    /**
     * ???????????????
     * <p>
     * ????????????????????????
     *
     * @param request  ??????
     * @param response ??????
     * @param e        ??????
     * @author jzy
     * @since 2021-08-01
     */
    @ExceptionHandler({AgentException.class})
    public void agentExceptionHandler(HttpServletRequest request, HttpServletResponse response, AgentException e) {
        Throwable cause = e.getCause();
        if (cause != null) {
            DefaultSystemLog.getLog().error("controller " + request.getRequestURI(), cause);
        }
        ServletUtil.write(response, JsonMessage.getString(405, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * ???????????????????????? (??????????????????????????????)
     *
     * @param request  ??????
     * @param response ??????
     * @param e        ??????
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, ValidateException.class})
    public void paramExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        if (!ConfigBean.getInstance().isPro()) {
            // ?????????????????????????????????
            DefaultSystemLog.getLog().error("controller " + request.getRequestURI(), e);
        }
        String message = e.getMessage();
        if (ObjectUtil.equals(message, ServerConfigBean.AUTHORIZE_TIME_OUT_CODE)) {
            ServletUtil.write(response, JsonMessage.getString(ServerConfigBean.AUTHORIZE_TIME_OUT_CODE, "?????????????????????,????????????"), MediaType.APPLICATION_JSON_VALUE);
        } else {
            ServletUtil.write(response, JsonMessage.getString(405, message), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMessageConversionException.class})
    @ResponseBody
    public JsonMessage<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        DefaultSystemLog.getLog().warn("??????????????????:{}", e.getMessage());
        return new JsonMessage<>(HttpStatus.EXPECTATION_FAILED.value(), "??????????????????????????????");
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    @ResponseBody
    public JsonMessage<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return new JsonMessage<>(HttpStatus.METHOD_NOT_ALLOWED.value(), "???????????????????????????", e.getMessage());
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    public void handleNoHandlerFoundException(HttpServletResponse response, NoHandlerFoundException e) {
        ServletUtil.write(response, JsonMessage.getString(HttpStatus.NOT_FOUND.value(), "???????????????????????????", e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
    }
}
