package nerdhub.cardinal.componentstest;

import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.BlockComponentProvider;
import nerdhub.cardinal.components.api.component.Component;
import nerdhub.cardinal.components.api.component.ComponentProvider;
import nerdhub.cardinal.components.api.util.provider.EmptyComponentProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.ViewableWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class VitalityCondenser extends Block implements BlockComponentProvider {
    public VitalityCondenser(Settings settings) {
        super(settings);
    }

    @Deprecated
    @Override
    public void onScheduledTick(BlockState state, World world, BlockPos pos, Random rand) {
        CardinalComponentsTest.VITA.get(world).transferTo(
                Objects.requireNonNull(this.getComponent(world, pos, CardinalComponentsTest.VITA, null)),
                1
        );
    }

    @Deprecated
    @Override
    public boolean activate(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitInfo) {
        // only on client side, to confirm that sync works
        if (world.isClient) {
            player.addChatMessage(new TranslatableText("componenttest:action.chunk_vitality",
                    CardinalComponentsTest.VITA.get(this.getChunkProvider(world, pos)).getVitality()), true);
        }
        return true;
    }

    @Override
    public <T extends Component> boolean hasComponent(BlockView blockView, BlockPos pos, ComponentType<T> type, @Nullable Direction side) {
        return getChunkProvider(blockView, pos).hasComponent(type);
    }

    @Nullable
    @Override
    public <T extends Component> T getComponent(BlockView blockView, BlockPos pos, ComponentType<T> type, @Nullable Direction side) {
        return getChunkProvider(blockView, pos).getComponent(type);
    }

    @Override
    public Set<ComponentType<?>> getComponentTypes(BlockView blockView, BlockPos pos, @Nullable Direction side) {
        return getChunkProvider(blockView, pos).getComponentTypes();
    }

    private ComponentProvider getChunkProvider(BlockView blockView, BlockPos pos) {
        if (blockView instanceof ViewableWorld) {
            return ComponentProvider.fromChunk(((ViewableWorld) blockView).getChunk(pos));
        }
        return EmptyComponentProvider.instance();
    }
}
