package com.todoapp.dto;

/**
 * Kinds of suggestions. The API returns the code, not a sentence: the frontend turns it into
 * text in the user's language (EN/VI), so the backend stays language-neutral.
 */
public enum RecommendationType {
    /** Planned for a past day and not done -> move it to today. */
    OVERDUE,
    /** Planned for today, not done, and the day is almost over -> move it to tomorrow. */
    CARRY_OVER,
    /** Planned for today, past midday, still under the progress threshold -> focus on it now. */
    FOCUS,
    /** Postponed many times -> probably too big for one day, split it. */
    SPLIT
}
