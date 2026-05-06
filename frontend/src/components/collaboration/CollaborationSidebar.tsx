import { useState, useEffect } from 'react';
import CommentPanel from './CommentPanel';
import TaskPanel from './TaskPanel';
import ActivityTimeline from './ActivityTimeline';
import { workspacesApi } from '../../api/workspaces';
import type { WorkspaceMember } from '../../types/collaboration';

interface CollaborationSidebarProps {
  fileId?: string;
  folderId?: string;
  workspaceId?: string;
  currentUserId: string;
  onClose: () => void;
}

type Tab = 'comments' | 'tasks' | 'activity';

export default function CollaborationSidebar({ fileId, folderId, workspaceId, currentUserId, onClose }: CollaborationSidebarProps) {
  const [activeTab, setActiveTab] = useState<Tab>('comments');
  const [members, setMembers] = useState<WorkspaceMember[]>([]);

  useEffect(() => {
    if (workspaceId) {
      workspacesApi.getMembers(workspaceId).then(res => {
        setMembers(res.data.data);
      }).catch(() => {});
    }
  }, [workspaceId]);

  const tabs: { key: Tab; label: string }[] = [
    { key: 'comments', label: 'Comments' },
    { key: 'tasks', label: 'Tasks' },
    { key: 'activity', label: 'Activity' },
  ];

  return (
    <div style={{
      width: 360, borderLeft: '1px solid #e0e0e0', display: 'flex', flexDirection: 'column',
      backgroundColor: '#fff', height: '100%',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', borderBottom: '1px solid #e8e8e8' }}>
        <div style={{ display: 'flex', gap: 0 }}>
          {tabs.map(tab => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              style={{
                padding: '6px 14px', border: 'none', cursor: 'pointer', fontSize: 13, fontWeight: 500,
                borderBottom: activeTab === tab.key ? '2px solid #1976d2' : '2px solid transparent',
                color: activeTab === tab.key ? '#1976d2' : '#666',
                background: 'none',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <button
          onClick={onClose}
          style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: 18, color: '#666' }}
        >
          ✕
        </button>
      </div>

      <div style={{ flex: 1, overflow: 'hidden' }}>
        {activeTab === 'comments' && (
          <CommentPanel fileId={fileId} folderId={folderId} currentUserId={currentUserId} members={members} />
        )}
        {activeTab === 'tasks' && fileId && (
          <TaskPanel fileId={fileId} currentUserId={currentUserId} />
        )}
        {activeTab === 'activity' && (
          <ActivityTimeline fileId={fileId} folderId={folderId} />
        )}
      </div>
    </div>
  );
}
