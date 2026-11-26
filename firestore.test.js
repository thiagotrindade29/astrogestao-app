const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const CHARLIE_UID = "charlie_uid_003";
const HOUSEHOLD_ID = "hh_alice_family_001";
const INVITE_CODE = "ASTRO7K9";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

function futureDate(msAhead = 60000) {
  return new Date(Date.now() + msAhead);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedAliceHouseholdWithData() {
  const ts = pastDate(20000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      activeHouseholdId: HOUSEHOLD_ID,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("households").doc(HOUSEHOLD_ID).set({
      ownerUid: ALICE_UID,
      name: "Casa da Alice",
      inviteCode: INVITE_CODE,
      memberUids: [ALICE_UID],
      nextProductSeq: 2,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("householdInvites").doc(INVITE_CODE).set({
      householdId: HOUSEHOLD_ID,
      householdName: "Casa da Alice",
      ownerUid: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("households").doc(HOUSEHOLD_ID).collection("members").doc(ALICE_UID).set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      uid: ALICE_UID,
      localId: 1001,
      name: "Alice",
      email: "alice@example.com",
      role: "Administrador",
      createdAtMillis: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("households").doc(HOUSEHOLD_ID).collection("categories").doc("10").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 10,
      name: "Despensa",
      colorHex: 0xff5b3df5,
      sectorNote: "Armários secos",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("100").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 100,
      uniqueCode: "AST-0001",
      code: "7891000100103",
      name: "Arroz 5kg",
      categoryId: 10,
      unit: "un",
      quantity: 2.0,
      minStock: 1.0,
      unitPrice: 24.9,
      notes: "",
      updatedAtMillis: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("appConfig").doc("android").set({
      latestVersionName: "1.2.0",
      minSupportedVersionName: "1.0.0",
      releaseNotes: "Melhorias no estoque e notificações",
      downloadUrl: "https://example.com/releases/v1.2.0",
      updatedAt: ts,
    });
  });
}

test("Unauthenticated users: denied on all collections", async () => {
  await seedAliceHouseholdWithData();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("appConfig").doc("android").get());
  await assertFails(unauthDb.collection("households").doc(HOUSEHOLD_ID).get());
  await assertFails(unauthDb.collection("householdInvites").doc(INVITE_CODE).get());
  await assertFails(
    unauthDb
      .collection("households")
      .doc(HOUSEHOLD_ID)
      .collection("products")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Hacker",
      email: "hacker@example.com",
      activeHouseholdId: HOUSEHOLD_ID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profiles: strict PII isolation and immutable uid/createdAt", async () => {
  await seedAliceHouseholdWithData();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Owner read & partial update succeed
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Silva",
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot read or write Alice's private user profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Pwned",
      updatedAt: pastDate(1000),
    })
  );

  // Alice cannot mutate immutable uid or createdAt or inject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      uid: BOB_UID,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Bob can create his own valid profile, but not with future timestamp
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      activeHouseholdId: "hh_bob_001",
      createdAt: futureDate(),
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      activeHouseholdId: "hh_bob_001",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Households & subcollections: cross-user isolation + Kotlin query filter enforcement", async () => {
  await seedAliceHouseholdWithData();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Filtered Kotlin queries succeed for Alice
  await assertSucceeds(
    aliceDb
      .collection("households")
      .doc(HOUSEHOLD_ID)
      .collection("products")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertSucceeds(
    aliceDb
      .collection("households")
      .doc(HOUSEHOLD_ID)
      .collection("categories")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );

  // Unfiltered list queries MUST fail
  await assertFails(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").get()
  );
  await assertFails(aliceDb.collection("householdInvites").get());

  // Non-member Charlie cannot read/query/write Alice's household or products
  await assertFails(charlieDb.collection("households").doc(HOUSEHOLD_ID).get());
  await assertFails(
    charlieDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("100").get()
  );
  await assertFails(
    charlieDb
      .collection("households")
      .doc(HOUSEHOLD_ID)
      .collection("products")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("households")
      .doc(HOUSEHOLD_ID)
      .collection("products")
      .doc("101")
      .set({
        householdId: HOUSEHOLD_ID,
        memberUids: [CHARLIE_UID],
        localId: 101,
        uniqueCode: "AST-0002",
        code: "",
        name: "Injected",
        unit: "un",
        quantity: 1.0,
        minStock: 1.0,
        unitPrice: 5.0,
        notes: "",
        updatedAtMillis: 1700000000000,
        createdAt: pastDate(),
        updatedAt: pastDate(),
      })
  );
});

test("Kotlin-mirrored creates & partial updates succeed and reject shadow/invalid payloads", async () => {
  await seedAliceHouseholdWithData();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();

  // Valid create for Product, Alias, Movement, MarketCheckout (no "id" key, optional nulls omitted)
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("102").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 102,
      uniqueCode: "AST-0002",
      code: "7891000200202",
      name: "Café 500g",
      unit: "un",
      quantity: 3.0,
      minStock: 1.0,
      unitPrice: 18.5,
      notes: "",
      updatedAtMillis: 1700000001000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject extra @DocumentId "id" key or negative quantity or shadow ghost field
  await assertFails(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("103").set({
      id: "103",
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 103,
      uniqueCode: "AST-0003",
      code: "",
      name: "Leite",
      unit: "un",
      quantity: 1.0,
      minStock: 1.0,
      unitPrice: 5.0,
      notes: "",
      updatedAtMillis: 1700000001000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("104").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 104,
      uniqueCode: "AST-0004",
      code: "",
      name: "Negativo",
      unit: "un",
      quantity: -1.0,
      minStock: 1.0,
      unitPrice: 5.0,
      notes: "",
      updatedAtMillis: 1700000001000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update on product succeeds (including clearing optional nullable fields to null); shadow ghost field or localId change fails
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("102").update({
      quantity: 4.0,
      unitPrice: 19.0,
      lastPaidPrice: 18.5,
      updatedAtMillis: 1700000002000,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("102").update({
      categoryId: null,
      lastPaidPrice: null,
      expiryDateMillis: null,
      updatedAtMillis: 1700000002500,
      updatedAt: pastDate(900),
    })
  );
  await assertFails(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("102").update({
      quantity: 5.0,
      isVerified: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("102").update({
      localId: 999,
      updatedAt: pastDate(1000),
    })
  );

  // Alias, Movement, MarketCheckout creates
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("productAliases").doc("201").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 201,
      productId: 102,
      externalCode: "INT-CAF-99",
      marketName: "Angeloni",
      createdAtMillis: 1700000002000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("stockMovements").doc("301").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 301,
      productId: 102,
      productNameSnapshot: "Café 500g",
      productUnitSnapshot: "un",
      type: "ENTRADA",
      quantityDelta: 1.0,
      previousBalance: 3.0,
      newBalance: 4.0,
      unitPrice: 19.0,
      totalValue: 19.0,
      reason: "Compra semanal",
      authorName: "Alice",
      timestampMillis: 1700000002000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("marketCheckouts").doc("401").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 401,
      storeName: "Angeloni",
      notes: "Compra mensal",
      totalAmount: 19.0,
      itemCount: 1,
      savedToStock: true,
      authorName: "Alice",
      timestampMillis: 1700000002000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Category singleConsumption create, partial update, and invalid type rejection
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("categories").doc("11").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 11,
      name: "Consumo Único",
      colorHex: 0xffdb2777,
      sectorNote: "Lanches consumidos na hora",
      singleConsumption: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("categories").doc("10").update({
      singleConsumption: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("categories").doc("12").set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID],
      localId: 12,
      name: "Inválida",
      colorHex: 0xffdb2777,
      sectorNote: "",
      singleConsumption: "sim",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Family sharing: Bob looks up invite code, joins household, receives synced access, and can leave", async () => {
  await seedAliceHouseholdWithData();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Bob looks up exact invite code (allowed); cannot list invites (denied)
  const inviteSnap = await assertSucceeds(
    bobDb.collection("householdInvites").doc(INVITE_CODE).get()
  );
  if (inviteSnap.data().householdId !== HOUSEHOLD_ID) {
    throw new Error("Invite householdId mismatch");
  }

  // Bob joins Alice's household (only adding himself to memberUids)
  await assertSucceeds(
    bobDb.collection("households").doc(HOUSEHOLD_ID).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(1000),
    })
  );

  // Charlie cannot force-add Bob or hijack memberUids
  await assertFails(
    charlieDb.collection("households").doc(HOUSEHOLD_ID).update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID, "extra_uid"],
      updatedAt: pastDate(500),
    })
  );

  // Bob registers himself in members subcollection
  await assertSucceeds(
    bobDb.collection("households").doc(HOUSEHOLD_ID).collection("members").doc(BOB_UID).set({
      householdId: HOUSEHOLD_ID,
      memberUids: [ALICE_UID, BOB_UID],
      uid: BOB_UID,
      localId: 1002,
      name: "Bob",
      email: "bob@example.com",
      role: "Morador",
      createdAtMillis: 1700000003000,
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Alice syncs memberUids onto existing products so Bob can query them cleanly
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("products").doc("100").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(500),
    })
  );

  // Now Bob's filtered query returns Alice's synced product
  const bobProducts = await assertSucceeds(
    bobDb
      .collection("households")
      .doc(HOUSEHOLD_ID)
      .collection("products")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  if (bobProducts.size !== 1) {
    throw new Error(`Expected 1 synced product for Bob, got ${bobProducts.size}`);
  }

  // Bob cannot remove Alice (owner), but Bob can leave the household
  await assertFails(
    bobDb.collection("households").doc(HOUSEHOLD_ID).update({
      memberUids: [BOB_UID],
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    bobDb.collection("households").doc(HOUSEHOLD_ID).update({
      memberUids: [ALICE_UID],
      updatedAt: pastDate(200),
    })
  );
});

test("App version config: signed-in users can read appConfig/android, but list and client writes are denied", async () => {
  await seedAliceHouseholdWithData();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const snap = await assertSucceeds(aliceDb.collection("appConfig").doc("android").get());
  if (snap.data().latestVersionName !== "1.2.0") {
    throw new Error("Expected latestVersionName 1.2.0");
  }

  await assertFails(aliceDb.collection("appConfig").doc("other").get());
  await assertFails(aliceDb.collection("appConfig").get());
  await assertFails(
    aliceDb.collection("appConfig").doc("android").set({
      latestVersionName: "9.9.9",
    })
  );
});
