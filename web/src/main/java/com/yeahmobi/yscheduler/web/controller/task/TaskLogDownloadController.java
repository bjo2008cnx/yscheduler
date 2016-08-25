package com.yeahmobi.yscheduler.web.controller.task;

import com.yeahmobi.yscheduler.common.fileserver.LocalFileBasedFileServer;
import com.yeahmobi.yscheduler.web.controller.AbstractController;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @author wukezhu
 */
@Controller
@RequestMapping
public class TaskLogDownloadController extends AbstractController {

    @Autowired
    private LocalFileBasedFileServer fileServer;

    /**
     * 下载global配置文件 （直接获取global对象，渲染成Velocity即可下载）
     */
    @SuppressWarnings("resource")
    @RequestMapping(value = "/**/*.log", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.addHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);

        String uri = request.getRequestURI();

        String contextPath = request.getContextPath();

        String downloadLink = uri.substring(contextPath.length());

        File file = this.fileServer.getFileFromDownloadLink(downloadLink);

        FileChannel inputChannel = new FileInputStream(file).getChannel();

        WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream());

        inputChannel.transferTo(0, Long.MAX_VALUE, outputChannel);

        IOUtils.closeQuietly(outputChannel);
        IOUtils.closeQuietly(inputChannel);

    }
}
