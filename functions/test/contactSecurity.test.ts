import assert from "node:assert/strict";
import test from "node:test";
import {
  MAX_LOOKUPS_PER_DAY,
  MAX_LOOKUPS_PER_MINUTE,
  callerQuotaDocumentId,
  contactDocumentId,
  conversationDocumentId,
  consumeLookupQuota,
  isPhoneE164,
  normalizePersonName,
  type LookupQuota,
} from "../src/contactSecurity.js";

test("normalizes names and rejects non-name input", () => {
  assert.equal(normalizePersonName("  Ana   María  "), "Ana María");
  assert.equal(normalizePersonName("O'Neil"), "O'Neil");
  assert.equal(normalizePersonName("Nombre3"), null);
  assert.equal(normalizePersonName("   "), null);
});

test("accepts only canonical E.164 values", () => {
  assert.equal(isPhoneE164("+15555550100"), true);
  assert.equal(isPhoneE164("15555550100"), false);
  assert.equal(isPhoneE164("+0123456"), false);
  assert.equal(isPhoneE164("+1234567890123456"), false);
});

test("uses stable non-reversible identifiers", () => {
  assert.equal(contactDocumentId("+15555550100"), contactDocumentId("+15555550100"));
  assert.notEqual(contactDocumentId("+15555550100"), "+15555550100");
  assert.notEqual(callerQuotaDocumentId("uid-a"), "uid-a");
  assert.equal(conversationDocumentId("uid-a", "uid-b"), conversationDocumentId("uid-b", "uid-a"));
  assert.notEqual(conversationDocumentId("uid-a", "uid-b"), conversationDocumentId("uid-a", "uid-c"));
});

test("enforces per-minute and per-day lookup quotas", () => {
  let quota: Partial<LookupQuota> | undefined;
  const now = Date.UTC(2026, 9, 16, 12, 0);
  for (let index = 0; index < MAX_LOOKUPS_PER_MINUTE; index += 1) {
    const result = consumeLookupQuota(quota, now);
    assert.equal(result.allowed, true);
    quota = result.quota;
  }
  assert.equal(consumeLookupQuota(quota, now).allowed, false);

  quota = undefined;
  for (let index = 0; index < MAX_LOOKUPS_PER_DAY; index += 1) {
    const result = consumeLookupQuota(quota, now + index * 60_000);
    assert.equal(result.allowed, true);
    quota = result.quota;
  }
  assert.equal(consumeLookupQuota(quota, now + MAX_LOOKUPS_PER_DAY * 60_000).allowed, false);
});
