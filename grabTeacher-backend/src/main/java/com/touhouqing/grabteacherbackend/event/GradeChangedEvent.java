package com.touhouqing.grabteacherbackend.event;

import org.springframework.context.ApplicationEvent;

public class GradeChangedEvent extends ApplicationEvent {
    public enum ChangeType { CREATE, UPDATE, DELETE }
    private final ChangeType changeType;

    public GradeChangedEvent(Object source, ChangeType changeType) {
        super(source);
        this.changeType = changeType;
    }

    public ChangeType getChangeType() { return changeType; }
}

