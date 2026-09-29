package com.futureboundtech.enums;

/**
 * Kind of practice item. {@link #MCQ} questions carry options, a correct answer,
 * an explanation and are auto-gradable. {@link #CODING} items are problem-statement
 * library entries (input/output format, constraints, examples, tags) that are
 * browsed and studied — submitted code is never executed inside this application.
 */
public enum QuestionType {
    MCQ,
    CODING
}
