import { createHash } from "node:crypto";

export const MAX_LOOKUPS_PER_MINUTE = 5;
export const MAX_LOOKUPS_PER_DAY = 20;
const MINUTE_MS = 60_000;
const DAY_MS = 24 * 60 * MINUTE_MS;

export interface LookupQuota {
  minuteStartMs: number;
  minuteCount: number;
  dayStartMs: number;
  dayCount: number;
}

export function normalizePersonName(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const normalized = value.normalize("NFC").trim().replace(/\s+/gu, " ");
  const length = [...normalized].length;
  if (length < 1 || length > 50) return null;
  if (!/^\p{L}[\p{L}\p{M}]*(?:[ '\u2019.-][\p{L}\p{M}]+)*$/u.test(normalized)) return null;
  return normalized;
}

export function isPhoneE164(value: unknown): value is string {
  return typeof value === "string" && /^\+[1-9][0-9]{6,14}$/.test(value);
}

export function contactDocumentId(phoneE164: string): string {
  return createHash("sha256").update(phoneE164, "utf8").digest("hex");
}

export function conversationDocumentId(firstUid: string, secondUid: string): string {
  return createHash("sha256")
    .update([firstUid, secondUid].sort().join("\u0000"), "utf8")
    .digest("hex");
}

export function callerQuotaDocumentId(uid: string): string {
  return createHash("sha256").update(uid, "utf8").digest("hex");
}

export function consumeLookupQuota(
  previous: Partial<LookupQuota> | undefined,
  nowMs: number,
): { allowed: boolean; quota: LookupQuota } {
  const minuteStartMs = Math.floor(nowMs / MINUTE_MS) * MINUTE_MS;
  const dayStartMs = Math.floor(nowMs / DAY_MS) * DAY_MS;
  const minuteCount = previous?.minuteStartMs === minuteStartMs ? previous.minuteCount ?? 0 : 0;
  const dayCount = previous?.dayStartMs === dayStartMs ? previous.dayCount ?? 0 : 0;
  const allowed = minuteCount < MAX_LOOKUPS_PER_MINUTE && dayCount < MAX_LOOKUPS_PER_DAY;
  return {
    allowed,
    quota: {
      minuteStartMs,
      minuteCount: minuteCount + (allowed ? 1 : 0),
      dayStartMs,
      dayCount: dayCount + (allowed ? 1 : 0),
    },
  };
}
