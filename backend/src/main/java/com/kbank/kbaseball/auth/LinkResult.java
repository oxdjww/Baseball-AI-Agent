package com.kbank.kbaseball.auth;

public sealed interface LinkResult {
    record Linked(String memberName) implements LinkResult {}
    record MemberNotFound()          implements LinkResult {}
    record TokenExpired()            implements LinkResult {}
}
