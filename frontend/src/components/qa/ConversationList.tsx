import React, { useState, useEffect } from 'react';
import { qaApi, type ConversationSummary } from '../../api/qa';

interface ConversationListProps {
  workspaceId: string;
  activeConversationId?: string;
  onSelectConversation: (conversationId: string) => void;
  onNewConversation: () => void;
}

export const ConversationList: React.FC<ConversationListProps> = ({
  workspaceId,
  activeConversationId,
  onSelectConversation,
  onNewConversation,
}) => {
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadConversations();
  }, [workspaceId]);

  const loadConversations = async () => {
    setLoading(true);
    try {
      const data = await qaApi.getConversations(workspaceId);
      setConversations(data.content);
    } catch {
      // Silently fail
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (conversationId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await qaApi.deleteConversation(conversationId);
      setConversations(prev => prev.filter(c => c.id !== conversationId));
    } catch {
      // Silently fail
    }
  };

  const filtered = search
    ? conversations.filter(c => c.title.toLowerCase().includes(search.toLowerCase()))
    : conversations;

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      borderRight: '1px solid #e5e7eb',
      width: '280px',
      backgroundColor: '#f9fafb',
    }}>
      {/* Header */}
      <div style={{ padding: '12px', borderBottom: '1px solid #e5e7eb' }}>
        <button
          onClick={onNewConversation}
          style={{
            width: '100%',
            padding: '8px',
            backgroundColor: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontSize: '13px',
            fontWeight: 500,
            cursor: 'pointer',
            marginBottom: '8px',
          }}
        >
          + New Conversation
        </button>
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search conversations..."
          style={{
            width: '100%',
            padding: '6px 10px',
            border: '1px solid #d1d5db',
            borderRadius: '4px',
            fontSize: '12px',
            boxSizing: 'border-box',
          }}
        />
      </div>

      {/* List */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {loading && (
          <div style={{ padding: '16px', textAlign: 'center', color: '#9ca3af', fontSize: '13px' }}>
            Loading...
          </div>
        )}
        {!loading && filtered.length === 0 && (
          <div style={{ padding: '16px', textAlign: 'center', color: '#9ca3af', fontSize: '13px' }}>
            No conversations yet
          </div>
        )}
        {filtered.map(conv => (
          <div
            key={conv.id}
            onClick={() => onSelectConversation(conv.id)}
            style={{
              padding: '10px 12px',
              borderBottom: '1px solid #f3f4f6',
              cursor: 'pointer',
              backgroundColor: conv.id === activeConversationId ? '#eff6ff' : 'transparent',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
            }}
          >
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{
                fontSize: '13px',
                fontWeight: 500,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}>
                {conv.title}
              </div>
              <div style={{ fontSize: '11px', color: '#9ca3af', marginTop: '2px' }}>
                {conv.messageCount} messages · {new Date(conv.updatedAt).toLocaleDateString()}
              </div>
            </div>
            <button
              onClick={(e) => handleDelete(conv.id, e)}
              style={{
                background: 'none',
                border: 'none',
                color: '#9ca3af',
                cursor: 'pointer',
                fontSize: '14px',
                padding: '2px',
              }}
              title="Delete"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
