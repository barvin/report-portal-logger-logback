/*
 *     Report Portal Logger Logback
 *     Copyright (C) 2018  Maksym Barvinskyi <maksym@mbarvinskyi.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.qatools.rp.logback;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
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
            rq.setLevel(level);
            rq.setLogTime(time);
            rq.setTestItemId(itemId);

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

