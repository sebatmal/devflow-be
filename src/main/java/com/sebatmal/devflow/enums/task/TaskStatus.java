package com.sebatmal.devflow.enums.task;

public enum TaskStatus {
    PLANNED,
    INPROGRESS,
    REVIEW,
    BLOCKED,
    MERGED,
    OPEN;

    // 완료(머지)된 작업은 AI 의존성 추천 대상에서 제외 — 활성 이슈만 트리 업데이트
    public boolean isActive() {
        return this != MERGED;
    }
}
