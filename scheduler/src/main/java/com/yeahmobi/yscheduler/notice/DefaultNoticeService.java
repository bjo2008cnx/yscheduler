package com.yeahmobi.yscheduler.notice;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.yeahmobi.yscheduler.common.notice.EmailSender;
import com.yeahmobi.yscheduler.common.notice.Message;
import com.yeahmobi.yscheduler.common.notice.SmsSender;
import com.yeahmobi.yscheduler.model.User;
import com.yeahmobi.yscheduler.model.type.ScheduleType;

/**
 * @author Ryan Sun
 */

@Service
public class DefaultNoticeService implements NoticeService {

    private Log            LOGGER = LogFactory.getLog(DefaultNoticeService.class);

    @Autowired
    private ContentHelper  contentHelper;

    @Autowired
    private ReceiverHelper receiverHelper;

    @Autowired
    private EmailSender    emailSender;

    @Autowired
    private SmsSender      smsSender;

    @SuppressWarnings("unchecked")
    public void alert(String title, String content, List<User> to, boolean needSms) {
        try {
            List<String> emails = (List<String>) CollectionUtils.collect(to, new Transformer() {

                public Object transform(Object input) {
                    return ((User) input).getEmail();
                }
            });
            List<String> teles = (List<String>) CollectionUtils.collect(to, new Transformer() {

                public Object transform(Object input) {
                    return ((User) input).getTelephone();
                }
            });
            this.emailSender.send(new Message(title, content, emails));
            if (needSms) {
                this.smsSender.send(new Message(title, content, teles));
            }

        } catch (Exception e) {
            this.LOGGER.error(e.getMessage(), e);
        }
    }

    public void workflowFail(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.WORKFLOW);
        String title = this.contentHelper.generateTitle(id, ScheduleType.WORKFLOW, NoticeStatus.FAIL);
        String content = this.contentHelper.getContent(id, ScheduleType.WORKFLOW, NoticeStatus.FAIL);
        alert(title, content, to, true);
    }

    public void workflowTimeout(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.WORKFLOW);
        String title = this.contentHelper.generateTitle(id, ScheduleType.WORKFLOW, NoticeStatus.TIMEOUT);
        String content = this.contentHelper.getContent(id, ScheduleType.WORKFLOW, NoticeStatus.TIMEOUT);
        alert(title, content, to, false);
    }

    public void workflowSkip(long id, Date scheduleTime) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.WORKFLOW);
        String title = this.contentHelper.generateTitle(id, ScheduleType.WORKFLOW, NoticeStatus.SKIP);
        String content = this.contentHelper.getContent(id, ScheduleType.WORKFLOW, NoticeStatus.SKIP);
        alert(title, content, to, false);
    }

    public void workflowCanncel(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.WORKFLOW);
        String title = this.contentHelper.generateTitle(id, ScheduleType.WORKFLOW, NoticeStatus.CANCEL);
        String content = this.contentHelper.getContent(id, ScheduleType.WORKFLOW, NoticeStatus.CANCEL);
        alert(title, content, to, false);
    }

    public void workflowSuccess(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.WORKFLOW);
        String title = this.contentHelper.generateTitle(id, ScheduleType.WORKFLOW, NoticeStatus.SUCCESS);
        String content = this.contentHelper.getContent(id, ScheduleType.WORKFLOW, NoticeStatus.SUCCESS);
        alert(title, content, to, false);
    }

    public void taskFail(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.TASK);
        String title = this.contentHelper.generateTitle(id, ScheduleType.TASK, NoticeStatus.FAIL);
        String content = this.contentHelper.getContent(id, ScheduleType.TASK, NoticeStatus.FAIL);
        alert(title, content, to, true);
    }

    public void taskTimeout(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.TASK);
        String title = this.contentHelper.generateTitle(id, ScheduleType.TASK, NoticeStatus.TIMEOUT);
        String content = this.contentHelper.getContent(id, ScheduleType.TASK, NoticeStatus.TIMEOUT);
        alert(title, content, to, false);
    }

    public void taskSkip(long id, Date scheduleTime) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.TASK);
        String title = this.contentHelper.generateTitle(id, ScheduleType.TASK, NoticeStatus.SKIP);
        String content = this.contentHelper.getContent(id, ScheduleType.TASK, NoticeStatus.SKIP);
        alert(title, content, to, false);

    }

    public void taskCanncel(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.TASK);
        String title = this.contentHelper.generateTitle(id, ScheduleType.TASK, NoticeStatus.CANCEL);
        String content = this.contentHelper.getContent(id, ScheduleType.TASK, NoticeStatus.CANCEL);
        alert(title, content, to, false);
    }

    public void taskSuccess(long id) {
        List<User> to = this.receiverHelper.getReceivers(id, ScheduleType.TASK);
        String title = this.contentHelper.generateTitle(id, ScheduleType.TASK, NoticeStatus.SUCCESS);
        String content = this.contentHelper.getContent(id, ScheduleType.TASK, NoticeStatus.SUCCESS);
        alert(title, content, to, false);

    }

    public void alertInnerError(String msg) {
        alert("InnerError", msg, Lists.newArrayList(this.receiverHelper.getAdminReceiver()), true);
    }

}
