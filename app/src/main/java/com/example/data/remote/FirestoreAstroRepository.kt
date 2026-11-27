package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.HouseholdUserEntity
import com.example.data.local.MarketCheckoutEntity
import com.example.data.local.MovementType
import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.data.nfce.ParsedNfceItem
import com.example.data.repository.BackupRestoreSummary
import com.example.data.repository.CanonicalResolution
import com.example.data.repository.PosCheckoutEntry
import com.example.data.repository.ProductMatcher
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

class FirestoreAstroRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        private val idCounter = AtomicLong(1L)
        private val inviteRandom = SecureRandom()
        private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val BACKUP_FORMAT_VERSION = 1

        fun nextLocalId(): Long {
            val suffix = idCounter.getAndIncrement() % 1000L
            return (System.currentTimeMillis() * 1000L + suffix).coerceAtLeast(1L)
        }

        fun generateInviteCode(length: Int = 8): String {
            return buildString(length) {
                repeat(length) {
                    append(INVITE_ALPHABET[inviteRandom.nextInt(INVITE_ALPHABET.length)])
                }
            }
        }
    }

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("Você precisa entrar na sua conta para acessar a nuvem.")
        }
        return uid
    }

    private fun <T> Flow<T>.guardFirestoreStream(
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    private suspend fun commitChunkedOps(ops: List<(WriteBatch) -> Unit>) {
        if (ops.isEmpty()) return
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { op -> op(batch) }
            batch.commit().await()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun nonNullMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
        return mapOf(*pairs).filterValues { it != null } as Map<String, Any>
    }

    // ---- OBSERVAÇÃO EM TEMPO REAL (FIRESTORE SNAPSHOTS) ----

    fun observeUserProfile(uid: String): Flow<UserProfileDoc?> = flow {
        val cleanUid = requireUserId()
        check(cleanUid == uid) { "Sessão alterada; leitura bloqueada." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .guardFirestoreStream(uid, OperationType.GET, path)
        )
    }

    fun observeHousehold(householdId: String, uid: String): Flow<HouseholdDoc?> = flow {
        val cleanUid = requireUserId()
        check(cleanUid == uid) { "Sessão alterada; leitura bloqueada." }
        require(householdId.isNotBlank()) { "householdId obrigatório" }
        val path = "households/$householdId"
        emitAll(
            db.collection("households").document(householdId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(HouseholdDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .guardFirestoreStream(uid, OperationType.GET, path)
        )
    }

    fun observeMembers(householdId: String, uid: String): Flow<List<HouseholdUserEntity>> = flow {
        requireUserId()
        val path = "households/$householdId/members"
        emitAll(
            db.collection("households").document(householdId).collection("members")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(HouseholdMemberDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toEntity() }
                        .sortedBy { it.name.lowercase() }
                }
                .guardFirestoreStream(uid, OperationType.LIST, path)
        )
    }

    fun observeCategories(householdId: String, uid: String): Flow<List<CategoryEntity>> = flow {
        requireUserId()
        val path = "households/$householdId/categories"
        emitAll(
            db.collection("households").document(householdId).collection("categories")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CategoryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toEntity() }
                        .sortedBy { it.name.lowercase() }
                }
                .guardFirestoreStream(uid, OperationType.LIST, path)
        )
    }

    fun observeProducts(householdId: String, uid: String): Flow<List<ProductEntity>> = flow {
        requireUserId()
        val path = "households/$householdId/products"
        emitAll(
            db.collection("households").document(householdId).collection("products")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ProductDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toEntity() }
                        .sortedBy { it.name.lowercase() }
                }
                .guardFirestoreStream(uid, OperationType.LIST, path)
        )
    }

    fun observeAliases(householdId: String, uid: String): Flow<List<ProductAliasEntity>> = flow {
        requireUserId()
        val path = "households/$householdId/productAliases"
        emitAll(
            db.collection("households").document(householdId).collection("productAliases")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ProductAliasDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toEntity() }
                }
                .guardFirestoreStream(uid, OperationType.LIST, path)
        )
    }

    fun observeMovements(householdId: String, uid: String): Flow<List<StockMovementEntity>> = flow {
        requireUserId()
        val path = "households/$householdId/stockMovements"
        emitAll(
            db.collection("households").document(householdId).collection("stockMovements")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StockMovementDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toEntity() }
                        .sortedByDescending { it.timestampMillis }
                }
                .guardFirestoreStream(uid, OperationType.LIST, path)
        )
    }

    fun observeCheckouts(householdId: String, uid: String): Flow<List<MarketCheckoutEntity>> = flow {
        requireUserId()
        val path = "households/$householdId/marketCheckouts"
        emitAll(
            db.collection("households").document(householdId).collection("marketCheckouts")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MarketCheckoutDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toEntity() }
                        .sortedByDescending { it.timestampMillis }
                }
                .guardFirestoreStream(uid, OperationType.LIST, path)
        )
    }

    // ---- ESPAÇO PESSOAL & COMPARTILHAMENTO COM A FAMÍLIA ----

    suspend fun ensureUserWorkspace(
        displayName: String,
        email: String
    ): Result<String> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val userSnap = userRef.get().await()
            val cleanName = displayName.trim().ifBlank {
                email.substringBefore("@").ifBlank { "Morador" }
            }.take(100)
            val cleanEmail = email.trim().lowercase().take(160)

            if (userSnap.exists()) {
                val existingProfile = userSnap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val activeHid = existingProfile?.activeHouseholdId.orEmpty()
                if (activeHid.isNotBlank()) {
                    val hhSnap = runCatching {
                        db.collection("households").document(activeHid).get().await()
                    }.getOrNull()
                    val hh = hhSnap?.toObject(HouseholdDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (hh != null && uid in hh.memberUids) {
                        if (existingProfile?.displayName != cleanName || existingProfile.email != cleanEmail) {
                            userRef.update(
                                nonNullMap(
                                    "displayName" to cleanName,
                                    "email" to cleanEmail,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                        return Result.success(activeHid)
                    }
                }
            }

            // Cria espaço pessoal/familiar exclusivo para o usuário
            val householdId = createHouseholdInternal(
                uid = uid,
                displayName = cleanName,
                email = cleanEmail,
                householdName = "Casa de ${cleanName.substringBefore(" ").ifBlank { cleanName }}".take(80),
                userAlreadyExists = userSnap.exists()
            )
            Result.success(householdId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun createHouseholdInternal(
        uid: String,
        displayName: String,
        email: String,
        householdName: String,
        userAlreadyExists: Boolean
    ): String {
        val householdRef = db.collection("households").document()
        val householdId = householdRef.id
        val inviteCode = generateInviteCode(8)
        val inviteRef = db.collection("householdInvites").document(inviteCode)
        val userRef = db.collection("users").document(uid)
        val memberRef = householdRef.collection("members").document(uid)

        // 1. Cria a casa primeiro para que exists()/get() nas regras do Firestore
        // aprovem o convite, o membro administrador e as categorias na sequência.
        householdRef.set(
            nonNullMap(
                "ownerUid" to uid,
                "name" to householdName.take(80),
                "inviteCode" to inviteCode,
                "memberUids" to listOf(uid),
                "nextProductSeq" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // 2. Cria convite, membro administrador e categorias padrão
        val ops = mutableListOf<(WriteBatch) -> Unit>()

        ops.add { batch ->
            batch.set(
                inviteRef,
                nonNullMap(
                    "householdId" to householdId,
                    "householdName" to householdName.take(80),
                    "ownerUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        ops.add { batch ->
            batch.set(
                memberRef,
                nonNullMap(
                    "householdId" to householdId,
                    "memberUids" to listOf(uid),
                    "uid" to uid,
                    "localId" to nextLocalId(),
                    "name" to displayName.take(80),
                    "email" to email.take(160),
                    "role" to "Administrador",
                    "createdAtMillis" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        defaultCategorySpecs().forEach { spec ->
            val catId = nextLocalId()
            val catRef = householdRef.collection("categories").document(catId.toString())
            ops.add { batch ->
                batch.set(
                    catRef,
                    nonNullMap(
                        "householdId" to householdId,
                        "memberUids" to listOf(uid),
                        "localId" to catId,
                        "name" to spec.name,
                        "colorHex" to spec.colorHex,
                        "sectorNote" to spec.sectorNote,
                        "singleConsumption" to spec.singleConsumption,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        commitChunkedOps(ops)

        // 3. Atualiza/cria o perfil do usuário por último para que observeUserProfile
        // dispare observeHousehold somente após a casa existir no servidor.
        if (userAlreadyExists) {
            userRef.update(
                nonNullMap(
                    "displayName" to displayName.take(100),
                    "email" to email.take(160),
                    "activeHouseholdId" to householdId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            userRef.set(
                nonNullMap(
                    "uid" to uid,
                    "displayName" to displayName.take(100),
                    "email" to email.take(160),
                    "activeHouseholdId" to householdId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        return householdId
    }

    private data class DefaultCategorySpec(
        val name: String,
        val colorHex: Long,
        val sectorNote: String,
        val singleConsumption: Boolean = false
    )

    private fun defaultCategorySpecs(): List<DefaultCategorySpec> = listOf(
        DefaultCategorySpec("Despensa", 0xFF5B3DF5L, "Armários secos e grãos"),
        DefaultCategorySpec("Geladeira", 0xFF0284C7L, "Frios, laticínios e bebidas"),
        DefaultCategorySpec("Limpeza", 0xFF059669L, "Lavanderia e área de serviço"),
        DefaultCategorySpec("Higiene", 0xFFD97706L, "Banheiros e cuidados pessoais"),
        DefaultCategorySpec("Congelados", 0xFF7C3AEDL, "Freezer e salgados"),
        DefaultCategorySpec("Hortifruti", 0xFF16A34AL, "Frutas, verduras e legumes"),
        DefaultCategorySpec("Consumo Único", 0xFFDB2777L, "Lanches e itens consumidos na hora (sem alerta)", singleConsumption = true)
    )

    suspend fun createPersonalHousehold(
        displayName: String,
        email: String,
        customName: String? = null
    ): Result<String> {
        var path = "households"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().ifBlank {
                email.substringBefore("@").ifBlank { "Morador" }
            }.take(100)
            val cleanEmail = email.trim().lowercase().take(160)
            val targetName = customName?.trim()?.takeIf { it.isNotBlank() }?.take(80)
                ?: "Casa de ${cleanName.substringBefore(" ").ifBlank { cleanName }}".take(80)
            val userSnap = db.collection("users").document(uid).get().await()
            path = "users/$uid"
            val newHid = createHouseholdInternal(
                uid = uid,
                displayName = cleanName,
                email = cleanEmail,
                householdName = targetName,
                userAlreadyExists = userSnap.exists()
            )
            Result.success(newHid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun renameHousehold(household: HouseholdDoc, newName: String): Result<Unit> {
        val cleanName = newName.trim().take(80)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Informe um nome para a casa."))
        val path = "households/${household.docId}"
        return try {
            val uid = requireUserId()
            check(uid in household.memberUids) { "Apenas moradores da casa podem renomeá-la." }
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            val hhRef = db.collection("households").document(household.docId)
            ops.add { batch ->
                batch.update(
                    hhRef,
                    nonNullMap(
                        "name" to cleanName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (household.ownerUid == uid && household.inviteCode.isNotBlank()) {
                val inviteRef = db.collection("householdInvites").document(household.inviteCode)
                ops.add { batch ->
                    batch.update(
                        inviteRef,
                        nonNullMap(
                            "householdName" to cleanName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitChunkedOps(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun regenerateInviteCode(household: HouseholdDoc): Result<String> {
        val path = "households/${household.docId}"
        return try {
            val uid = requireUserId()
            check(household.ownerUid == uid) { "Apenas o criador da casa pode gerar um novo código." }
            val newCode = generateInviteCode(8)
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            if (household.inviteCode.isNotBlank()) {
                val oldInviteRef = db.collection("householdInvites").document(household.inviteCode)
                ops.add { batch -> batch.delete(oldInviteRef) }
            }
            val newInviteRef = db.collection("householdInvites").document(newCode)
            ops.add { batch ->
                batch.set(
                    newInviteRef,
                    nonNullMap(
                        "householdId" to household.docId,
                        "householdName" to household.name.take(80),
                        "ownerUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val hhRef = db.collection("households").document(household.docId)
            ops.add { batch ->
                batch.update(
                    hhRef,
                    nonNullMap(
                        "inviteCode" to newCode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            commitChunkedOps(ops)
            Result.success(newCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinHouseholdByInviteCode(
        rawCode: String,
        displayName: String,
        email: String
    ): Result<String> {
        val cleanCode = rawCode.trim().uppercase().replace("[^A-Z0-9]".toRegex(), "")
        if (cleanCode.length !in 6..16) {
            return Result.failure(IllegalArgumentException("Código inválido. Digite o código de 6 a 16 letras/números."))
        }
        var path = "householdInvites/$cleanCode"
        return try {
            val uid = requireUserId()
            val inviteSnap = db.collection("householdInvites").document(cleanCode).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("Código de convite não encontrado."))
            }
            val targetHouseholdId = inviteSnap.getString("householdId").orEmpty()
            require(targetHouseholdId.isNotBlank()) { "Convite sem casa vinculada." }
            path = "households/$targetHouseholdId"

            val hhRef = db.collection("households").document(targetHouseholdId)
            // Tenta adicionar uid aos memberUids (se já for membro, update pode manter ou ignorar)
            val alreadyMemberSnap = runCatching { hhRef.get().await() }.getOrNull()
            val alreadyInHouse = alreadyMemberSnap?.exists() == true &&
                (alreadyMemberSnap.get("memberUids") as? List<*>)?.contains(uid) == true

            if (!alreadyInHouse) {
                hhRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val updatedHouseSnap = hhRef.get().await()
            val updatedHousehold = updatedHouseSnap.toObject(
                HouseholdDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: error("Casa não encontrada após entrada.")

            val memberRef = hhRef.collection("members").document(uid)
            val existingMemberSnap = runCatching { memberRef.get().await() }.getOrNull()
            val cleanName = displayName.trim().ifBlank {
                email.substringBefore("@").ifBlank { "Morador" }
            }.take(80)
            val cleanEmail = email.trim().lowercase().take(160)

            if (existingMemberSnap != null && existingMemberSnap.exists()) {
                memberRef.update(
                    nonNullMap(
                        "name" to cleanName,
                        "email" to cleanEmail,
                        "memberUids" to updatedHousehold.memberUids,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                memberRef.set(
                    nonNullMap(
                        "householdId" to targetHouseholdId,
                        "memberUids" to updatedHousehold.memberUids,
                        "uid" to uid,
                        "localId" to nextLocalId(),
                        "name" to cleanName,
                        "email" to cleanEmail,
                        "role" to if (updatedHousehold.ownerUid == uid) "Administrador" else "Morador",
                        "createdAtMillis" to System.currentTimeMillis(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Atualiza workspace ativo do usuário
            db.collection("users").document(uid).update(
                nonNullMap(
                    "activeHouseholdId" to targetHouseholdId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(updatedHousehold.name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncMemberUidsAcrossHousehold(household: HouseholdDoc): Result<Int> {
        val path = "households/${household.docId}"
        return try {
            val uid = requireUserId()
            if (uid !in household.memberUids) return Result.success(0)
            val targetUids = household.memberUids.distinct().take(20)
            val hhRef = db.collection("households").document(household.docId)
            val collections = listOf("members", "categories", "products", "productAliases", "stockMovements", "marketCheckouts")
            val ops = mutableListOf<(WriteBatch) -> Unit>()

            collections.forEach { colName ->
                val snap = hhRef.collection(colName)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                snap.documents.forEach { doc ->
                    val current = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    if (current.toSet() != targetUids.toSet()) {
                        ops.add { batch ->
                            batch.update(
                                doc.reference,
                                nonNullMap(
                                    "memberUids" to targetUids,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                }
            }
            commitChunkedOps(ops)
            Result.success(ops.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---- CATEGORIAS ----

    suspend fun saveCategory(household: HouseholdDoc, category: CategoryEntity): Result<Long> {
        val cleanName = category.name.trim().take(80)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Nome da categoria obrigatório"))
        val isNew = category.id == 0L
        val localId = if (isNew) nextLocalId() else category.id
        val docRef = db.collection("households").document(household.docId)
            .collection("categories").document(localId.toString())
        return try {
            requireUserId()
            if (isNew) {
                docRef.set(
                    nonNullMap(
                        "householdId" to household.docId,
                        "memberUids" to household.memberUids,
                        "localId" to localId,
                        "name" to cleanName,
                        "colorHex" to category.colorHex,
                        "sectorNote" to category.sectorNote.trim().take(200),
                        "singleConsumption" to category.singleConsumption,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    nonNullMap(
                        "name" to cleanName,
                        "colorHex" to category.colorHex,
                        "sectorNote" to category.sectorNote.trim().take(200),
                        "singleConsumption" to category.singleConsumption,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(localId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(household: HouseholdDoc, categoryId: Long): Result<Unit> {
        val path = "households/${household.docId}/categories/$categoryId"
        return try {
            requireUserId()
            db.collection("households").document(household.docId)
                .collection("categories").document(categoryId.toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- PRODUTOS & MOVIMENTAÇÕES ----

    suspend fun saveProduct(
        household: HouseholdDoc,
        product: ProductEntity,
        existingProducts: List<ProductEntity>,
        existingAliases: List<ProductAliasEntity>,
        authorName: String
    ): Result<Long> {
        val cleanName = product.name.trim().take(160)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Nome do produto obrigatório"))
        val isNew = product.id == 0L
        val localId = if (isNew) nextLocalId() else product.id
        val docRef = db.collection("households").document(household.docId)
            .collection("products").document(localId.toString())

        return try {
            requireUserId()
            val previous = if (!isNew) existingProducts.firstOrNull { it.id == product.id } else null
            val assignedUniqueCode = product.uniqueCode.ifBlank {
                previous?.uniqueCode?.ifBlank { null }
                    ?: ProductMatcher.formatUniqueCode(ProductMatcher.nextUniqueSequence(existingProducts))
            }.take(32)
            val cleanCode = product.code.trim().take(64)
            val cleanUnit = product.unit.trim().ifBlank { "un" }.take(20)
            val cleanQty = product.quantity.coerceAtLeast(0.0)
            val cleanMin = product.minStock.coerceAtLeast(0.0)
            val cleanPrice = product.unitPrice.coerceAtLeast(0.0)
            val cleanNotes = product.notes.trim().take(500)
            val nowMillis = System.currentTimeMillis()
            val cleanAuthor = authorName.trim().ifBlank { "Morador" }.take(100)

            val ops = mutableListOf<(WriteBatch) -> Unit>()

            if (isNew) {
                ops.add { batch ->
                    batch.set(
                        docRef,
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to localId,
                            "uniqueCode" to assignedUniqueCode,
                            "code" to cleanCode,
                            "name" to cleanName,
                            "categoryId" to product.categoryId,
                            "unit" to cleanUnit,
                            "quantity" to cleanQty,
                            "minStock" to cleanMin,
                            "unitPrice" to cleanPrice,
                            "lastPaidPrice" to product.lastPaidPrice?.coerceAtLeast(0.0),
                            "expiryDateMillis" to product.expiryDateMillis?.coerceAtLeast(0L),
                            "notes" to cleanNotes,
                            "updatedAtMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                ops.add { batch ->
                    batch.update(
                        docRef,
                        mapOf(
                            "uniqueCode" to assignedUniqueCode,
                            "code" to cleanCode,
                            "name" to cleanName,
                            "categoryId" to product.categoryId,
                            "unit" to cleanUnit,
                            "quantity" to cleanQty,
                            "minStock" to cleanMin,
                            "unitPrice" to cleanPrice,
                            "lastPaidPrice" to product.lastPaidPrice?.coerceAtLeast(0.0),
                            "expiryDateMillis" to product.expiryDateMillis?.coerceAtLeast(0L),
                            "notes" to cleanNotes,
                            "updatedAtMillis" to nowMillis,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            if (cleanCode.isNotBlank() && existingAliases.none { it.externalCode.equals(cleanCode, ignoreCase = true) }) {
                val aliasId = nextLocalId()
                val aliasRef = db.collection("households").document(household.docId)
                    .collection("productAliases").document(aliasId.toString())
                ops.add { batch ->
                    batch.set(
                        aliasRef,
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to aliasId,
                            "productId" to localId,
                            "externalCode" to cleanCode,
                            "marketName" to "Catálogo",
                            "createdAtMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            if (isNew && cleanQty > 0.0) {
                val mvId = nextLocalId()
                val mvRef = db.collection("households").document(household.docId)
                    .collection("stockMovements").document(mvId.toString())
                ops.add { batch ->
                    batch.set(
                        mvRef,
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to mvId,
                            "productId" to localId,
                            "productNameSnapshot" to cleanName,
                            "productUnitSnapshot" to cleanUnit,
                            "type" to MovementType.ENTRADA.name,
                            "quantityDelta" to cleanQty,
                            "previousBalance" to 0.0,
                            "newBalance" to cleanQty,
                            "unitPrice" to cleanPrice,
                            "totalValue" to cleanQty * cleanPrice,
                            "reason" to "Cadastro inicial de estoque ($assignedUniqueCode)".take(300),
                            "authorName" to cleanAuthor,
                            "timestampMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else if (previous != null && abs(previous.quantity - cleanQty) > 0.0001) {
                val diff = cleanQty - previous.quantity
                val mvId = nextLocalId()
                val mvRef = db.collection("households").document(household.docId)
                    .collection("stockMovements").document(mvId.toString())
                ops.add { batch ->
                    batch.set(
                        mvRef,
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to mvId,
                            "productId" to localId,
                            "productNameSnapshot" to cleanName,
                            "productUnitSnapshot" to cleanUnit,
                            "type" to MovementType.AJUSTE.name,
                            "quantityDelta" to diff,
                            "previousBalance" to previous.quantity.coerceAtLeast(0.0),
                            "newBalance" to cleanQty,
                            "unitPrice" to cleanPrice,
                            "totalValue" to abs(diff) * cleanPrice,
                            "reason" to "Edição manual no catálogo ($assignedUniqueCode)".take(300),
                            "authorName" to cleanAuthor,
                            "timestampMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            commitChunkedOps(ops)
            Result.success(localId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(
        household: HouseholdDoc,
        productId: Long,
        existingAliases: List<ProductAliasEntity>,
        existingMovements: List<StockMovementEntity>
    ): Result<Unit> {
        val hhRef = db.collection("households").document(household.docId)
        val path = "${hhRef.path}/products/$productId"
        return try {
            requireUserId()
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            ops.add { batch ->
                batch.delete(hhRef.collection("products").document(productId.toString()))
            }
            existingAliases.filter { it.productId == productId }.forEach { alias ->
                ops.add { batch ->
                    batch.delete(hhRef.collection("productAliases").document(alias.id.toString()))
                }
            }
            existingMovements.filter { it.productId == productId }.forEach { mv ->
                ops.add { batch ->
                    batch.delete(hhRef.collection("stockMovements").document(mv.id.toString()))
                }
            }
            commitChunkedOps(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun linkAliasToProduct(
        household: HouseholdDoc,
        product: ProductEntity,
        externalCode: String,
        marketName: String,
        existingAliases: List<ProductAliasEntity>
    ): Result<Unit> {
        val cleanCode = externalCode.trim().take(64)
        if (cleanCode.isBlank()) return Result.success(Unit)
        val hhRef = db.collection("households").document(household.docId)
        val path = "${hhRef.path}/productAliases"
        return try {
            requireUserId()
            val nowMillis = System.currentTimeMillis()
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            if (existingAliases.none { it.externalCode.equals(cleanCode, ignoreCase = true) }) {
                val aliasId = nextLocalId()
                val aliasRef = hhRef.collection("productAliases").document(aliasId.toString())
                ops.add { batch ->
                    batch.set(
                        aliasRef,
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to aliasId,
                            "productId" to product.id,
                            "externalCode" to cleanCode,
                            "marketName" to marketName.trim().ifBlank { "Mercado" }.take(100),
                            "createdAtMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            if (product.code.isBlank()) {
                val prodRef = hhRef.collection("products").document(product.id.toString())
                ops.add { batch ->
                    batch.update(
                        prodRef,
                        nonNullMap(
                            "code" to cleanCode,
                            "updatedAtMillis" to nowMillis,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitChunkedOps(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveOrCreateCanonicalProduct(
        household: HouseholdDoc,
        existingProducts: List<ProductEntity>,
        existingAliases: List<ProductAliasEntity>,
        scannedCode: String,
        name: String,
        unit: String,
        unitPrice: Double,
        marketName: String,
        categoryId: Long?
    ): Result<CanonicalResolution> {
        val hhRef = db.collection("households").document(household.docId)
        val path = "${hhRef.path}/products"
        return try {
            requireUserId()
            val cleanCode = scannedCode.trim().take(64)
            val cleanName = name.trim().take(160)
            val nowMillis = System.currentTimeMillis()

            ProductMatcher.findByCode(existingProducts, existingAliases, cleanCode)?.let { matched ->
                return Result.success(CanonicalResolution(matched, "code", linkedNewAlias = false))
            }

            ProductMatcher.findByNormalizedName(existingProducts, cleanName)?.let { matched ->
                var linked = false
                if (cleanCode.isNotBlank() && existingAliases.none { it.externalCode.equals(cleanCode, ignoreCase = true) }) {
                    val aliasId = nextLocalId()
                    hhRef.collection("productAliases").document(aliasId.toString()).set(
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to aliasId,
                            "productId" to matched.id,
                            "externalCode" to cleanCode,
                            "marketName" to marketName.trim().ifBlank { "Mercado" }.take(100),
                            "createdAtMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    linked = true
                }
                return Result.success(CanonicalResolution(matched, "name", linkedNewAlias = linked))
            }

            val newId = nextLocalId()
            val newUniqueCode = ProductMatcher.formatUniqueCode(ProductMatcher.nextUniqueSequence(existingProducts))
            val cleanUnit = unit.trim().ifBlank { "un" }.take(20)
            val cleanPrice = unitPrice.coerceAtLeast(0.0)
            val ops = mutableListOf<(WriteBatch) -> Unit>()

            val prodRef = hhRef.collection("products").document(newId.toString())
            ops.add { batch ->
                batch.set(
                    prodRef,
                    nonNullMap(
                        "householdId" to household.docId,
                        "memberUids" to household.memberUids,
                        "localId" to newId,
                        "uniqueCode" to newUniqueCode,
                        "code" to cleanCode,
                        "name" to cleanName,
                        "categoryId" to categoryId,
                        "unit" to cleanUnit,
                        "quantity" to 0.0,
                        "minStock" to 1.0,
                        "unitPrice" to cleanPrice,
                        "notes" to "",
                        "updatedAtMillis" to nowMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (cleanCode.isNotBlank()) {
                val aliasId = nextLocalId()
                val aliasRef = hhRef.collection("productAliases").document(aliasId.toString())
                ops.add { batch ->
                    batch.set(
                        aliasRef,
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to aliasId,
                            "productId" to newId,
                            "externalCode" to cleanCode,
                            "marketName" to marketName.trim().ifBlank { "Mercado" }.take(100),
                            "createdAtMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            commitChunkedOps(ops)

            val created = ProductEntity(
                id = newId,
                uniqueCode = newUniqueCode,
                code = cleanCode,
                name = cleanName,
                categoryId = categoryId,
                unit = cleanUnit,
                quantity = 0.0,
                minStock = 1.0,
                unitPrice = cleanPrice,
                lastPaidPrice = null,
                updatedAt = nowMillis
            )
            Result.success(CanonicalResolution(created, "created", linkedNewAlias = cleanCode.isNotBlank()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun registerMovement(
        household: HouseholdDoc,
        product: ProductEntity,
        type: MovementType,
        amountOrTargetBalance: Double,
        unitPriceOverride: Double?,
        reason: String,
        authorName: String
    ): Result<Unit> {
        val hhRef = db.collection("households").document(household.docId)
        val prodRef = hhRef.collection("products").document(product.id.toString())
        return try {
            requireUserId()
            val previous = product.quantity.coerceAtLeast(0.0)
            val effectivePrice = (unitPriceOverride?.takeIf { it >= 0.0 } ?: product.unitPrice).coerceAtLeast(0.0)
            val newBalance = when (type) {
                MovementType.ENTRADA -> (previous + amountOrTargetBalance).coerceAtLeast(0.0)
                MovementType.SAIDA -> (previous - amountOrTargetBalance).coerceAtLeast(0.0)
                MovementType.AJUSTE -> amountOrTargetBalance.coerceAtLeast(0.0)
            }
            val delta = when (type) {
                MovementType.ENTRADA -> newBalance - previous
                MovementType.SAIDA -> -(previous - newBalance)
                MovementType.AJUSTE -> newBalance - previous
            }
            val updatedPrice = if (type == MovementType.ENTRADA && effectivePrice > 0.0) effectivePrice else product.unitPrice.coerceAtLeast(0.0)
            val updatedLastPaid = if (type == MovementType.ENTRADA && product.unitPrice > 0.0 && effectivePrice != product.unitPrice) {
                product.unitPrice.coerceAtLeast(0.0)
            } else {
                product.lastPaidPrice?.coerceAtLeast(0.0)
            }
            val nowMillis = System.currentTimeMillis()
            val mvId = nextLocalId()
            val mvRef = hhRef.collection("stockMovements").document(mvId.toString())
            val cleanAuthor = authorName.trim().ifBlank { "Morador" }.take(100)

            val ops = listOf<(WriteBatch) -> Unit>(
                { batch ->
                    batch.update(
                        prodRef,
                        mapOf(
                            "quantity" to newBalance,
                            "unitPrice" to updatedPrice,
                            "lastPaidPrice" to updatedLastPaid,
                            "updatedAtMillis" to nowMillis,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                },
                { batch ->
                    batch.set(
                        mvRef,
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to mvId,
                            "productId" to product.id,
                            "productNameSnapshot" to product.name.take(160),
                            "productUnitSnapshot" to product.unit.ifBlank { "un" }.take(20),
                            "type" to type.name,
                            "quantityDelta" to delta,
                            "previousBalance" to previous,
                            "newBalance" to newBalance,
                            "unitPrice" to effectivePrice,
                            "totalValue" to abs(delta) * effectivePrice,
                            "reason" to reason.trim().ifBlank { type.label }.take(300),
                            "authorName" to cleanAuthor,
                            "timestampMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            )
            commitChunkedOps(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, prodRef.path)
            Result.failure(e)
        }
    }

    suspend fun finalizeMarketPosCheckout(
        household: HouseholdDoc,
        existingProducts: List<ProductEntity>,
        existingAliases: List<ProductAliasEntity>,
        storeName: String,
        notes: String,
        entries: List<PosCheckoutEntry>,
        saveToStock: Boolean,
        authorName: String
    ): Result<Unit> {
        val validEntries = entries.filter { it.quantity > 0.0 && it.name.isNotBlank() }
        if (validEntries.isEmpty()) return Result.success(Unit)

        val hhRef = db.collection("households").document(household.docId)
        val path = "${hhRef.path}/marketCheckouts"
        return try {
            requireUserId()
            val cleanStore = storeName.trim().ifBlank { "Supermercado" }.take(120)
            val cleanNotes = notes.trim().take(500)
            val totalAmount = validEntries.sumOf { it.quantity * it.unitPrice.coerceAtLeast(0.0) }
            val nowMillis = System.currentTimeMillis()
            val cleanAuthor = authorName.trim().ifBlank { "Morador" }.take(100)

            val ops = mutableListOf<(WriteBatch) -> Unit>()
            val checkoutId = nextLocalId()
            ops.add { batch ->
                batch.set(
                    hhRef.collection("marketCheckouts").document(checkoutId.toString()),
                    nonNullMap(
                        "householdId" to household.docId,
                        "memberUids" to household.memberUids,
                        "localId" to checkoutId,
                        "storeName" to cleanStore,
                        "notes" to cleanNotes,
                        "totalAmount" to totalAmount,
                        "itemCount" to validEntries.size,
                        "savedToStock" to saveToStock,
                        "authorName" to cleanAuthor,
                        "timestampMillis" to nowMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (saveToStock) {
                val workingProducts = existingProducts.toMutableList()
                val workingAliases = existingAliases.toMutableList()
                var nextSeq = ProductMatcher.nextUniqueSequence(workingProducts)

                validEntries.forEach { entry ->
                    val cleanCode = entry.scannedCode.trim().take(64)
                    val match = entry.canonicalProductId?.let { id ->
                        workingProducts.firstOrNull { it.id == id }
                    } ?: ProductMatcher.findCanonicalMatch(workingProducts, workingAliases, cleanCode, entry.name)

                    val (targetProduct, prevBalance, seqUsed) = queueIncomingStockOps(
                        household = household,
                        ops = ops,
                        workingProducts = workingProducts,
                        workingAliases = workingAliases,
                        match = match,
                        nextSeq = nextSeq,
                        scannedCode = cleanCode,
                        name = entry.name,
                        unit = entry.unit,
                        quantity = entry.quantity,
                        unitPrice = entry.unitPrice.coerceAtLeast(0.0),
                        categoryId = entry.categoryId,
                        marketName = cleanStore,
                        timestampMillis = nowMillis
                    )
                    nextSeq = seqUsed

                    val effectivePrice = if (entry.unitPrice > 0.0) entry.unitPrice else targetProduct.unitPrice
                    val reason = buildList {
                        add("Frente de Caixa — $cleanStore (${targetProduct.uniqueCode})")
                        if (cleanNotes.isNotBlank()) add(cleanNotes)
                    }.joinToString(" • ").take(300)

                    val mvId = nextLocalId()
                    ops.add { batch ->
                        batch.set(
                            hhRef.collection("stockMovements").document(mvId.toString()),
                            nonNullMap(
                                "householdId" to household.docId,
                                "memberUids" to household.memberUids,
                                "localId" to mvId,
                                "productId" to targetProduct.id,
                                "productNameSnapshot" to targetProduct.name.take(160),
                                "productUnitSnapshot" to targetProduct.unit.take(20),
                                "type" to MovementType.ENTRADA.name,
                                "quantityDelta" to entry.quantity,
                                "previousBalance" to prevBalance,
                                "newBalance" to prevBalance + entry.quantity,
                                "unitPrice" to effectivePrice,
                                "totalValue" to entry.quantity * effectivePrice,
                                "reason" to reason,
                                "authorName" to cleanAuthor,
                                "timestampMillis" to nowMillis,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            commitChunkedOps(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun importNfceItems(
        household: HouseholdDoc,
        existingProducts: List<ProductEntity>,
        existingAliases: List<ProductAliasEntity>,
        items: List<ParsedNfceItem>,
        storeName: String?,
        defaultCategoryId: Long?,
        authorName: String
    ): Result<Int> {
        val selected = items.filter { it.selected && it.name.isNotBlank() && it.quantity > 0.0 }
        if (selected.isEmpty()) return Result.success(0)

        val hhRef = db.collection("households").document(household.docId)
        val path = "${hhRef.path}/products"
        return try {
            requireUserId()
            val nowMillis = System.currentTimeMillis()
            val workingProducts = existingProducts.toMutableList()
            val workingAliases = existingAliases.toMutableList()
            var nextSeq = ProductMatcher.nextUniqueSequence(workingProducts)
            val cleanStore = storeName?.trim().orEmpty().take(100)
            val reasonLabel = if (cleanStore.isNotBlank()) "Importação NFC-e ($cleanStore)" else "Importação Cupom / NFC-e"
            val cleanAuthor = authorName.trim().ifBlank { "Morador" }.take(100)
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            var count = 0

            selected.forEach { parsed ->
                val cleanCode = parsed.code.trim().take(64)
                val match = ProductMatcher.findCanonicalMatch(workingProducts, workingAliases, cleanCode, parsed.name)

                val (targetProduct, prevBalance, seqUsed) = queueIncomingStockOps(
                    household = household,
                    ops = ops,
                    workingProducts = workingProducts,
                    workingAliases = workingAliases,
                    match = match,
                    nextSeq = nextSeq,
                    scannedCode = cleanCode,
                    name = parsed.name,
                    unit = parsed.unit,
                    quantity = parsed.quantity,
                    unitPrice = parsed.unitPrice.coerceAtLeast(0.0),
                    categoryId = parsed.categoryId ?: defaultCategoryId,
                    marketName = cleanStore.ifBlank { "NFC-e" },
                    timestampMillis = nowMillis
                )
                nextSeq = seqUsed

                val effectivePrice = if (parsed.unitPrice > 0.0) parsed.unitPrice else targetProduct.unitPrice
                val mvId = nextLocalId()
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("stockMovements").document(mvId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to mvId,
                            "productId" to targetProduct.id,
                            "productNameSnapshot" to targetProduct.name.take(160),
                            "productUnitSnapshot" to targetProduct.unit.take(20),
                            "type" to MovementType.ENTRADA.name,
                            "quantityDelta" to parsed.quantity,
                            "previousBalance" to prevBalance,
                            "newBalance" to prevBalance + parsed.quantity,
                            "unitPrice" to effectivePrice,
                            "totalValue" to parsed.quantity * effectivePrice,
                            "reason" to "$reasonLabel (${targetProduct.uniqueCode})".take(300),
                            "authorName" to cleanAuthor,
                            "timestampMillis" to nowMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                count++
            }

            commitChunkedOps(ops)
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun queueIncomingStockOps(
        household: HouseholdDoc,
        ops: MutableList<(WriteBatch) -> Unit>,
        workingProducts: MutableList<ProductEntity>,
        workingAliases: MutableList<ProductAliasEntity>,
        match: ProductEntity?,
        nextSeq: Int,
        scannedCode: String,
        name: String,
        unit: String,
        quantity: Double,
        unitPrice: Double,
        categoryId: Long?,
        marketName: String,
        timestampMillis: Long
    ): Triple<ProductEntity, Double, Int> {
        val hhRef = db.collection("households").document(household.docId)
        var seq = nextSeq
        val targetProduct: ProductEntity
        val previousBalance: Double

        if (match != null) {
            previousBalance = match.quantity.coerceAtLeast(0.0)
            val prevPrice = if (match.unitPrice > 0.0) match.unitPrice else match.lastPaidPrice
            val newPrice = if (unitPrice > 0.0) unitPrice else match.unitPrice.coerceAtLeast(0.0)
            val newLastPaid = if (prevPrice != null && prevPrice != newPrice) prevPrice.coerceAtLeast(0.0) else match.lastPaidPrice?.coerceAtLeast(0.0)
            val updated = match.copy(
                quantity = previousBalance + quantity,
                unitPrice = newPrice,
                lastPaidPrice = newLastPaid,
                updatedAt = timestampMillis
            )
            val prodRef = hhRef.collection("products").document(match.id.toString())
            ops.add { batch ->
                batch.update(
                    prodRef,
                    mapOf(
                        "quantity" to updated.quantity,
                        "unitPrice" to updated.unitPrice,
                        "lastPaidPrice" to updated.lastPaidPrice,
                        "updatedAtMillis" to timestampMillis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val idx = workingProducts.indexOf(match)
            if (idx >= 0) workingProducts[idx] = updated
            targetProduct = updated
        } else {
            val uniqueCode = ProductMatcher.formatUniqueCode(seq++)
            previousBalance = 0.0
            val newId = nextLocalId()
            val cleanName = name.trim().take(160)
            val cleanUnit = unit.trim().ifBlank { "un" }.take(20)
            val created = ProductEntity(
                id = newId,
                uniqueCode = uniqueCode,
                code = scannedCode,
                name = cleanName,
                categoryId = categoryId,
                unit = cleanUnit,
                quantity = quantity.coerceAtLeast(0.0),
                minStock = 1.0,
                unitPrice = unitPrice.coerceAtLeast(0.0),
                lastPaidPrice = null,
                updatedAt = timestampMillis
            )
            val prodRef = hhRef.collection("products").document(newId.toString())
            ops.add { batch ->
                batch.set(
                    prodRef,
                    nonNullMap(
                        "householdId" to household.docId,
                        "memberUids" to household.memberUids,
                        "localId" to newId,
                        "uniqueCode" to uniqueCode,
                        "code" to scannedCode,
                        "name" to cleanName,
                        "categoryId" to categoryId,
                        "unit" to cleanUnit,
                        "quantity" to created.quantity,
                        "minStock" to 1.0,
                        "unitPrice" to created.unitPrice,
                        "notes" to "",
                        "updatedAtMillis" to timestampMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            workingProducts.add(created)
            targetProduct = created
        }

        if (scannedCode.isNotBlank() && workingAliases.none { it.externalCode.equals(scannedCode, ignoreCase = true) }) {
            val aliasId = nextLocalId()
            val alias = ProductAliasEntity(
                id = aliasId,
                productId = targetProduct.id,
                externalCode = scannedCode,
                marketName = marketName.take(100),
                createdAt = timestampMillis
            )
            val aliasRef = hhRef.collection("productAliases").document(aliasId.toString())
            ops.add { batch ->
                batch.set(
                    aliasRef,
                    nonNullMap(
                        "householdId" to household.docId,
                        "memberUids" to household.memberUids,
                        "localId" to aliasId,
                        "productId" to targetProduct.id,
                        "externalCode" to scannedCode,
                        "marketName" to marketName.take(100),
                        "createdAtMillis" to timestampMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            workingAliases.add(alias)
        }

        return Triple(targetProduct, previousBalance, seq)
    }

    // ---- MORADORES & OPERADORES ----

    suspend fun addOperatorMember(
        household: HouseholdDoc,
        name: String,
        email: String,
        role: String
    ): Result<Unit> {
        val cleanName = name.trim().take(80)
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Nome obrigatório"))
        val safeRole = if (role == "Administrador") "Administrador" else "Morador"
        val docId = "op_${nextLocalId()}"
        val memberRef = db.collection("households").document(household.docId)
            .collection("members").document(docId)
        return try {
            requireUserId()
            memberRef.set(
                nonNullMap(
                    "householdId" to household.docId,
                    "memberUids" to household.memberUids,
                    "uid" to "",
                    "localId" to nextLocalId(),
                    "name" to cleanName,
                    "email" to email.trim().lowercase().take(160),
                    "role" to safeRole,
                    "createdAtMillis" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, memberRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMember(household: HouseholdDoc, userLocalId: Long): Result<Unit> {
        val hhRef = db.collection("households").document(household.docId)
        val path = "${hhRef.path}/members"
        return try {
            val uid = requireUserId()
            val snap = hhRef.collection("members")
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
            val targetDoc = snap.documents.firstOrNull { doc ->
                (doc.getLong("localId") ?: 0L) == userLocalId
            } ?: return Result.success(Unit)
            val targetUid = targetDoc.getString("uid").orEmpty()
            if (targetUid == household.ownerUid) {
                return Result.failure(IllegalStateException("Não é possível remover o administrador principal da casa."))
            }
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            if (targetUid.isNotBlank() && targetUid in household.memberUids) {
                if (household.ownerUid != uid && targetUid != uid) {
                    return Result.failure(IllegalStateException("Apenas o criador da casa pode remover outro membro conectado."))
                }
                ops.add { batch ->
                    batch.update(
                        hhRef,
                        mapOf(
                            "memberUids" to FieldValue.arrayRemove(targetUid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            ops.add { batch -> batch.delete(targetDoc.reference) }
            commitChunkedOps(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- CATÁLOGO DE EXEMPLO, BACKUP JSON & MIGRAÇÃO LOCAL -> NUVEM ----

    suspend fun loadSampleCatalog(household: HouseholdDoc, authorName: String): Result<Unit> {
        val hhRef = db.collection("households").document(household.docId)
        return try {
            val uid = requireUserId()
            val cleanAuthor = authorName.trim().ifBlank { "Morador" }.take(100)
            val ops = mutableListOf<(WriteBatch) -> Unit>()

            // Limpa subcoleções de catálogo da casa atual
            for (col in listOf("marketCheckouts", "productAliases", "stockMovements", "products", "categories")) {
                val existingSnap = hhRef.collection(col)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                existingSnap.documents.forEach { doc ->
                    ops.add { batch -> batch.delete(doc.reference) }
                }
            }

            val despensaId = nextLocalId()
            val geladeiraId = nextLocalId()
            val limpezaId = nextLocalId()
            val higieneId = nextLocalId()
            val consumoUnicoId = nextLocalId()

            listOf(
                DefaultCategorySpec("Despensa", 0xFF5B3DF5L, "Armários secos e bebidas") to despensaId,
                DefaultCategorySpec("Geladeira", 0xFF0284C7L, "Frios e perecíveis") to geladeiraId,
                DefaultCategorySpec("Limpeza", 0xFF059669L, "Lavanderia e cozinha") to limpezaId,
                DefaultCategorySpec("Higiene", 0xFFD97706L, "Banheiros") to higieneId,
                DefaultCategorySpec("Consumo Único", 0xFFDB2777L, "Lanches e itens consumidos na hora (sem alerta)", singleConsumption = true) to consumoUnicoId
            ).forEach { (spec, catId) ->
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("categories").document(catId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to catId,
                            "name" to spec.name,
                            "colorHex" to spec.colorHex,
                            "sectorNote" to spec.sectorNote,
                            "singleConsumption" to spec.singleConsumption,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val now = System.currentTimeMillis()
            val day = 86_400_000L
            fun monthsAgo(months: Int): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -months)
                return cal.timeInMillis
            }

            val p1 = nextLocalId()
            val p2 = nextLocalId()
            val p3 = nextLocalId()
            val p4 = nextLocalId()
            val p5 = nextLocalId()
            val p6 = nextLocalId()

            val sampleProducts = listOf(
                ProductEntity(p1, "AST-0001", "7891000100103", "COXINHA 60GR", consumoUnicoId, "kg", 0.17, 0.0, 32.90, 30.50, now + 12 * day, updatedAt = now),
                ProductEntity(p2, "AST-0002", "7891000200202", "PAO DE QUEIJO", despensaId, "kg", 0.22, 1.0, 24.50, 26.00, now + 21 * day, updatedAt = now),
                ProductEntity(p3, "AST-0003", "7891149103508", "CERVEJA SKOL 350ML", despensaId, "un", 1.0, 6.0, 4.29, 3.99, null, updatedAt = now),
                ProductEntity(p4, "AST-0004", "7891000400401", "PASTEL FRITO DE CALABRESA C QUEIJO", consumoUnicoId, "un", 1.0, 0.0, 7.50, 7.50, now + 5 * day, updatedAt = now),
                ProductEntity(p5, "AST-0005", "7891024135006", "DETERGENTE NEUTRO 500ML", limpezaId, "un", 1.0, 3.0, 3.45, 3.89, null, updatedAt = now),
                ProductEntity(p6, "AST-0006", "7891150024601", "SABONETE LIQUIDO LAVANDA", higieneId, "un", 1.0, 2.0, 14.50, 13.90, null, updatedAt = now)
            )
            sampleProducts.forEach { prod ->
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("products").document(prod.id.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to prod.id,
                            "uniqueCode" to prod.uniqueCode,
                            "code" to prod.code,
                            "name" to prod.name,
                            "categoryId" to prod.categoryId,
                            "unit" to prod.unit,
                            "quantity" to prod.quantity,
                            "minStock" to prod.minStock,
                            "unitPrice" to prod.unitPrice,
                            "lastPaidPrice" to prod.lastPaidPrice,
                            "expiryDateMillis" to prod.expiryDateMillis,
                            "notes" to "",
                            "updatedAtMillis" to prod.updatedAt,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            listOf(
                Triple(p1, "7891000100103", "Angeloni"),
                Triple(p2, "7891000200202", "Angeloni"),
                Triple(p3, "7891149103508", "Bistek"),
                Triple(p3, "00035012", "Giassi"),
                Triple(p4, "7891000400401", "Padaria Central"),
                Triple(p5, "7891024135006", "Fort Atacadista"),
                Triple(p6, "7891150024601", "Farmácia")
            ).forEach { (prodId, extCode, market) ->
                val aliasId = nextLocalId()
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("productAliases").document(aliasId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to aliasId,
                            "productId" to prodId,
                            "externalCode" to extCode,
                            "marketName" to market,
                            "createdAtMillis" to now,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            listOf(
                StockMovementEntity(0, p1, "COXINHA 60GR", "kg", MovementType.ENTRADA, 1.0, 0.0, 1.0, 32.90, 32.90, "Compra semanal (AST-0001)", cleanAuthor, monthsAgo(1)),
                StockMovementEntity(0, p1, "COXINHA 60GR", "kg", MovementType.SAIDA, -0.83, 1.0, 0.17, 32.90, 27.31, "Consumo doméstico", cleanAuthor, now - 2 * day),
                StockMovementEntity(0, p2, "PAO DE QUEIJO", "kg", MovementType.ENTRADA, 1.0, 0.0, 1.0, 24.50, 24.50, "Reposição café da manhã (AST-0002)", cleanAuthor, monthsAgo(2)),
                StockMovementEntity(0, p3, "CERVEJA SKOL 350ML", "un", MovementType.ENTRADA, 6.0, 0.0, 6.0, 4.29, 25.74, "Compra mercado (AST-0003)", cleanAuthor, now - 5 * day),
                StockMovementEntity(0, p4, "PASTEL FRITO DE CALABRESA C QUEIJO", "un", MovementType.ENTRADA, 3.0, 0.0, 3.0, 7.50, 22.50, "Compra feira (AST-0004)", cleanAuthor, now - 3 * day),
                StockMovementEntity(0, p5, "DETERGENTE NEUTRO 500ML", "un", MovementType.SAIDA, -2.0, 3.0, 1.0, 3.45, 6.90, "Uso cozinha", cleanAuthor, now - 1 * day),
                StockMovementEntity(0, p6, "SABONETE LIQUIDO LAVANDA", "un", MovementType.ENTRADA, 2.0, 0.0, 2.0, 14.50, 29.00, "Compra farmácia (AST-0006)", cleanAuthor, monthsAgo(3))
            ).forEach { mv ->
                val mvId = nextLocalId()
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("stockMovements").document(mvId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to mvId,
                            "productId" to mv.productId,
                            "productNameSnapshot" to mv.productNameSnapshot,
                            "productUnitSnapshot" to mv.productUnitSnapshot,
                            "type" to mv.type.name,
                            "quantityDelta" to mv.quantityDelta,
                            "previousBalance" to mv.previousBalance,
                            "newBalance" to mv.newBalance,
                            "unitPrice" to mv.unitPrice,
                            "totalValue" to mv.totalValue,
                            "reason" to mv.reason,
                            "authorName" to mv.authorName,
                            "timestampMillis" to mv.timestampMillis,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            commitChunkedOps(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, hhRef.path)
            Result.failure(e)
        }
    }

    fun exportBackupJson(
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        aliases: List<ProductAliasEntity>,
        movements: List<StockMovementEntity>,
        checkouts: List<MarketCheckoutEntity>
    ): String {
        val root = JSONObject().apply {
            put("app", "AstroGestao")
            put("backupVersion", BACKUP_FORMAT_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())

            put("categories", JSONArray().apply {
                categories.forEach { cat ->
                    put(JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("colorHex", cat.colorHex)
                        put("sectorNote", cat.sectorNote)
                        put("singleConsumption", cat.singleConsumption)
                    })
                }
            })

            put("products", JSONArray().apply {
                products.forEach { prod ->
                    put(JSONObject().apply {
                        put("id", prod.id)
                        put("uniqueCode", prod.uniqueCode)
                        put("code", prod.code)
                        put("name", prod.name)
                        if (prod.categoryId != null) put("categoryId", prod.categoryId) else put("categoryId", JSONObject.NULL)
                        put("unit", prod.unit)
                        put("quantity", prod.quantity)
                        put("minStock", prod.minStock)
                        put("unitPrice", prod.unitPrice)
                        if (prod.lastPaidPrice != null) put("lastPaidPrice", prod.lastPaidPrice) else put("lastPaidPrice", JSONObject.NULL)
                        if (prod.expiryDateMillis != null) put("expiryDateMillis", prod.expiryDateMillis) else put("expiryDateMillis", JSONObject.NULL)
                        put("notes", prod.notes)
                        put("updatedAt", prod.updatedAt)
                    })
                }
            })

            put("aliases", JSONArray().apply {
                aliases.forEach { alias ->
                    put(JSONObject().apply {
                        put("productId", alias.productId)
                        put("externalCode", alias.externalCode)
                        put("marketName", alias.marketName)
                        put("createdAt", alias.createdAt)
                    })
                }
            })

            put("movements", JSONArray().apply {
                movements.forEach { mv ->
                    put(JSONObject().apply {
                        put("productId", mv.productId)
                        put("productNameSnapshot", mv.productNameSnapshot)
                        put("productUnitSnapshot", mv.productUnitSnapshot)
                        put("type", mv.type.name)
                        put("quantityDelta", mv.quantityDelta)
                        put("previousBalance", mv.previousBalance)
                        put("newBalance", mv.newBalance)
                        put("unitPrice", mv.unitPrice)
                        put("totalValue", mv.totalValue)
                        put("reason", mv.reason)
                        put("authorName", mv.authorName)
                        put("timestampMillis", mv.timestampMillis)
                    })
                }
            })

            put("checkouts", JSONArray().apply {
                checkouts.forEach { ck ->
                    put(JSONObject().apply {
                        put("storeName", ck.storeName)
                        put("notes", ck.notes)
                        put("totalAmount", ck.totalAmount)
                        put("itemCount", ck.itemCount)
                        put("savedToStock", ck.savedToStock)
                        put("authorName", ck.authorName)
                        put("timestampMillis", ck.timestampMillis)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun importBackupJson(household: HouseholdDoc, rawJson: String): Result<BackupRestoreSummary> {
        val hhRef = db.collection("households").document(household.docId)
        return try {
            val uid = requireUserId()
            require(rawJson.length <= 5_000_000) {
                "Arquivo de backup excede o tamanho máximo suportado (5 MB)."
            }
            val root = JSONObject(rawJson)
            require(root.optString("app") == "AstroGestao") {
                "Arquivo inválido: este JSON não é um backup do AstroGestão."
            }
            val version = root.optInt("backupVersion", 0)
            require(version in 1..BACKUP_FORMAT_VERSION) {
                "Versão de backup não suportada ($version)."
            }

            val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
            val productsJson = root.optJSONArray("products") ?: JSONArray()
            val aliasesJson = root.optJSONArray("aliases") ?: JSONArray()
            val movementsJson = root.optJSONArray("movements") ?: JSONArray()
            val checkoutsJson = root.optJSONArray("checkouts") ?: JSONArray()

            val ops = mutableListOf<(WriteBatch) -> Unit>()
            for (col in listOf("marketCheckouts", "productAliases", "stockMovements", "products", "categories")) {
                val snap = hhRef.collection(col)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                snap.documents.forEach { doc -> ops.add { batch -> batch.delete(doc.reference) } }
            }

            val categoryIdMap = mutableMapOf<Long, Long>()
            var catCount = 0
            for (i in 0 until categoriesJson.length()) {
                val obj = categoriesJson.getJSONObject(i)
                val oldId = obj.optLong("id", 0L)
                val name = obj.optString("name").trim().take(80)
                if (name.isBlank()) continue
                val newId = nextLocalId()
                if (oldId > 0L) categoryIdMap[oldId] = newId
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("categories").document(newId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to newId,
                            "name" to name,
                            "colorHex" to obj.optLong("colorHex", 0xFF5B3DF5L),
                            "sectorNote" to obj.optString("sectorNote", "").take(200),
                            "singleConsumption" to obj.optBoolean("singleConsumption", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                catCount++
            }

            val productIdMap = mutableMapOf<Long, Long>()
            val createdSeqList = mutableListOf<ProductEntity>()
            var nextSeq = 1
            var prodCount = 0
            for (i in 0 until productsJson.length()) {
                val obj = productsJson.getJSONObject(i)
                val oldId = obj.optLong("id", 0L)
                val name = obj.optString("name").trim().take(160)
                if (name.isBlank()) continue
                val oldCatId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId")
                val mappedCatId = oldCatId?.let { categoryIdMap[it] }
                val assignedUnique = obj.optString("uniqueCode").trim().ifBlank {
                    ProductMatcher.formatUniqueCode(nextSeq++)
                }.take(32)
                val newId = nextLocalId()
                val lastPaid = if (obj.isNull("lastPaidPrice")) null else obj.optDouble("lastPaidPrice").coerceAtLeast(0.0)
                val expiry = if (obj.isNull("expiryDateMillis")) null else obj.optLong("expiryDateMillis").coerceAtLeast(0L)
                val cleanCode = obj.optString("code", "").trim().take(64)
                val cleanUnit = obj.optString("unit", "un").ifBlank { "un" }.take(20)
                val qty = obj.optDouble("quantity", 0.0).coerceAtLeast(0.0)
                val minStock = obj.optDouble("minStock", 1.0).coerceAtLeast(0.0)
                val price = obj.optDouble("unitPrice", 0.0).coerceAtLeast(0.0)
                val notes = obj.optString("notes", "").take(500)
                val upd = obj.optLong("updatedAt", System.currentTimeMillis()).coerceAtLeast(0L)

                ops.add { batch ->
                    batch.set(
                        hhRef.collection("products").document(newId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to newId,
                            "uniqueCode" to assignedUnique,
                            "code" to cleanCode,
                            "name" to name,
                            "categoryId" to mappedCatId,
                            "unit" to cleanUnit,
                            "quantity" to qty,
                            "minStock" to minStock,
                            "unitPrice" to price,
                            "lastPaidPrice" to lastPaid,
                            "expiryDateMillis" to expiry,
                            "notes" to notes,
                            "updatedAtMillis" to upd,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                createdSeqList.add(ProductEntity(id = newId, uniqueCode = assignedUnique, name = name))
                nextSeq = maxOf(nextSeq, ProductMatcher.nextUniqueSequence(createdSeqList))
                if (oldId > 0L) productIdMap[oldId] = newId
                prodCount++
            }

            var aliasCount = 0
            for (i in 0 until aliasesJson.length()) {
                val obj = aliasesJson.getJSONObject(i)
                val mappedProdId = productIdMap[obj.optLong("productId", 0L)] ?: continue
                val extCode = obj.optString("externalCode").trim().take(64)
                if (extCode.isBlank()) continue
                val aliasId = nextLocalId()
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("productAliases").document(aliasId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to aliasId,
                            "productId" to mappedProdId,
                            "externalCode" to extCode,
                            "marketName" to obj.optString("marketName", "").take(100),
                            "createdAtMillis" to obj.optLong("createdAt", System.currentTimeMillis()).coerceAtLeast(0L),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                aliasCount++
            }

            var moveCount = 0
            for (i in 0 until movementsJson.length()) {
                val obj = movementsJson.getJSONObject(i)
                val mappedProdId = productIdMap[obj.optLong("productId", 0L)] ?: continue
                val typeName = obj.optString("type", MovementType.ENTRADA.name)
                val type = MovementType.entries.firstOrNull { it.name == typeName } ?: MovementType.ENTRADA
                val mvId = nextLocalId()
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("stockMovements").document(mvId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to mvId,
                            "productId" to mappedProdId,
                            "productNameSnapshot" to obj.optString("productNameSnapshot", "Produto").ifBlank { "Produto" }.take(160),
                            "productUnitSnapshot" to obj.optString("productUnitSnapshot", "un").ifBlank { "un" }.take(20),
                            "type" to type.name,
                            "quantityDelta" to obj.optDouble("quantityDelta", 0.0),
                            "previousBalance" to obj.optDouble("previousBalance", 0.0).coerceAtLeast(0.0),
                            "newBalance" to obj.optDouble("newBalance", 0.0).coerceAtLeast(0.0),
                            "unitPrice" to obj.optDouble("unitPrice", 0.0).coerceAtLeast(0.0),
                            "totalValue" to obj.optDouble("totalValue", 0.0).coerceAtLeast(0.0),
                            "reason" to obj.optString("reason", "").take(300),
                            "authorName" to obj.optString("authorName", "Morador").ifBlank { "Morador" }.take(100),
                            "timestampMillis" to obj.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(0L),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                moveCount++
            }

            var checkoutCount = 0
            for (i in 0 until checkoutsJson.length()) {
                val obj = checkoutsJson.getJSONObject(i)
                val ckId = nextLocalId()
                ops.add { batch ->
                    batch.set(
                        hhRef.collection("marketCheckouts").document(ckId.toString()),
                        nonNullMap(
                            "householdId" to household.docId,
                            "memberUids" to household.memberUids,
                            "localId" to ckId,
                            "storeName" to obj.optString("storeName", "Supermercado").ifBlank { "Supermercado" }.take(120),
                            "notes" to obj.optString("notes", "").take(500),
                            "totalAmount" to obj.optDouble("totalAmount", 0.0).coerceAtLeast(0.0),
                            "itemCount" to obj.optInt("itemCount", 0).coerceAtLeast(0),
                            "savedToStock" to obj.optBoolean("savedToStock", true),
                            "authorName" to obj.optString("authorName", "Morador").ifBlank { "Morador" }.take(100),
                            "timestampMillis" to obj.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(0L),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                checkoutCount++
            }

            commitChunkedOps(ops)
            Result.success(
                BackupRestoreSummary(
                    categoriesRestored = catCount,
                    productsRestored = prodCount,
                    aliasesRestored = aliasCount,
                    movementsRestored = moveCount,
                    checkoutsRestored = checkoutCount
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, hhRef.path)
            Result.failure(e)
        }
    }

    suspend fun hasLocalRoomProducts(localDb: AppDatabase): Boolean {
        return runCatching {
            localDb.productDao().getAllOnce().isNotEmpty()
        }.getOrDefault(false)
    }

    suspend fun migrateLocalRoomToCloud(
        household: HouseholdDoc,
        localDb: AppDatabase
    ): Result<BackupRestoreSummary> {
        return try {
            val cats = localDb.categoryDao().getAllOnce()
            val prods = localDb.productDao().getAllOnce()
            val aliases = localDb.productAliasDao().getAllOnce()
            val moves = localDb.stockMovementDao().getAllOnce()
            val checkouts = localDb.marketCheckoutDao().getAllOnce()
            if (prods.isEmpty() && cats.isEmpty() && moves.isEmpty() && checkouts.isEmpty()) {
                return Result.success(BackupRestoreSummary(0, 0, 0, 0, 0))
            }
            val json = exportBackupJson(cats, prods, aliases, moves, checkouts)
            importBackupJson(household, json)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAppVersionConfig(): Result<AppVersionConfigDoc?> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                return Result.success(null)
            }
            val snap = db.collection("appConfig").document("android").get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                Result.success(snap.toObject(AppVersionConfigDoc::class.java))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "appConfig/android")
            Result.failure(e)
        }
    }
}

