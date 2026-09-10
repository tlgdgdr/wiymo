import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

import type { Message } from '@/api/types';
import { API_URL } from '@/config';
import { useAuthStore } from '@/store/auth';

interface WsEvent {
  type: string;
  payload: unknown;
}

/**
 * One live chat socket for the signed-in user. Incoming messages are merged
 * into the TanStack Query caches ('messages', partnerId) and refresh the
 * conversation list. Reconnects with a small backoff while mounted.
 */
export function useChatSocket() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const myUserId = useAuthStore((s) => s.userId);
  const queryClient = useQueryClient();
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!accessToken || !myUserId) return;

    let closed = false;
    let retryDelay = 1000;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;

    const connect = () => {
      const wsUrl = `${API_URL.replace(/^http/, 'ws')}/ws/chat?token=${encodeURIComponent(accessToken)}`;
      const socket = new WebSocket(wsUrl);
      socketRef.current = socket;

      socket.onopen = () => {
        retryDelay = 1000;
      };

      socket.onmessage = (event) => {
        try {
          const parsed = JSON.parse(String(event.data)) as WsEvent;
          if (parsed.type === 'message') {
            const message = parsed.payload as Message;
            const partnerId = message.senderId === myUserId ? message.receiverId : message.senderId;
            queryClient.setQueryData<Message[]>(['messages', partnerId], (old: Message[] | undefined) => {
              if (!old) return [message];
              if (old.some((m: Message) => m.id === message.id)) return old;
              return [message, ...old];
            });
            void queryClient.invalidateQueries({ queryKey: ['conversations'] });
          } else if (parsed.type === 'gift') {
            void queryClient.invalidateQueries({ queryKey: ['wallet'] });
          }
        } catch {
          // Ignore malformed frames.
        }
      };

      socket.onclose = () => {
        if (!closed) {
          reconnectTimer = setTimeout(connect, retryDelay);
          retryDelay = Math.min(retryDelay * 2, 15_000);
        }
      };
      socket.onerror = () => {
        socket.close();
      };
    };

    connect();
    return () => {
      closed = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      socketRef.current?.close();
      socketRef.current = null;
    };
  }, [accessToken, myUserId, queryClient]);

  const sendViaSocket = (receiverId: string, content: string): boolean => {
    const socket = socketRef.current;
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'chat.send', receiverId, content }));
      return true;
    }
    return false;
  };

  return { sendViaSocket };
}
