package nerdhub.cardinal.components.api;

import nerdhub.cardinal.components.api.component.Component;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

import javax.annotation.Nullable;
import java.util.Set;

public interface BlockComponentProvider {

    /**
     * if this method returns {@code true}, then {@link #getComponent(BlockView, BlockPos, ComponentType, Direction)} <strong>must not</strong> return {@code null} for the same {@link ComponentType}
     *
     * @return whether or not this {@link BlockComponentProvider} can provide the desired component
     */
    <T extends Component> boolean hasComponent(BlockView blockView, BlockPos pos, ComponentType<T> type, @Nullable Direction side);

    /**
     * @return an instance of the requested component, or {@code null}
     */
    <T extends Component> T getComponent(BlockView blockView, BlockPos pos, ComponentType<T> type, @Nullable Direction side);

    /**
     * @return an <strong>immutable</strong> view of the component types exposed by this {@link BlockComponentProvider}
     */
    Set<ComponentType<? extends Component>> getComponentTypes(BlockView blockView, BlockPos pos, @Nullable Direction side);
}
