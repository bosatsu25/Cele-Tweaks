package io.github.bosatsu25.celetweaks;

import io.github.bosatsu25.celetweaks.mixin.integration.IntegrationMixinPlugin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link IntegrationMixinPlugin#countHandlerCalls}: the count CI relies on to notice a hook that
 * wove nothing. Partitions of a call instruction: our handler by its plain name, by the
 * {@code handler$...$name} name Mixin generates, a same-named method on another class, an unrelated
 * method on the target; plus a target with no calls at all.
 * <p>
 * Lives outside the {@code mixin} packages: Mixin owns the classes declared there and refuses to
 * load a test class placed among them.
 */
class HookCountTest {
    private static final String TARGET = "fi/dy/masa/example/Target";
    private static final Set<String> HANDLERS = Set.of("celetweaks$guard");

    @Test
    void aTargetWithoutCallsCountsZero() {
        ClassNode target = target(method("tick", new InsnNode(Opcodes.RETURN)));
        assertEquals(0, IntegrationMixinPlugin.countHandlerCalls(target, HANDLERS));
    }

    @Test
    void aPlainHandlerCallCounts() {
        ClassNode target = target(method("tick", call(TARGET, "celetweaks$guard")));
        assertEquals(1, IntegrationMixinPlugin.countHandlerCalls(target, HANDLERS));
    }

    @Test
    void aMixinRenamedHandlerCallCounts() {
        ClassNode target = target(method("tick", call(TARGET, "handler$zza000$celetweaks$guard")));
        assertEquals(1, IntegrationMixinPlugin.countHandlerCalls(target, HANDLERS));
    }

    @Test
    void callsToOtherClassesOrOtherMethodsDoNotCount() {
        ClassNode target = target(method("tick",
                call("some/other/Owner", "celetweaks$guard"),
                call(TARGET, "celetweaks$other"),
                call(TARGET, "guard"),
                call(TARGET, "tick")));
        assertEquals(0, IntegrationMixinPlugin.countHandlerCalls(target, HANDLERS));
    }

    @Test
    void callsAreSummedAcrossMethodsAndHandlers() {
        ClassNode target = target(
                method("tick", call(TARGET, "celetweaks$guard"), call(TARGET, "celetweaks$guard")),
                method("render", call(TARGET, "handler$abc$celetweaks$guard"), call(TARGET, "celetweaks$other")));
        assertEquals(3, IntegrationMixinPlugin.countHandlerCalls(target, HANDLERS));
        assertEquals(4, IntegrationMixinPlugin.countHandlerCalls(target, Set.of("celetweaks$guard", "celetweaks$other")));
    }

    private static ClassNode target(MethodNode... methods) {
        ClassNode node = new ClassNode();
        node.name = TARGET;
        for (MethodNode method : methods) node.methods.add(method);
        return node;
    }

    private static MethodNode method(String name, org.objectweb.asm.tree.AbstractInsnNode... instructions) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, name, "()V", null, null);
        for (org.objectweb.asm.tree.AbstractInsnNode insn : instructions) method.instructions.add(insn);
        return method;
    }

    private static MethodInsnNode call(String owner, String name) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, "()V", false);
    }
}
