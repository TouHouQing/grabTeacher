package com.touhouqing.grabteacherbackend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProgramChangedEvent extends ApplicationEvent {
    public enum ChangeType { CREATE, UPDATE, DELETE, STATUS, FLAGS }
    private final ChangeType changeType;
    private final Long countryId;
    private final Long stageId;

    public ProgramChangedEvent(Object source, ChangeType changeType, Long countryId, Long stageId) {
        super(source);
        this.changeType = changeType;
        this.countryId = countryId;
        this.stageId = stageId;
    }

}

