package com.mcmiddleearth.mcme.pvp.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CommandNewMapArgument implements ArgumentType<String> {

    static Set<String> options;
    Set<String> endings;
    public CommandNewMapArgument() {
        options = PVPCommand.getMapNames(); endings = new HashSet<>(Lists.newArrayList("-TS", "-TDM", "-IN", "-OITQ", "-TC", "-RB", "-FFA", "-DR", "-CTF"));
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String o = reader.readString();
        if (options.contains(o)) {
            throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Failed parsing during action evaluation")), new LiteralMessage("Failed parsing during action evaluation on action:" + o));
        }
        for(String ending : endings) {
            if (o.endsWith(ending))
                return o;
        }
        throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Failed parsing during action evaluation")), new LiteralMessage("Failed parsing during action evaluation on action:" + o));
    }

    @Override
    public Collection<String> getExamples() {
        return options;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        for (String ending : endings) {
            if (builder.getRemaining().contains("-")) {
                int n = builder.getRemaining().indexOf("-");
                if (ending.startsWith(builder.getRemaining().substring(n))) {
                    builder.suggest(builder.getRemaining().substring(0, n) + ending);
                }
            } else
                builder.suggest(builder.getRemaining() + ending);
        }
        return builder.buildFuture();
    }
    public static void UpdateOptions(){
        options = PVPCommand.getMapNames();
    }
}


