import { useState } from 'react';
import type { Comment } from '../../types/collaboration';

interface CommentItemProps {
  comment: Comment;
  currentUserId: string;
  onReply: (parentId: string) => void;
  onDelete: (commentId: string) => void;
  depth?: number;
}

export default function CommentItem({ comment, currentUserId, onReply, onDelete, depth = 0 }: CommentItemProps) {
  const [showReplies, setShowReplies] = useState(true);
  const isAuthor = comment.author?.id === currentUserId;
  const canReply = depth < 1; // Max 2 levels

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHrs = Math.floor(diffMin / 60);
    if (diffHrs < 24) return `${diffHrs}h ago`;
    const diffDays = Math.floor(diffHrs / 24);
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  // Render content with highlighted mentions
  const renderContent = (content: string) => {
    const mentionRegex = /@\[([a-f0-9-]{36})]/g;
    const parts: (string | JSX.Element)[] = [];
    let lastIndex = 0;
    let match;

    while ((match = mentionRegex.exec(content)) !== null) {
      if (match.index > lastIndex) {
        parts.push(content.slice(lastIndex, match.index));
      }
      const mentionedUserId = match[1];
      const mentionInfo = comment.mentions?.find(m => m.userId === mentionedUserId);
      parts.push(
        <span key={match.index} style={{ color: '#1976d2', fontWeight: 500 }}>
          @{mentionInfo?.name || 'user'}
        </span>
      );
      lastIndex = match.index + match[0].length;
    }
    if (lastIndex < content.length) {
      parts.push(content.slice(lastIndex));
    }
    return parts.length > 0 ? parts : content;
  };

  return (
    <div style={{ marginLeft: depth * 24, marginBottom: 12 }}>
      <div style={{
        padding: '10px 12px',
        borderRadius: 8,
        backgroundColor: depth > 0 ? '#f8f9fa' : '#fff',
        border: '1px solid #e8e8e8',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{
              width: 28, height: 28, borderRadius: '50%', backgroundColor: '#e3f2fd',
              display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 600,
            }}>
              {comment.author?.name?.charAt(0)?.toUpperCase() || '?'}
            </span>
            <span style={{ fontWeight: 500, fontSize: 13 }}>{comment.author?.name || 'Unknown'}</span>
            <span style={{ fontSize: 12, color: '#888' }}>{formatTime(comment.createdAt)}</span>
          </div>
          {isAuthor && (
            <button
              onClick={() => onDelete(comment.id)}
              style={{ border: 'none', background: 'none', cursor: 'pointer', color: '#999', fontSize: 12 }}
              title="Delete comment"
            >
              ✕
            </button>
          )}
        </div>
        <div style={{ fontSize: 14, lineHeight: 1.5, whiteSpace: 'pre-wrap' }}>
          {renderContent(comment.content)}
        </div>
        <div style={{ marginTop: 6, display: 'flex', gap: 12 }}>
          {canReply && (
            <button
              onClick={() => onReply(comment.id)}
              style={{ border: 'none', background: 'none', cursor: 'pointer', color: '#666', fontSize: 12 }}
            >
              Reply
            </button>
          )}
        </div>
      </div>

      {comment.replies && comment.replies.length > 0 && (
        <div style={{ marginTop: 4 }}>
          {comment.replies.length > 0 && (
            <button
              onClick={() => setShowReplies(!showReplies)}
              style={{ border: 'none', background: 'none', cursor: 'pointer', color: '#1976d2', fontSize: 12, marginBottom: 4 }}
            >
              {showReplies ? '▾' : '▸'} {comment.replies.length} {comment.replies.length === 1 ? 'reply' : 'replies'}
            </button>
          )}
          {showReplies && comment.replies.map(reply => (
            <CommentItem
              key={reply.id}
              comment={reply}
              currentUserId={currentUserId}
              onReply={onReply}
              onDelete={onDelete}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}
