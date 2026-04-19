package dev.thenexusgates.hyguard.map;

import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.chunk.ChunkInfo;
import com.buuz135.simpleclaims.claim.party.PartyInfo;

import java.util.UUID;

public final class SimpleClaimsMapCompat {

	private final Adapter adapter;

	public SimpleClaimsMapCompat() {
		this.adapter = createAdapter();
	}

	public boolean isAvailable() {
		return this.adapter != null;
	}

	public String describeMode() {
		return isAvailable() ? "hyguard+simpleclaims" : "hyguard-only";
	}

	public ChunkOverlay resolveChunkOverlay(String worldId, int chunkX, int chunkZ) {
		if (this.adapter == null || worldId == null || worldId.isBlank()) {
			return ChunkOverlay.none();
		}
		return this.adapter.resolveChunkOverlay(worldId, chunkX, chunkZ);
	}

	private static Adapter createAdapter() {
		try {
			Class.forName("com.buuz135.simpleclaims.claim.ClaimManager", false, SimpleClaimsMapCompat.class.getClassLoader());
			return new DirectAdapter();
		} catch (Throwable ignored) {
			return null;
		}
	}

	public record ChunkOverlay(boolean visible,
	                          int color,
	                          boolean westBorder,
	                          boolean eastBorder,
	                          boolean northBorder,
	                          boolean southBorder) {

		private static ChunkOverlay none() {
			return new ChunkOverlay(false, 0, false, false, false, false);
		}
	}

	private interface Adapter {
		ChunkOverlay resolveChunkOverlay(String worldId, int chunkX, int chunkZ);
	}

	private static final class DirectAdapter implements Adapter {

		@Override
		public ChunkOverlay resolveChunkOverlay(String worldId, int chunkX, int chunkZ) {
			ClaimManager claimManager = ClaimManager.getInstance();
			if (claimManager == null) {
				return ChunkOverlay.none();
			}

			ChunkInfo claim = claimManager.getChunk(worldId, chunkX, chunkZ);
			if (claim == null || claim.getPartyOwner() == null) {
				return ChunkOverlay.none();
			}

			UUID ownerId = claim.getPartyOwner();
			PartyInfo party = claimManager.getPartyById(ownerId);
			if (party == null) {
				return ChunkOverlay.none();
			}

			return new ChunkOverlay(
					true,
					party.getColor(),
					!hasSameOwner(claimManager, worldId, chunkX - 1, chunkZ, ownerId),
					!hasSameOwner(claimManager, worldId, chunkX + 1, chunkZ, ownerId),
					!hasSameOwner(claimManager, worldId, chunkX, chunkZ - 1, ownerId),
					!hasSameOwner(claimManager, worldId, chunkX, chunkZ + 1, ownerId)
			);
		}

		private boolean hasSameOwner(ClaimManager claimManager, String worldId, int chunkX, int chunkZ, UUID ownerId) {
			ChunkInfo neighbor = claimManager.getChunk(worldId, chunkX, chunkZ);
			return neighbor != null && ownerId.equals(neighbor.getPartyOwner());
		}
	}
}