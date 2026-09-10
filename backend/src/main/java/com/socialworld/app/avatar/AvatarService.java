package com.socialworld.app.avatar;

import com.socialworld.app.avatar.dto.AvatarAssetResponse;
import com.socialworld.app.avatar.dto.AvatarResponse;
import com.socialworld.app.avatar.dto.UpdateAvatarRequest;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarService {

    /** Shown until the user customizes their avatar. Keys exist in the V3 seed. */
    static final AvatarResponse DEFAULT_AVATAR = new AvatarResponse(
            "body_01", "face_01", "eyes_01", "hair_01",
            "top_01", "bottom_01", "shoes_01", null);

    private final AvatarRepository avatarRepository;
    private final AvatarAssetRepository avatarAssetRepository;

    @Transactional(readOnly = true)
    public List<AvatarAssetResponse> listAssets() {
        return avatarAssetRepository.findByEnabledTrueOrderByCategoryAscSortOrderAsc().stream()
                .map(AvatarAssetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvatarResponse getForUser(UUID userId) {
        return avatarRepository.findById(userId)
                .map(AvatarResponse::from)
                .orElse(DEFAULT_AVATAR);
    }

    @Transactional
    public AvatarResponse update(UUID userId, UpdateAvatarRequest request) {
        Avatar avatar = avatarRepository.findById(userId)
                .orElseGet(() -> Avatar.builder().userId(userId).build());

        avatar.setBodyKey(requireAsset(request.bodyId(), AvatarCategory.BODY));
        avatar.setFaceKey(optionalAsset(request.faceId(), AvatarCategory.FACE));
        avatar.setEyesKey(optionalAsset(request.eyesId(), AvatarCategory.EYES));
        avatar.setHairKey(optionalAsset(request.hairId(), AvatarCategory.HAIR));
        avatar.setTopKey(optionalAsset(request.topId(), AvatarCategory.TOP));
        avatar.setBottomKey(optionalAsset(request.bottomId(), AvatarCategory.BOTTOM));
        avatar.setShoesKey(optionalAsset(request.shoesId(), AvatarCategory.SHOES));
        avatar.setAccessoryKey(optionalAsset(request.accessoryId(), AvatarCategory.ACCESSORY));

        return AvatarResponse.from(avatarRepository.save(avatar));
    }

    private String optionalAsset(String assetKey, AvatarCategory category) {
        if (assetKey == null || assetKey.isBlank()) {
            return null;
        }
        return requireAsset(assetKey, category);
    }

    private String requireAsset(String assetKey, AvatarCategory category) {
        AvatarAsset asset = avatarAssetRepository.findByAssetKeyAndEnabledTrue(assetKey)
                .orElseThrow(() -> new ApiException(ErrorCode.AVATAR_ASSET_INVALID,
                        "Unknown avatar asset: " + assetKey));
        if (asset.getCategory() != category) {
            throw new ApiException(ErrorCode.AVATAR_ASSET_INVALID,
                    "Asset " + assetKey + " is not a " + category + " asset.");
        }
        return asset.getAssetKey();
    }
}
