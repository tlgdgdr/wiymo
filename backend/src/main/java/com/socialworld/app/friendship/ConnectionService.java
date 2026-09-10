package com.socialworld.app.friendship;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.friendship.dto.ConnectionResponse;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final AvatarService avatarService;

    @Transactional
    public ConnectionResponse request(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new ApiException(ErrorCode.SELF_ACTION_NOT_ALLOWED, "You cannot connect with yourself.");
        }
        User addressee = userRepository.findById(addresseeId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        // A REJECTED pair is not "active", so a new request is allowed later.
        connectionRepository.findActivePair(requesterId, addresseeId).ifPresent(existing -> {
            throw new ApiException(ErrorCode.CONNECTION_ALREADY_EXISTS);
        });

        Connection connection = connectionRepository.save(Connection.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .status(ConnectionStatus.PENDING)
                .build());
        return ConnectionResponse.from(connection, requesterId, addressee,
                avatarService.getForUser(addresseeId));
    }

    @Transactional
    public ConnectionResponse accept(UUID viewerId, UUID connectionId) {
        return decide(viewerId, connectionId, ConnectionStatus.ACCEPTED);
    }

    @Transactional
    public ConnectionResponse reject(UUID viewerId, UUID connectionId) {
        return decide(viewerId, connectionId, ConnectionStatus.REJECTED);
    }

    private ConnectionResponse decide(UUID viewerId, UUID connectionId, ConnectionStatus decision) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Connection not found."));
        // Only the addressee of a pending request may decide it.
        if (!connection.getAddresseeId().equals(viewerId)
                || connection.getStatus() != ConnectionStatus.PENDING) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Connection not found.");
        }
        connection.setStatus(decision);

        User partner = userRepository.findById(connection.getRequesterId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        return ConnectionResponse.from(connection, viewerId, partner,
                avatarService.getForUser(partner.getId()));
    }

    @Transactional(readOnly = true)
    public List<ConnectionResponse> list(UUID viewerId) {
        List<Connection> connections = connectionRepository.findAllForUser(viewerId).stream()
                .filter(c -> c.getStatus() == ConnectionStatus.PENDING
                        || c.getStatus() == ConnectionStatus.ACCEPTED)
                .toList();
        if (connections.isEmpty()) {
            return List.of();
        }

        List<UUID> partnerIds = connections.stream()
                .map(c -> partnerOf(c, viewerId))
                .toList();
        Map<UUID, User> partners = userRepository.findAllById(partnerIds).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return connections.stream()
                .map(c -> {
                    User partner = partners.get(partnerOf(c, viewerId));
                    if (partner == null) {
                        return null;
                    }
                    return ConnectionResponse.from(c, viewerId, partner,
                            avatarService.getForUser(partner.getId()));
                })
                .filter(r -> r != null)
                .toList();
    }

    private UUID partnerOf(Connection connection, UUID viewerId) {
        return connection.getRequesterId().equals(viewerId)
                ? connection.getAddresseeId()
                : connection.getRequesterId();
    }
}
