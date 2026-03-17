package com.azuredoom.classescore.gameplay.services.damage;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.data.PassiveType;

public class ClassDamageSystem extends DamageEventSystem {

    private static final Set<String> RANGED_TERMS = Set.of(
        "range",
        "ranged",
        "projectile",
        "arrow",
        "bow",
        "crossbow",
        "bolt",
        "shot",
        "bullet",
        "thrown",
        "throw",
        "missile"
    );

    @Override
    public void handle(
        int index,
        @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
        @NonNullDecl Damage damage
    ) {
        var isPlayer = archetypeChunk.getArchetype().contains(EntityModule.get().getPlayerComponentType());
        if (isPlayer)
            return;

        var holder = EntityUtils.toHolder(index, archetypeChunk);
        var victimNPCRef = holder.getComponent(Objects.requireNonNull(NPCEntity.getComponentType()));
        if (victimNPCRef == null)
            return;

        if (!(damage.getSource() instanceof Damage.EntitySource entitySource))
            return;

        var attackerRef = entitySource.getRef();
        if (!attackerRef.isValid())
            return;

        var playerRefAttacker = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (playerRefAttacker == null)
            return;

        var classService = ClassesCore.getClassService();
        if (classService == null)
            return;

        var playerClass = classService.getSelectedClassDefinition(playerRefAttacker.getUuid());
        if (playerClass.isEmpty())
            return;

        var passives = playerClass.get().passives();
        if (passives.isEmpty())
            return;

        var incoming = damage.getAmount();
        if (incoming <= 0f)
            return;

        var cause = damage.getCause();
        if (cause == null)
            return;

        var causeId = cause.getId();
        var causeIdLower = causeId == null ? "" : causeId.toLowerCase(Locale.ROOT);

        var isRanged = containsAny(causeIdLower, RANGED_TERMS);
        var isMelee = !isRanged;

        var multiplier = 1.0f;

        for (var passive : passives) {
            if (passive.type() != PassiveType.DAMAGE_MULTIPLIER) {
                continue;
            }
            var passiveId = passive.id();
            if (passiveId == null) {
                continue;
            }

            var passiveIdLower = passiveId.toLowerCase(Locale.ROOT);

            if (isRanged && containsAny(passiveIdLower, RANGED_TERMS)) {
                multiplier *= passive.value();
            } else if (isMelee && passiveIdLower.contains("melee")) {
                multiplier *= passive.value();
            }
        }

        damage.setAmount(Math.round(incoming * multiplier));
    }

    @NullableDecl
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    /**
     * Checks if the given text contains any of the specified terms, ignoring case sensitivity.
     *
     * @param text  the text to be searched; if null or blank, the method will return false
     * @param terms the set of terms to search for in the text; cannot be null
     * @return true if any term from the set is found within the text; false otherwise
     */
    private static boolean containsAny(String text, Set<String> terms) {
        if (text == null || text.isBlank()) {
            return false;
        }

        var lower = text.toLowerCase(Locale.ROOT);
        for (var term : terms) {
            if (lower.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
