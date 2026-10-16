import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore, type DocumentData } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  callerQuotaDocumentId,
  consumeLookupQuota,
  contactDocumentId,
  isPhoneE164,
  normalizePersonName,
  type LookupQuota,
} from "./contactSecurity.js";

initializeApp();

const firestore = getFirestore();
const auth = getAuth();
const contactsPath = (uid: string) => firestore.collection("users").doc(uid).collection("contacts");

export const addContact = onCall(
  {
    enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== "true",
    maxInstances: 10,
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Se necesita una sesión activa.");

    const callerPhone = request.auth?.token.phone_number;
    if (typeof callerPhone !== "string" || !isPhoneE164(callerPhone)) {
      throw new HttpsError("failed-precondition", "La cuenta debe tener un teléfono verificado.");
    }

    const input = request.data as Record<string, unknown>;
    const displayName = normalizePersonName(input?.displayName);
    const surname = normalizePersonName(input?.surname);
    const phoneE164 = input?.phoneE164;
    if (!displayName || !surname || !isPhoneE164(phoneE164)) {
      throw new HttpsError("invalid-argument", "Los datos del contacto no son válidos.");
    }

    const contactId = contactDocumentId(phoneE164);
    const contactRef = contactsPath(uid).doc(contactId);
    const currentContact = await contactRef.get();
    if (currentContact.exists) {
      return { outcome: "duplicate", contact: publicContact(currentContact.id, currentContact.data()) };
    }

    const quotaRef = firestore.collection("contactLookupLimits").doc(callerQuotaDocumentId(uid));
    const nowMs = Date.now();
    const hasQuota = await firestore.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(quotaRef);
      const previous = snapshot.data() as Partial<LookupQuota> | undefined;
      const consumed = consumeLookupQuota(previous, nowMs);
      if (!consumed.allowed) return false;
      transaction.set(quotaRef, consumed.quota);
      return true;
    });
    if (!hasQuota) {
      throw new HttpsError("resource-exhausted", "Has alcanzado el límite temporal de altas. Inténtalo más tarde.");
    }

    let linkedUserId: string | null = null;
    try {
      const linkedUser = await auth.getUserByPhoneNumber(phoneE164);
      if (!linkedUser.disabled && linkedUser.uid !== uid) linkedUserId = linkedUser.uid;
    } catch (error) {
      if ((error as { code?: string }).code !== "auth/user-not-found") {
        throw new HttpsError("unavailable", "No se pudo comprobar la cuenta del contacto.");
      }
    }

    const contact = {
      displayName,
      surname,
      phoneE164,
      linkedUserId,
      status: linkedUserId === null ? "pending" : "linked",
      createdAt: FieldValue.serverTimestamp(),
    };
    const outcome = await firestore.runTransaction(async (transaction) => {
      const existing = await transaction.get(contactRef);
      if (existing.exists) {
        return { outcome: "duplicate" as const, contact: publicContact(existing.id, existing.data()) };
      }
      transaction.create(contactRef, contact);
      return {
        outcome: "added" as const,
        contact: { id: contactId, ...contact, createdAt: null },
      };
    });
    return outcome;
  },
);

function publicContact(
  id: string,
  data: DocumentData | undefined,
) {
  if (!data) throw new HttpsError("data-loss", "El contacto guardado no es válido.");
  return {
    id,
    displayName: data.displayName,
    surname: data.surname,
    phoneE164: data.phoneE164,
    linkedUserId: data.linkedUserId ?? null,
    status: data.status,
    createdAt: null,
  };
}