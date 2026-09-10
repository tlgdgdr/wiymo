package com.socialworld.app.avatar;

import com.socialworld.app.avatar.dto.AvatarResponse;
import com.socialworld.app.avatar.dto.UpdateAvatarRequest;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private AvatarRepository avatarRepository;
    @Mock
    private AvatarAssetRepository assetRepository;

    @InjectMocks
    private AvatarService avatarService;

    private final UUID userId = UUID.randomUUID();

    private AvatarAsset asset(String key, AvatarCategory category) {
        return AvatarAsset.builder().assetKey(key).category(category)
                .displayName(key).imageUrl("https://example.com/" + key + ".png").build();
    }

    private void stubAsset(String key, AvatarCategory category) {
        when(assetRepository.findByAssetKeyAndEnabledTrue(key))
                .thenReturn(Optional.of(asset(key, category)));
    }

    @Test
    void getForUser_returnsDefaultWhenUnset() {
        when(avatarRepository.findById(userId)).thenReturn(Optional.empty());
        AvatarResponse response = avatarService.getForUser(userId);
        assertThat(response.bodyId()).isEqualTo("body_01");
        assertThat(response.accessoryId()).isNull();
    }

    @Test
    void update_savesValidSelection() {
        when(avatarRepository.findById(userId)).thenReturn(Optional.empty());
        when(avatarRepository.save(any(Avatar.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAsset("body_02", AvatarCategory.BODY);
        stubAsset("hair_03", AvatarCategory.HAIR);

        AvatarResponse response = avatarService.update(userId,
                new UpdateAvatarRequest("body_02", null, null, "hair_03", null, null, null, null));

        assertThat(response.bodyId()).isEqualTo("body_02");
        assertThat(response.hairId()).isEqualTo("hair_03");
        assertThat(response.topId()).isNull();
    }

    @Test
    void update_rejectsUnknownAsset() {
        when(avatarRepository.findById(userId)).thenReturn(Optional.empty());
        when(assetRepository.findByAssetKeyAndEnabledTrue("body_999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> avatarService.update(userId,
                new UpdateAvatarRequest("body_999", null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.AVATAR_ASSET_INVALID);
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void update_rejectsCategoryMismatch() {
        when(avatarRepository.findById(userId)).thenReturn(Optional.empty());
        stubAsset("hair_01", AvatarCategory.HAIR);

        // hair asset passed as the body layer
        assertThatThrownBy(() -> avatarService.update(userId,
                new UpdateAvatarRequest("hair_01", null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.AVATAR_ASSET_INVALID);
    }

    @Test
    void update_blankOptionalLayerClearsIt() {
        Avatar existing = Avatar.builder().userId(userId).bodyKey("body_01").hairKey("hair_01").build();
        when(avatarRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(avatarRepository.save(any(Avatar.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAsset("body_01", AvatarCategory.BODY);

        AvatarResponse response = avatarService.update(userId,
                new UpdateAvatarRequest("body_01", null, null, "", null, null, null, null));

        assertThat(response.hairId()).isNull();
    }
}
