import { useEffect, useState } from 'react';
import type { Comment } from '../../types/preview';
import { getComments, createComment, deleteComment } from '../../api/comments';

interface MetadataPanelProps {
  fileId: string;
  onClose: () => void;
}

export default function MetadataPanel({ fileId, onClose }: MetadataPanelProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [replyTo, setReplyTo] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadComments();
  }, [fileId]);

  const loadComments = async () => {
    try {
      setLoading(true);
      const data = await getComments(fileId);
      setComments(data.content);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!newComment.trim()) return;
    try {
      setSubmitting(true);
      await createComment(fileId, {
        content: newComment.trim(),
        parentId: replyTo || undefined,
      });
      setNewComment('');
      setReplyTo(null);
      await loadComments();
    } catch {
      // silent
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (commentId: string) => {
    try {
      await deleteComment(fileId, commentId);
      await loadComments();
    } catch {
      // silent
    }
  };

  return (
    <div style={{
      width: 320,
      borderLeft: '1px solid #374151',
      background: '#1f2937',
      display: 'flex',
      flexDirection: 'column',
      color: '#f9fafb',
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '12px 16px',
        borderBottom: '1px solid #374151',
      }}>
        <h4 style={{ margin: 0, fontSize: 14 }}>Comments</h4>
        <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#9ca3af', cursor: 'pointer' }}>✕</button>
      </div>

      {/* Comments list */}
      <div style={{ flex: 1, overflow: 'auto', padding: 12 }}>
        {loading && <div style={{ color: '#9ca3af', fontSize: 13 }}>Loading...</div>}
        {!loading && comments.length === 0 && (
          <div style={{ color: '#9ca3af', fontSize: 13, textAlign: 'center', padding: 16 }}>
            No comments yet
          </div>
        )}
        {comments.map(comment => (
          <CommentItem
            key={comment.id}
            comment={comment}
            onReply={(id) => setReplyTo(id)}
            onDelete={handleDelete}
          />
        ))}
      </div>

      {/* Input */}
      <div style={{ padding: 12, borderTop: '1px solid #374151' }}>
        {replyTo && (
          <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
            <span>Replying...</span>
            <button onClick={() => setReplyTo(null)} style={{ background: 'none', border: 'none', color: '#9ca3af', cursor: 'pointer', fontSize: 12 }}>Cancel</button>
          </div>
        )}
        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="Add a comment..."
          style={{
            width: '100%',
            minHeight: 60,
            padding: 8,
            background: '#374151',
            border: '1px solid #4b5563',
            borderRadius: 4,
            color: '#f9fafb',
            fontSize: 13,
            resize: 'vertical',
          }}
        />
        <button
          onClick={handleSubmit}
          disabled={!newComment.trim() || submitting}
          style={{
            marginTop: 8,
            width: '100%',
            padding: '6px 12px',
            background: newComment.trim() ? '#3b82f6' : '#4b5563',
            border: 'none',
            borderRadius: 4,
            color: '#fff',
            cursor: newComment.trim() ? 'pointer' : 'default',
            fontSize: 13,
          }}
        >
          {submitting ? 'Posting...' : 'Post Comment'}
        </button>
      </div>
    </div>
  );
}

function CommentItem({ comment, onReply, onDelete }: {
  comment: Comment;
  onReply: (id: string) => void;
  onDelete: (id: string) => void;
}) {
  return (
    <div style={{ marginBottom: 12, fontSize: 13 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontWeight: 500 }}>{comment.author.name}</span>
        <span style={{ color: '#6b7280', fontSize: 11 }}>
          {new Date(comment.createdAt).toLocaleDateString()}
        </span>
      </div>
      <p style={{ margin: '4px 0', color: '#d1d5db', lineHeight: 1.4 }}>{comment.content}</p>
      <div style={{ display: 'flex', gap: 12 }}>
        <button
          onClick={() => onReply(comment.id)}
          style={{ background: 'none', border: 'none', color: '#60a5fa', cursor: 'pointer', fontSize: 12, padding: 0 }}
        >
          Reply
        </button>
        <button
          onClick={() => onDelete(comment.id)}
          style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: 12, padding: 0 }}
        >
          Delete
        </button>
      </div>

      {/* Replies */}
      {comment.replies && comment.replies.length > 0 && (
        <div style={{ marginLeft: 16, marginTop: 8, borderLeft: '2px solid #374151', paddingLeft: 12 }}>
          {comment.replies.map(reply => (
            <div key={reply.id} style={{ marginBottom: 8 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 500, fontSize: 12 }}>{reply.author.name}</span>
                <span style={{ color: '#6b7280', fontSize: 11 }}>
                  {new Date(reply.createdAt).toLocaleDateString()}
                </span>
              </div>
              <p style={{ margin: '2px 0', color: '#d1d5db', fontSize: 12, lineHeight: 1.4 }}>{reply.content}</p>
              <button
                onClick={() => onDelete(reply.id)}
                style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: 11, padding: 0 }}
              >
                Delete
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
