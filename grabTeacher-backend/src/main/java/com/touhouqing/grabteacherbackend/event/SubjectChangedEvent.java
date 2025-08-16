package com.touhouqing.grabteacherbackend.event;

import org.springframework.context.ApplicationEvent;

public class SubjectChangedEvent extends ApplicationEvent {
    public enum ChangeType { CREATE, UPDATE, DELETE }
    private final ChangeType changeType;

    public SubjectChangedEvent(Object source, ChangeType changeType) {
        super(source);
        this.changeType = changeType;
    }

    public ChangeType getChangeType() { return changeType; }
}

