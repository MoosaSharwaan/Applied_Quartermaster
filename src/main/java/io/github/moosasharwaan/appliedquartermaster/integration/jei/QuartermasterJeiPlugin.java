package io.github.moosasharwaan.appliedquartermaster.integration.jei;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import io.github.moosasharwaan.appliedquartermaster.client.TabletScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional JEI support: drag any item from JEI onto a farm or plate on the tablet's Automation page to use it as the
 * icon, and keep JEI's item list clear of the tablet's left toolbar. Only loaded when JEI is installed.
 */
@JeiPlugin
public class QuartermasterJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return AppliedQuartermaster.id("jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(TabletScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(TabletScreen screen) {
                return screen.getExtraAreas();
            }
        });
        registration.addGhostIngredientHandler(TabletScreen.class, new IGhostIngredientHandler<>() {
            @Override
            public <I> List<Target<I>> getTargetsTyped(TabletScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
                var stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
                var targets = new ArrayList<Target<I>>();
                if (stack.isEmpty()) {
                    return targets;
                }
                for (var area : screen.getIconTargets()) {
                    targets.add(new Target<>() {
                        @Override
                        public Rect2i getArea() {
                            return area.area();
                        }

                        @Override
                        public void accept(I ignored) {
                            screen.setIconFromItem(area.entry(), stack);
                        }
                    });
                }
                return targets;
            }

            @Override
            public void onComplete() {
            }
        });
    }
}
