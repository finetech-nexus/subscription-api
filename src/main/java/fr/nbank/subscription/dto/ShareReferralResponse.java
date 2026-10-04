package fr.nbank.subscription.dto;

/** Result of sharing: the code that was shared, plus the next ready code. */
public record ShareReferralResponse(ReferralResponse shared, ReferralResponse next) {}
