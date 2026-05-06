import { useState, useEffect, useCallback } from 'react';
import { getComments, createComment, deleteComment, getFolderComments, createFolderComment, deleteFolderComment } from '../../api/comments';
import CommentItem from './CommentItem';
import MentionInput from './MentionInput';
import type { Comment, WorkspaceMember } from '../../types/collaboration';

interface CommentPanelProps {
  fileId?: string;
  folderId?: string;
  currentUserId: string;
  members?: WorkspaceMember[];
}

export default function CommentPanel({ fileId, folderId, currentUserId, members = [] }: CommentPanelProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newComment, setNewComment] = useState('');
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const loadComments = useCallback(async (pageNum = 0, append = false) => {
    try {
      if (!append) setLoading(true);
      let result;
      if (folderId) {
        result = await getFolderComments(folderId, pageNum, 50);
      } else if (fileId) {
        result = await getComments(fileId, pageNum, 50);
      } else return;

      if (append) {
        setComments(prev => [...prev, ...result.content]);
      } else {
        setComments(result.content);
      }
      setHasMore(result.number < result.totalPages - 1);
      setError(null);
    } catch {
      setError('Failed to load comments');
    } finally {
      setLoading(false);
    }
  }, [fileId, folderId]);

  useEffect(() => {
    setComments([]);
    setPage(0);
    loadComments(0);
  }, [loadComments]);

  const handleSubmit = async () => {
    if (!newComment.trim() || submitting) return;
    setSubmitting(true);
    try {
      if (folderId) {
        await createFolderComment(folderId, { content: newComment.trim(), parentId: replyingTo || undefined });
      } else if (fileId) {
        await createComment(fileId, { content: newComment.trim(), parentId: replyingTo || undefined });
      }
      setNewComment('');
      setReplyingTo(null);
      await loadComments(0);
    } catch {
      setError('Failed to post comment');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (commentId: string) => {
    if (!confirm('Delete this comment?')) return;
    try {
      if (folderId) {
        await deleteFolderComment(folderId, commentId);
      } else if (fileId) {
        await deleteComment(fileId, commentId);
      }
      await loadComments(0);
    } catch {
      setError('Failed to delete comment');
    }
  };

  const handleLoadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    loadComments(nextPage, true);
  };

  if (loading && comments.length === 0) {
    return <div style={{ padding: 16, color: '#888', textAlign: 'center' }}>Loading comments...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px' }}>
        {error && (
          <div style={{ padding: 8, marginBottom: 8, backgroundColor: '#fff3f3', color: '#d32f2f', borderRadius: 4, fontSize: 13 }}>
            {error}
          </div>
        )}

        {comments.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 24, color: '#888' }}>
            <p style={{ margin: 0 }}>No comments yet</p>
            <p style={{ margin: '4px 0 0', fontSize: 13 }}>Be the first to start a discussion</p>
          </div>
        ) : (
          <>
            {comments.map(comment => (
              <CommentItem
                key={comment.id}
                comment={comment}
                currentUserId={currentUserId}
                onReply={(parentId) => setReplyingTo(parentId)}
                onDelete={handleDelete}
              />
            ))}
            {hasMore && (
              <button
                onClick={handleLoadMore}
                style={{ display: 'block', margin: '8px auto', padding: '6px 16px', cursor: 'pointer', border: '1px solid #ddd', borderRadius: 4, background: '#fff' }}
              >
                Load more
              </button>
            )}
          </>
        )}
      </div>

      <div style={{ borderTop: '1px solid #e8e8e8', padding: 12 }}>
        {replyingTo && (
          <div style={{ fontSize: 12, color: '#666', marginBottom: 6, display: 'flex', justifyContent: 'space-between' }}>
            <span>Replying to comment...</span>
            <button onClick={() => setReplyingTo(null)} style={{ border: 'none', background: 'none', cursor: 'pointer', color: '#999' }}>Cancel</button>
          </div>
        )}
        <div style={{ display: 'flex', gap: 8 }}>
          <div style={{ flex: 1 }}>
            <MentionInput
              value={newComment}
              onChange={setNewComment}
              onSubmit={handleSubmit}
              members={members}
              placeholder="Write a comment... Use @ to mention"
            />
          </div>
          <button
            onClick={handleSubmit}
            disabled={!newComment.trim() || submitting}
            style={{
              padding: '8px 16px', borderRadius: 6, border: 'none', cursor: 'pointer',
              backgroundColor: newComment.trim() ? '#1976d2' : '#ccc', color: '#fff',
              alignSelf: 'flex-end',
            }}
          >
            {submitting ? '...' : 'Post'}
          </button>
        </div>
      </div>
    </div>
  );
}
