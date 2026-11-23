import { createRequire } from "node:module";
import { test, before, after, beforeEach } from "node:test";
import fs from "node:fs";

const require = createRequire(import.meta.url);
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write any AstroGestao collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_1")
      .set({
        name: "Arroz 5kg",
        location: "Despensa",
        unit: "pct",
        quantity: 2,
        minQuantity: 1,
        unitPrice: 29.9,
        previousUnitPrice: 29.9,
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Owner can create and update user profile, categories, products, movements, and purchases", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);
  const now = new Date(Date.now() - 1000);

  // User profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Maria Silva",
      email: "maria@example.com",
      seededDefaults: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Category create + partial update
  const catRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("categories")
    .doc("cat_1");
  await assertSucceeds(
    catRef.set({
      name: "Alimentos Secos",
      description: "Grãos e massas",
      color: "#f59e0b",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    catRef.update({
      name: "Alimentos",
      updatedAt: now,
    })
  );

  // Product create + partial update (with null optional fields omitted)
  const prodRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("products")
    .doc("prod_1");
  await assertSucceeds(
    prodRef.set({
      name: "Arroz Tipo 1 (5kg)",
      sku: "ALI-001",
      barcode: "7891000250787",
      categoryId: "cat_1",
      categoryName: "Alimentos",
      categoryColor: "#f59e0b",
      location: "Despensa",
      unit: "pct",
      quantity: 2,
      minQuantity: 1,
      unitPrice: 29.9,
      previousUnitPrice: 27.5,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    prodRef.update({
      quantity: 3,
      unitPrice: 31.0,
      previousUnitPrice: 29.9,
      updatedAt: now,
    })
  );

  // Movement create
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("movements")
      .doc("mov_1")
      .set({
        productId: "prod_1",
        productName: "Arroz Tipo 1 (5kg)",
        productSku: "ALI-001",
        productUnit: "pct",
        type: "IN",
        quantity: 1,
        previousQuantity: 2,
        newQuantity: 3,
        reason: "Compra no mercado",
        createdAt: now,
      })
  );

  // Purchase + PurchaseItem create
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("purchases")
      .doc("pur_1")
      .set({
        storeName: "Supermercado Central",
        notes: "Compra da semana",
        totalAmount: 31.0,
        itemsCount: 1,
        totalUnits: 1,
        createdAt: now,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("purchaseItems")
      .doc("pi_1")
      .set({
        purchaseId: "pur_1",
        productId: "prod_1",
        productName: "Arroz Tipo 1 (5kg)",
        barcode: "7891000250787",
        categoryName: "Alimentos",
        categoryColor: "#f59e0b",
        quantity: 1,
        unitPrice: 31.0,
        previousUnitPrice: 29.9,
        totalPrice: 31.0,
        createdAt: now,
      })
  );

  // List queries matching Kotlin repositories
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("movements").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("purchases").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("purchaseItems").get()
  );
});

test("Cross-user isolation and validation bounds reject unauthorized or malformed writes", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("products")
      .doc("bob_prod")
      .set({
        name: "Café Bob",
        location: "Despensa",
        unit: "pct",
        quantity: 1,
        minQuantity: 1,
        unitPrice: 18.0,
        previousUnitPrice: 18.0,
        createdAt: past,
        updatedAt: past,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Cross-user read/write denied
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("products")
      .doc("bob_prod")
      .get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("products").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("products")
      .doc("bob_prod")
      .update({ name: "Hackeado", updatedAt: new Date(Date.now() - 1000) })
  );

  // Ghost @DocumentId "id" field rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_with_id")
      .set({
        id: "prod_with_id",
        name: "Feijão",
        location: "Despensa",
        unit: "pct",
        quantity: 1,
        minQuantity: 1,
        unitPrice: 8.5,
        previousUnitPrice: 8.5,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Negative price or future timestamp rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_neg")
      .set({
        name: "Feijão",
        location: "Despensa",
        unit: "pct",
        quantity: 1,
        minQuantity: 1,
        unitPrice: -5,
        previousUnitPrice: 0,
        createdAt: past,
        updatedAt: past,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .doc("prod_future")
      .set({
        name: "Feijão",
        location: "Despensa",
        unit: "pct",
        quantity: 1,
        minQuantity: 1,
        unitPrice: 8.5,
        previousUnitPrice: 8.5,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: past,
      })
  );
});
