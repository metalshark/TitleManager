package io.puharesource.mc.titlemanager.api.variables;

import io.puharesource.mc.titlemanager.internal.APIProvider;
import io.puharesource.mc.titlemanager.internal.InternalsKt;
import io.puharesource.mc.titlemanager.internal.functionality.placeholder.Placeholder;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
public final class VariableManager {
    private final Map<Integer, VariableReplacer> replacers = new HashMap<>();
    private Map<String, VariableRule> rules = new HashMap<>();

    @Deprecated
    private void registerMethod(final Method method, int replacer, final Variable variable) {
        for (String var : variable.vars()) {
            APIProvider.INSTANCE.addPlaceholder(new Placeholder("") {
                @NotNull
                @Override
                public String getText(@NotNull Player player, @Nullable String value) {
                    try {
                        return method.invoke(replacers.get(replacer), player).toString();
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    return "";
                }
            });
        }
    }

    @Deprecated
    public void registerVariableReplacer(final VariableReplacer replacer) {
        int rReplacer = replacers.size();
        replacers.put(rReplacer, replacer);

        for (Method method : replacer.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Variable.class)) continue;
            Variable variable = null;
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (annotation instanceof Variable) {
                    variable = (Variable) annotation;
                    break;
                }
            }

            if (variable == null) continue;

            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 || params[0].equals(Player.class)) {
                registerMethod(method, rReplacer, variable);
            }
        }
    }

    @Deprecated
    public void registerRule(final String name, final VariableRule rule) {
        rules.put(name.toUpperCase().trim(), rule);
    }

    @Deprecated
    public VariableRule getRule(final String name) {
        return rules.get(name.toUpperCase().trim());
    }

    @Deprecated
    private Pattern getVariablePattern(final String var) {
        return Pattern.compile("[{](?i)" + var + "[:]\\d+[,]?(\\d+)?[}]");
    }

    @Deprecated
    public String replaceText(final Player player, String text) {
        return InternalsKt.getPluginInstance().replaceText(player, text);
    }
}