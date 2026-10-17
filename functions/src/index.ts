import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore, type DocumentData } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  callerQuotaDocumentId,
  consumeLookupQuota,
  contactDocumentId,
  conversationDocumentId,
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

export const openConversation = onCall(
  {
    enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== "true",
    maxInstances: 10,
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Debes iniciar sesión.");

    const contactId = (request.data as { contactId?: unknown } | undefined)?.contactId;
    if (typeof contactId !== "string" || !/^[0-9a-f]{64}$/.test(contactId)) {
      throw new HttpsError("invalid-argument", "El contacto no es válido.");
    }

    const contactSnapshot = await contactsPath(uid).doc(contactId).get();
    const contact = contactSnapshot.data();
    const linkedUserId = contact?.linkedUserId;
    if (!contactSnapshot.exists || contact?.status !== "linked" || typeof linkedUserId !== "string" || linkedUserId === uid) {
      throw new HttpsError("failed-precondition", "El contacto todavía no puede abrir una conversación.");
    }

    const [ownerProfile, linkedProfile] = await Promise.all([
      firestore.collection("users").doc(uid).get(),
      firestore.collection("users").doc(linkedUserId).get(),
    ]);
    if (!ownerProfile.exists || !linkedProfile.exists) {
      throw new HttpsError("failed-precondition", "Falta el perfil de uno de los participantes.");
    }

    const owner = ownerProfile.data();
    const linked = linkedProfile.data();
    const ownerDisplayName = normalizePersonName(owner?.displayName);
    const ownerSurname = normalizePersonName(owner?.surname);
    const contactDisplayName = normalizePersonName(contact?.displayName);
    const contactSurname = normalizePersonName(contact?.surname);
    if (!ownerDisplayName || !ownerSurname || !contactDisplayName || !contactSurname) {
      throw new HttpsError("data-loss", "Los perfiles de la conversación no son válidos.");
    }

    const conversationId = conversationDocumentId(uid, linkedUserId);
    const conversationRef = firestore.collection("conversations").doc(conversationId);
    await firestore.runTransaction(async (transaction) => {
      const existing = await transaction.get(conversationRef);
      if (existing.exists) {
        const participants = existing.data()?.participantIds;
        if (!Array.isArray(participants) || participants.length !== 2 || !participants.includes(uid) || !participants.includes(linkedUserId)) {
          throw new HttpsError("already-exists", "El identificador de conversación está ocupado.");
        }
        return;
      }

      const participantIds = [uid, linkedUserId].sort();
      transaction.create(conversationRef, {
        participantIds,
        participantDetails: {
          [uid]: {
            displayName: ownerDisplayName,
            surname: ownerSurname,
            avatarPath: typeof owner?.avatarPath === "string" ? owner.avatarPath : null,
          },
          [linkedUserId]: {
            displayName: contactDisplayName,
            surname: contactSurname,
            avatarPath: typeof linked?.avatarPath === "string" ? linked.avatarPath : null,
          },
        },
        unreadCountByUser: {
          [uid]: 0,
          [linkedUserId]: 0,
        },
        lastActivityAt: FieldValue.serverTimestamp(),
      });
    });

    return { conversationId };
  },
);

// Utilidad exclusiva de Emulator Suite para preparar estados que no forman
// parte del flujo bajo prueba. En producción siempre rechaza la llamada.
export const seedConversationForTests = onCall(
  { enforceAppCheck: false, maxInstances: 1 },
  async (request) => {
    if (process.env.FUNCTIONS_EMULATOR !== "true") {
      throw new HttpsError("failed-precondition", "Solo disponible en Emulator Suite.");
    }
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
    const input = request.data as {
      conversationId?: unknown;
      otherUserId?: unknown;
      currentUnreadCount?: unknown;
      otherUnreadCount?: unknown;
      activityMillis?: unknown;
    } | undefined;
    if (
      typeof input?.conversationId !== "string" ||
      !/^[A-Za-z0-9_-]{1,128}$/.test(input.conversationId) ||
      typeof input.otherUserId !== "string" ||
      input.otherUserId.length < 1 ||
      input.otherUserId.includes("/")
    ) {
      throw new HttpsError("invalid-argument", "Los datos de prueba no son válidos.");
    }
    const currentUnreadCount =
      typeof input.currentUnreadCount === "number" && input.currentUnreadCount >= 0 ? input.currentUnreadCount : 0;
    const otherUnreadCount =
      typeof input.otherUnreadCount === "number" && input.otherUnreadCount >= 0 ? input.otherUnreadCount : 0;
    const activity = typeof input.activityMillis === "number" ? new Date(input.activityMillis) : new Date();
    await firestore.collection("conversations").doc(input.conversationId).set({
      participantIds: [uid, input.otherUserId],
      participantDetails: {
        [uid]: { displayName: "Usuario", surname: "Ficticio", avatarPath: null },
        [input.otherUserId]: {
          displayName: `Persona ${input.otherUserId}`,
          surname: input.otherUserId,
          avatarPath: "avatars/ficticio.png",
        },
      },
      unreadCountByUser: { [uid]: currentUnreadCount, [input.otherUserId]: otherUnreadCount },
      lastActivityAt: activity,
    });
    return { conversationId: input.conversationId };
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
