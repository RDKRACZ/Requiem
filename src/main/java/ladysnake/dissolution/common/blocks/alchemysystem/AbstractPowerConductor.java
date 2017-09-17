package ladysnake.dissolution.common.blocks.alchemysystem;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import ladysnake.dissolution.common.blocks.alchemysystem.IPowerConductor.IMachine.PowerConsumption;
import ladysnake.dissolution.common.registries.modularsetups.ISetupInstance;
import ladysnake.dissolution.common.registries.modularsetups.SetupPowerGenerator;
import ladysnake.dissolution.common.tileentities.TileEntityModularMachine;
import ladysnake.dissolution.common.tileentities.TileEntityProxy;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class AbstractPowerConductor extends Block implements IPowerConductor {

	public AbstractPowerConductor() {
		super(Material.CIRCUITS);
		this.setDefaultState(this.blockState.getBaseState().withProperty(POWERED, false));
	}
	
	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
		super.onBlockHarvested(worldIn, pos, state, player);
		if(!worldIn.isRemote && this.isPowered(worldIn, pos)) {
			updatePowerCore(worldIn, pos);
		}
	}
	
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer,
			ItemStack stack) {
		if(!worldIn.isRemote)
			SetupPowerGenerator.THREADPOOL.submit(() -> updatePowerCore(worldIn, pos));
		worldIn.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
	}
	
	public static void updatePowerCore(World world, BlockPos pos) {
		scan(world, pos, new LinkedList<>(), 0).ifPresent(bp -> {
			if(world.getTileEntity(bp) instanceof TileEntityProxy)
				bp = bp.down();
			if(world.getTileEntity(bp) instanceof TileEntityModularMachine) {
				ISetupInstance setup = ((TileEntityModularMachine)world.getTileEntity(bp)).getCurrentSetup();
				if(setup instanceof SetupPowerGenerator.Instance)
					((SetupPowerGenerator.Instance)setup).scheduleUpdate();
			} else {
				final BlockPos pos2 = bp.up(0);
				world.getMinecraftServer().addScheduledTask(() -> world.scheduleBlockUpdate(pos2, world.getBlockState(pos2).getBlock(), 0, 1));
			}
			System.out.println(bp);
		});
	}
	
	private static Optional<BlockPos> scan(World world, BlockPos pos, List<BlockPos> searchedBlocks, int i) {
		IBlockState state = world.getBlockState(pos);
		if(++i > 100 || !(state.getBlock() instanceof IPowerConductor) || searchedBlocks.contains(pos)) 
			return Optional.empty();
		
		searchedBlocks.add(pos);
		
		if(state.getBlock() instanceof IMachine && ((IMachine)state.getBlock()).getPowerConsumption(world, pos) == PowerConsumption.GENERATOR)
		{
			System.out.println(pos);
			return Optional.of(pos);
		}
		
		for(EnumFacing face : EnumFacing.values()) {
			if(((IPowerConductor)state.getBlock()).shouldPowerConnect(world, pos, face)) {
				Optional<BlockPos> result = scan(world, pos.offset(face), searchedBlocks, i);
				if(result.isPresent())
					return result;
			}
		}
		
		return Optional.empty();
	}

	@Override
	public void setPowered(IBlockAccess worldIn, BlockPos pos, boolean b) {
		if(worldIn instanceof World && b != worldIn.getBlockState(pos).getValue(POWERED))
			((World) worldIn).setBlockState(pos, worldIn.getBlockState(pos).withProperty(POWERED, b));
	}
	
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, POWERED);
	}
	
	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(POWERED, (meta & 1) == 1);
	}
	
	@Override
	public int getMetaFromState(IBlockState state) {
		return (state.getValue(POWERED) ? 1 : 0);
	}

}
