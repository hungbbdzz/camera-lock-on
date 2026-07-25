package com.velorise.cameralockon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves real damage-bearing boxes for multipart entities without hard-coding one loader API.
 * Vanilla Ender Dragon parts and compatible modded multipart entities are discovered through
 * getSubEntities/getParts-style accessors or their part fields.
 */
public final class MultipartDamageBoxResolver {
    private static final String[] PART_METHOD_NAMES = {
            "getSubEntities", "getParts", "getPartEntities", "getPartsArray"
    };
    private static final Map<Class<?>, AccessPlan> ACCESS_PLANS = new ConcurrentHashMap<>();
    private static final int MAX_DAMAGE_BOXES = 12;

    private MultipartDamageBoxResolver() {}

    public static List<DamageBox> resolve(LivingEntity target) {
        if (target == null) return Collections.emptyList();

        IdentityHashMap<Entity, String> parts = new IdentityHashMap<>();

        // Direct vanilla path is mapping-safe in production; reflection remains as a compatibility
        // fallback for multipart entities supplied by other mods/loaders.
        if (target instanceof EnderDragon dragon) {
            EnderDragonPart[] dragonParts = dragon.getSubEntities();
            for (EnderDragonPart dragonPart : dragonParts) {
                parts.put(dragonPart, dragonPart.name);
            }
        }

        if (target.isMultipartEntity()) {
            PartEntity<?>[] loaderParts = target.getParts();
            if (loaderParts != null) {
                for (int i = 0; i < loaderParts.length; i++) {
                    parts.putIfAbsent(loaderParts[i], "multipart_part_" + i);
                }
            }
        }

        AccessPlan plan = ACCESS_PLANS.computeIfAbsent(target.getClass(), MultipartDamageBoxResolver::scanClass);
        plan.collect(target, parts);

        List<DamageBox> result = new ArrayList<>();
        VecCenter parentCenter = VecCenter.of(target.getBoundingBox());
        for (Map.Entry<Entity, String> entry : parts.entrySet()) {
            Entity part = entry.getKey();
            if (part == null || part == target || part.isRemoved() || !part.isPickable()) continue;

            AABB box = part.getBoundingBox();
            if (!isUsable(box)) continue;
            VecCenter center = VecCenter.of(box);
            if (center.distanceToSqr(parentCenter) > 96.0D * 96.0D) continue;

            String label = resolvePartLabel(part, entry.getValue());
            result.add(new DamageBox(part, box, label, priorityFor(label), true));
        }

        if (result.isEmpty()) {
            AABB ownBox = target.getBoundingBox();
            if (isUsable(ownBox)) {
                result.add(new DamageBox(target, ownBox, "body", 2.0D, false));
            }
            return result;
        }

        result.sort(Comparator.comparingDouble(DamageBox::priority).reversed());
        if (result.size() > MAX_DAMAGE_BOXES) {
            return new ArrayList<>(result.subList(0, MAX_DAMAGE_BOXES));
        }
        return result;
    }

    private static AccessPlan scanClass(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        List<Field> fields = new ArrayList<>();

        for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            for (Method method : cursor.getDeclaredMethods()) {
                if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())) continue;
                for (String acceptedName : PART_METHOD_NAMES) {
                    if (method.getName().equals(acceptedName)) {
                        makeAccessible(method);
                        methods.add(method);
                        break;
                    }
                }
            }

            for (Field field : cursor.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                String name = field.getName().toLowerCase(Locale.ROOT);
                Class<?> fieldType = field.getType();
                boolean entityType = Entity.class.isAssignableFrom(fieldType);
                boolean entityArray = fieldType.isArray()
                        && Entity.class.isAssignableFrom(fieldType.getComponentType());
                boolean namedLikePart = name.contains("part") || name.contains("subentit")
                        || name.contains("head") || name.contains("neck") || name.contains("body")
                        || name.contains("wing") || name.contains("tail");
                if (entityType || entityArray || namedLikePart) {
                    makeAccessible(field);
                    fields.add(field);
                }
            }
        }
        return new AccessPlan(methods, fields);
    }

    private static void makeAccessible(java.lang.reflect.AccessibleObject object) {
        try {
            object.setAccessible(true);
        } catch (RuntimeException ignored) {
            // A denied reflective member is simply skipped during collection.
        }
    }

    private static void collectValue(Object value, String label, LivingEntity target,
                                     IdentityHashMap<Entity, String> output) {
        if (value == null) return;
        if (value instanceof Entity entity) {
            if (entity != target) output.putIfAbsent(entity, label);
            return;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                collectValue(Array.get(value, i), label + "[" + i + "]", target, output);
            }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            int index = 0;
            for (Object element : iterable) {
                collectValue(element, label + "[" + index++ + "]", target, output);
            }
        }
    }

    private static String resolvePartLabel(Entity part, String fallback) {
        for (Class<?> cursor = part.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                if (field.getType() != String.class || Modifier.isStatic(field.getModifiers())) continue;
                String fieldName = field.getName().toLowerCase(Locale.ROOT);
                if (!fieldName.contains("name") && !fieldName.contains("part")) continue;
                try {
                    makeAccessible(field);
                    Object value = field.get(part);
                    if (value instanceof String text && !text.isBlank()) return text;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
        return fallback == null ? part.getClass().getSimpleName() : fallback;
    }

    private static double priorityFor(String label) {
        String normalized = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if (normalized.contains("head")) return 4.2D;
        if (normalized.contains("neck")) return 3.4D;
        if (normalized.contains("body") || normalized.contains("torso")) return 3.0D;
        if (normalized.contains("wing")) return 1.8D;
        if (normalized.contains("tail")) return 1.35D;
        return 2.2D;
    }

    private static boolean isUsable(AABB box) {
        if (box == null) return false;
        double width = box.maxX - box.minX;
        double height = box.maxY - box.minY;
        double depth = box.maxZ - box.minZ;
        return width > 1.0E-4D && height > 1.0E-4D && depth > 1.0E-4D
                && Double.isFinite(box.minX) && Double.isFinite(box.maxX)
                && Double.isFinite(box.minY) && Double.isFinite(box.maxY)
                && Double.isFinite(box.minZ) && Double.isFinite(box.maxZ);
    }

    public static final class DamageBox {
        private final Entity entity;
        private final AABB box;
        private final String label;
        private final double priority;
        private final boolean multipart;

        private DamageBox(Entity entity, AABB box, String label, double priority, boolean multipart) {
            this.entity = entity;
            this.box = box;
            this.label = label;
            this.priority = priority;
            this.multipart = multipart;
        }

        public Entity entity() { return entity; }
        public AABB box() { return box; }
        public String label() { return label; }
        public double priority() { return priority; }
        public boolean multipart() { return multipart; }
    }

    private static final class AccessPlan {
        private final List<Method> methods;
        private final List<Field> fields;

        private AccessPlan(List<Method> methods, List<Field> fields) {
            this.methods = methods;
            this.fields = fields;
        }

        private void collect(LivingEntity target, IdentityHashMap<Entity, String> output) {
            for (Method method : methods) {
                try {
                    collectValue(method.invoke(target), method.getName(), target, output);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
            for (Field field : fields) {
                try {
                    collectValue(field.get(target), field.getName(), target, output);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
    }

    private static final class VecCenter {
        private final double x;
        private final double y;
        private final double z;

        private VecCenter(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static VecCenter of(AABB box) {
            return new VecCenter((box.minX + box.maxX) * 0.5D,
                    (box.minY + box.maxY) * 0.5D,
                    (box.minZ + box.maxZ) * 0.5D);
        }

        private double distanceToSqr(VecCenter other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
