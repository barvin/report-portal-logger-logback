/*
 *    Copyright 2019 Maksym Barvinskyi <maksym@mbarvinskyi.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.qatools.rp.logback;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.qatools.rp.dto.generated.SaveLogRQ;
import org.qatools.rp.ReportPortalClient;
import org.qatools.rp.message.HashMarkSeparatedMessageParser;
import org.qatools.rp.message.MessageParser;
import org.qatools.rp.message.ReportPortalMessage;
import org.qatools.rp.message.TypeAwareByteSource;

import java.util.Date;
import java.util.UUID;

public class ReportPortalAppender extends AppenderBase<ILoggingEvent> {

    private static final MessageParser MESSAGE_PARSER = new HashMarkSeparatedMessageParser();
    private PatternLayoutEncoder encoder;

    @Override
    protected void append(final ILoggingEvent event) {
        ReportPortalClient.emitLog(itemId -> {
            final String message = event.getFormattedMessage();
            final String level = event.getLevel().toString();
            final Date time = new Date(event.getTimeStamp());

            SaveLogRQ rq = new SaveLogRQ();
            rq.setLevel(SaveLogRQ.LevelEnum.fromValue(level));
            rq.setTime(time);
            rq.setItemId(itemId);

            try {
                if (MESSAGE_PARSER.supports(message)) {
                    ReportPortalMessage rpMessage = MESSAGE_PARSER.parse(message);
                    TypeAwareByteSource data = rpMessage.getData();
                    SaveLogRQ.File file = new SaveLogRQ.File();
                    file.setContent(data.read());
                    file.setContentType(data.getMediaType());
                    file.setName(UUID.randomUUID().toString());

                    rq.setFile(file);
                    rq.setMessage(rpMessage.getMessage());
                } else {
                    rq.setMessage(encoder.getLayout().doLayout(event));
                }

            } catch (Exception e) {
                // skip
            }

            return rq;
        });
    }

    @Override
    public void start() {
        if (this.encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return;
        }
        this.encoder.start();
        super.start();
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }
}

